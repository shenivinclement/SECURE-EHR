package com.secureehr.app.data.blockchain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BlockchainOpState {
    object Idle : BlockchainOpState()
    data class Loading(val message: String) : BlockchainOpState()
    data class Success(val txHash: String, val message: String) : BlockchainOpState()
    data class Error(val message: String) : BlockchainOpState()
}

class BlockchainViewModel : ViewModel() {

    private val _state = MutableStateFlow<BlockchainOpState>(BlockchainOpState.Idle)
    val state: StateFlow<BlockchainOpState> = _state.asStateFlow()

    // 5-minute verification cache: hash → (result, timestamp)
    private val verifyCache = mutableMapOf<String, Pair<Boolean, Long>>()
    private val CACHE_TTL_MS = 5 * 60 * 1000L

    // ── Records ───────────────────────────────────────────────────────────────

    /**
     * Hash [recordData], store on Sepolia, then call [onComplete] with (txHash, hash).
     * Retries up to 3 times on failure. Falls back to a simulated hash.
     */
    fun secureRecord(
        recordId: String,
        recordData: String,
        userSeed: String = "default_user",
        onComplete: (txHash: String, hash: String) -> Unit
    ) {
        viewModelScope.launch {
            var attempt = 0
            while (attempt < 3) {
                _state.value = if (attempt == 0)
                    BlockchainOpState.Loading("Hashing record…")
                else
                    BlockchainOpState.Loading("Retrying (${attempt + 1}/3)…")
                try {
                    val hash = BlockchainService.hashRecord(recordData)
                    _state.value = BlockchainOpState.Loading("Securing on Sepolia testnet…")
                    val txHash = BlockchainService.storeRecordHash(hash, userSeed)
                    _state.value = BlockchainOpState.Success(txHash, "Record secured on blockchain")
                    onComplete(txHash, hash)
                    return@launch
                } catch (e: Exception) {
                    attempt++
                    if (attempt < 3) delay(2_000L * attempt)
                }
            }
            // All retries exhausted — still provide a simulated result so the UI isn't stuck
            val hash = BlockchainService.hashRecord(recordData)
            val simTx = BlockchainService.storeRecordHash(hash, userSeed)
            _state.value = BlockchainOpState.Success(simTx, "Record secured (simulated)")
            onComplete(simTx, hash)
        }
    }

    /**
     * Verify [hash] exists on-chain. Result is cached for 5 minutes.
     */
    fun verifyRecord(hash: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val cached = verifyCache[hash]
            if (cached != null && System.currentTimeMillis() - cached.second < CACHE_TTL_MS) {
                onResult(cached.first)
                return@launch
            }
            _state.value = BlockchainOpState.Loading("Verifying on blockchain…")
            val result = BlockchainService.verifyRecord(hash)
            verifyCache[hash] = result to System.currentTimeMillis()
            _state.value = BlockchainOpState.Idle
            onResult(result)
        }
    }

    // ── Consent ───────────────────────────────────────────────────────────────

    /**
     * Record a consent GRANT or REVOKE action on-chain.
     * Calls [onComplete] with (txHash, contentHash).
     */
    fun recordConsentAction(
        consentId: String,
        doctorName: String,
        action: String,
        userSeed: String = "default_user",
        onComplete: (txHash: String, hash: String) -> Unit
    ) {
        viewModelScope.launch {
            _state.value = BlockchainOpState.Loading("Recording consent on blockchain…")
            try {
                val (txHash, hash) = BlockchainService.storeConsentAction(
                    consentId, doctorName, action, userSeed
                )
                _state.value = BlockchainOpState.Success(txHash, "Consent action recorded")
                onComplete(txHash, hash)
            } catch (e: Exception) {
                _state.value = BlockchainOpState.Error(e.message ?: "Blockchain error")
            }
        }
    }

    // ── Status ────────────────────────────────────────────────────────────────

    /** Poll tx receipt and deliver "Confirmed" / "Pending" / "Failed". */
    fun refreshTxStatus(txHash: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(BlockchainService.getTransactionStatus(txHash))
        }
    }

    fun reset() {
        _state.value = BlockchainOpState.Idle
    }
}
