package com.secureehr.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE medical_records ADD COLUMN txHash TEXT")
        database.execSQL("ALTER TABLE medical_records ADD COLUMN ipfsCid TEXT")
        database.execSQL("ALTER TABLE medical_records ADD COLUMN recordHash TEXT")
        database.execSQL("ALTER TABLE medical_records ADD COLUMN blockchainVerified INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE medical_records ADD COLUMN chainTimestamp TEXT")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS consent_audit (
                id TEXT NOT NULL PRIMARY KEY,
                consentId TEXT NOT NULL,
                doctorName TEXT NOT NULL,
                action TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                txHash TEXT NOT NULL DEFAULT '',
                recordHash TEXT NOT NULL DEFAULT '',
                blockchainStatus TEXT NOT NULL DEFAULT 'Pending'
            )
        """)
    }
}

@Database(
    entities = [MedicalRecordEntity::class, ConsentAuditEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicalRecordDao(): MedicalRecordDao
    abstract fun consentAuditDao(): ConsentAuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secureehr_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
