package com.secureehr.app.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalRecordDao {
    @Query("SELECT * FROM medical_records ORDER BY date DESC")
    fun observeAll(): Flow<List<MedicalRecordEntity>>

    @Query("SELECT * FROM medical_records WHERE txHash IS NOT NULL ORDER BY date DESC")
    fun observeBlockchainRecords(): Flow<List<MedicalRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MedicalRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<MedicalRecordEntity>)

    @Delete
    suspend fun delete(record: MedicalRecordEntity)

    @Query("DELETE FROM medical_records")
    suspend fun deleteAll()

    @Query("""
        UPDATE medical_records
        SET txHash = :txHash, recordHash = :hash, blockchainVerified = 1, chainTimestamp = :timestamp
        WHERE id = :id
    """)
    suspend fun updateBlockchainInfo(id: String, txHash: String, hash: String, timestamp: String)

    @Query("UPDATE medical_records SET ipfsCid = :cid WHERE id = :id")
    suspend fun updateIpfsCid(id: String, cid: String)
}
