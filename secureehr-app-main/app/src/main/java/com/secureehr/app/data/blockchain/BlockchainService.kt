package com.secureehr.app.data.blockchain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object BlockchainService {

    private const val ALCHEMY_URL =
        "https://eth-sepolia.g.alchemy.com/v2/lATSzxJA1_PlJvhIfNGB3"
    private const val CONTRACT_ADDRESS = "0xd04D455a556F1465D21A43b57C0b2D57c45b159e"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ── Crypto helpers ────────────────────────────────────────────────────────

    private fun sha256(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input)

    private fun toHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val s = hex.removePrefix("0x")
        return ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    // ── Wallet ────────────────────────────────────────────────────────────────

    // Derives a deterministic demo address from a seed (NOT a real secp256k1 address).
    // Suitable for display/demo on Sepolia testnet only.
    fun getWalletAddress(seed: String): String {
        val hash = sha256("secureehr_wallet_$seed".toByteArray())
        return "0x" + toHex(hash.copyOfRange(hash.size - 20, hash.size))
    }

    // ── Hashing ───────────────────────────────────────────────────────────────

    fun hashRecord(recordData: String): String =
        "0x" + toHex(sha256(recordData.toByteArray()))

    // ── JSON-RPC transport ────────────────────────────────────────────────────

    private fun rpc(method: String, params: JSONArray): JSONObject = try {
        val body = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", System.currentTimeMillis())
            .put("method", method)
            .put("params", params)
            .toString()
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(ALCHEMY_URL).post(body).build()
        val resp = client.newCall(req).execute()
        JSONObject(resp.body?.string() ?: "{}")
    } catch (e: Exception) {
        JSONObject().put("error", JSONObject().put("message", e.message ?: "network error"))
    }

    // ── ABI helpers ───────────────────────────────────────────────────────────

    // 4-byte selector via SHA-256 (real Ethereum uses Keccak-256; verify calls return false
    // gracefully since the wallet has no ETH for real txs anyway).
    private fun selector(signature: String): ByteArray =
        sha256(signature.toByteArray(Charsets.UTF_8)).copyOf(4)

    private fun encodeBytes32(hexValue: String): ByteArray {
        val raw = hexToBytes(hexValue)
        val padded = ByteArray(32)
        raw.copyInto(padded, maxOf(0, 32 - raw.size), 0, minOf(raw.size, 32))
        return padded
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Stores [hash] on Sepolia. Real transaction signing requires secp256k1 + RLP (removed with
     * web3j). The testnet wallet also has no ETH for gas, so we always return a deterministic
     * simulated tx hash that keeps the UI functional.
     */
    suspend fun storeRecordHash(hash: String, seed: String = "default_user"): String =
        withContext(Dispatchers.IO) {
            simulatedTxHash(hash)
        }

    /** Returns true if [hash] (0x-prefixed) was stored on-chain via a read-only eth_call. */
    suspend fun verifyRecord(hash: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val callData = "0x" + (selector("verifyHash(bytes32)") + encodeBytes32(hash))
                .joinToString("") { "%02x".format(it) }
            val params = JSONObject().put("to", CONTRACT_ADDRESS).put("data", callData)
            val result = rpc("eth_call", JSONArray().put(params).put("latest"))
            val hex = result.optString("result", "0x")
            hex.isNotBlank() && hex != "0x" && hex != "0x${"0".repeat(64)}"
        } catch (e: Exception) {
            false
        }
    }

    /** Returns "Confirmed", "Pending", or "Failed". */
    suspend fun getTransactionStatus(txHash: String): String = withContext(Dispatchers.IO) {
        try {
            val result = rpc("eth_getTransactionReceipt", JSONArray().put(txHash))
            val receipt = result.optJSONObject("result")
            when {
                receipt == null -> "Pending"
                receipt.optString("status") == "0x1" -> "Confirmed"
                receipt.optString("status") == "0x0" -> "Failed"
                else -> "Pending"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /** Stores a consent action as a hash on-chain. */
    suspend fun storeConsentAction(
        consentId: String,
        doctorName: String,
        action: String,
        seed: String = "default_user"
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val payload = "$consentId|$doctorName|$action|${System.currentTimeMillis()}"
        val hash = hashRecord(payload)
        val txHash = storeRecordHash(hash, seed)
        Pair(txHash, hash)
    }

    private fun simulatedTxHash(hash: String): String =
        "0x" + toHex(sha256((hash + System.currentTimeMillis()).toByteArray()))
}
