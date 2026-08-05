package com.secureehr.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class SavedCredential(
    val email: String,
    val password: String,
    val displayName: String,
    val role: String,
    val lastUsed: Long
)

fun deriveDisplayName(email: String): String {
    val local = email.substringBefore("@")
    return local.split(".", "_", "-", "+")
        .filter { it.isNotBlank() }
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
        .ifBlank { email }
}

/**
 * Stores multiple saved login credentials for quick account switching (patient/doctor demos).
 * Backed by EncryptedSharedPreferences (AES-256, Keystore-wrapped key) so passwords are never
 * written to disk in plaintext.
 */
class SavedCredentialsManager(context: Context) {
    private val gson = Gson()

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getAll(): List<SavedCredential> {
        val json = prefs.getString(KEY_CREDENTIALS, null) ?: return emptyList()
        val type = object : TypeToken<List<SavedCredential>>() {}.type
        val list: List<SavedCredential>? = try {
            gson.fromJson(json, type)
        } catch (_: Exception) {
            null
        }
        return (list ?: emptyList()).sortedByDescending { it.lastUsed }
    }

    /** Upsert on email (case-insensitive) — updates password/displayName/role and bumps lastUsed. */
    fun upsert(email: String, password: String, displayName: String, role: String) {
        val updated = getAll().filterNot { it.email.equals(email, ignoreCase = true) } +
            SavedCredential(email, password, displayName, role, System.currentTimeMillis())
        save(updated)
    }

    fun delete(email: String) {
        save(getAll().filterNot { it.email.equals(email, ignoreCase = true) })
    }

    fun clearAll() {
        prefs.edit().remove(KEY_CREDENTIALS).apply()
    }

    private fun save(list: List<SavedCredential>) {
        prefs.edit().putString(KEY_CREDENTIALS, gson.toJson(list)).apply()
    }

    companion object {
        private const val KEY_CREDENTIALS = "saved_credentials_v1"
    }
}
