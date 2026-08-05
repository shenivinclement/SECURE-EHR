package com.secureehr.app.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medical_records")
data class MedicalRecordEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,
    val doctor: String,
    val hospital: String,
    val notes: String,
    val type: String = "General",
    @ColumnInfo(name = "diagnosis") val diagnosis: String? = null,
    @ColumnInfo(name = "treatment") val treatment: String? = null,
    @ColumnInfo(name = "prescription") val prescription: String? = null,
    // Blockchain fields (added in v2)
    @ColumnInfo(name = "txHash") val txHash: String? = null,
    @ColumnInfo(name = "ipfsCid") val ipfsCid: String? = null,
    @ColumnInfo(name = "recordHash") val recordHash: String? = null,
    @ColumnInfo(name = "blockchainVerified") val blockchainVerified: Boolean = false,
    @ColumnInfo(name = "chainTimestamp") val chainTimestamp: String? = null
)
