package com.secureehr.app.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsentAuditDao {
    @Query("SELECT * FROM consent_audit WHERE consentId = :consentId ORDER BY timestamp DESC")
    fun observeByConsent(consentId: String): Flow<List<ConsentAuditEntity>>

    @Query("SELECT * FROM consent_audit ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ConsentAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ConsentAuditEntity)

    @Query("UPDATE consent_audit SET blockchainStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM consent_audit")
    suspend fun deleteAll()
}
