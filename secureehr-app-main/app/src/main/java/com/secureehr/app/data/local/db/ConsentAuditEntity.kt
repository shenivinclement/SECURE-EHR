package com.secureehr.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "consent_audit")
data class ConsentAuditEntity(
    @PrimaryKey val id: String,
    val consentId: String,
    val doctorName: String,
    val action: String,           // "GRANTED" or "REVOKED"
    val timestamp: String,
    val txHash: String = "",
    val recordHash: String = "",
    val blockchainStatus: String = "Pending"  // "Pending", "Confirmed", "Failed", "Simulated"
)
