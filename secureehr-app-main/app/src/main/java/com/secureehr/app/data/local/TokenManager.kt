package com.secureehr.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class TokenManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        // Health Wallet
        private val INSURANCE_PROVIDER_KEY = stringPreferencesKey("insurance_provider")
        private val POLICY_NUMBER_KEY = stringPreferencesKey("policy_number")
        private val COVERAGE_AMOUNT_KEY = stringPreferencesKey("coverage_amount")
        private val INSURANCE_EXPIRY_KEY = stringPreferencesKey("insurance_expiry")
        private val HEALTH_ID_KEY = stringPreferencesKey("health_id")
        private val ABHA_NUMBER_KEY = stringPreferencesKey("abha_number")
        private val WALLET_DOCUMENTS_KEY = stringPreferencesKey("wallet_documents")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
    }

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED_KEY] ?: false }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE_KEY] ?: true }
    val language: Flow<String> = context.dataStore.data.map { it[LANGUAGE_KEY] ?: "English" }
    // Health Wallet flows
    val insuranceProvider: Flow<String> = context.dataStore.data.map { it[INSURANCE_PROVIDER_KEY] ?: "" }
    val policyNumber: Flow<String> = context.dataStore.data.map { it[POLICY_NUMBER_KEY] ?: "" }
    val coverageAmount: Flow<String> = context.dataStore.data.map { it[COVERAGE_AMOUNT_KEY] ?: "" }
    val insuranceExpiry: Flow<String> = context.dataStore.data.map { it[INSURANCE_EXPIRY_KEY] ?: "" }
    val healthId: Flow<String> = context.dataStore.data.map { it[HEALTH_ID_KEY] ?: "" }
    val abhaNumber: Flow<String> = context.dataStore.data.map { it[ABHA_NUMBER_KEY] ?: "" }
    val walletDocuments: Flow<String> = context.dataStore.data.map { it[WALLET_DOCUMENTS_KEY] ?: "[]" }
    val userRole: Flow<String?> = context.dataStore.data.map { it[USER_ROLE_KEY] }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(TOKEN_KEY) }
    }

    suspend fun saveUserRole(role: String) {
        context.dataStore.edit { it[USER_ROLE_KEY] = role }
    }

    suspend fun clearUserRole() {
        context.dataStore.edit { it.remove(USER_ROLE_KEY) }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_ENABLED_KEY] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = lang }
    }

    suspend fun saveHealthWallet(
        provider: String,
        policy: String,
        coverage: String,
        expiry: String,
        healthId: String,
        abha: String
    ) {
        context.dataStore.edit {
            it[INSURANCE_PROVIDER_KEY] = provider
            it[POLICY_NUMBER_KEY] = policy
            it[COVERAGE_AMOUNT_KEY] = coverage
            it[INSURANCE_EXPIRY_KEY] = expiry
            it[HEALTH_ID_KEY] = healthId
            it[ABHA_NUMBER_KEY] = abha
        }
    }

    suspend fun saveWalletDocuments(json: String) {
        context.dataStore.edit { it[WALLET_DOCUMENTS_KEY] = json }
    }

    suspend fun clearHealthWallet() {
        context.dataStore.edit {
            it.remove(INSURANCE_PROVIDER_KEY)
            it.remove(POLICY_NUMBER_KEY)
            it.remove(COVERAGE_AMOUNT_KEY)
            it.remove(INSURANCE_EXPIRY_KEY)
            it.remove(HEALTH_ID_KEY)
            it.remove(ABHA_NUMBER_KEY)
            it.remove(WALLET_DOCUMENTS_KEY)
        }
    }
}
