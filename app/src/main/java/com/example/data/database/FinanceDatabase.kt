package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FinanceDao
import com.example.data.model.*

@Database(
    entities = [
        Wallet::class,
        Category::class,
        Transaction::class,
        Debt::class,
        Bill::class
    ],
    version = 3,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {

    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure debts table exists
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `debts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `personName` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `remainingAmount` REAL NOT NULL,
                        `dueDate` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                """.trimIndent())

                // Ensure bills table exists
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bills` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `dueDateValue` TEXT NOT NULL,
                        `status` TEXT NOT NULL
                    )
                """.trimIndent())

                // Ensure targetWalletId and adminFee columns exist in transactions
                try {
                    db.execSQL("ALTER TABLE `transactions` ADD COLUMN `targetWalletId` INTEGER DEFAULT NULL")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `transactions` ADD COLUMN `adminFee` REAL NOT NULL DEFAULT 0.0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add isArchived to debts
                try {
                    db.execSQL("ALTER TABLE `debts` ADD COLUMN `isArchived` INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}

                // Add targetLimit and isLimitless to wallets
                try {
                    db.execSQL("ALTER TABLE `wallets` ADD COLUMN `targetLimit` REAL DEFAULT NULL")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `wallets` ADD COLUMN `isLimitless` INTEGER NOT NULL DEFAULT 1")
                } catch (_: Exception) {}

                // Add lastPaidMonth, lastPaidDate, and lastPaidWalletId to bills
                try {
                    db.execSQL("ALTER TABLE `bills` ADD COLUMN `lastPaidMonth` INTEGER NOT NULL DEFAULT -1")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `bills` ADD COLUMN `lastPaidDate` INTEGER DEFAULT NULL")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `bills` ADD COLUMN `lastPaidWalletId` INTEGER DEFAULT NULL")
                } catch (_: Exception) {}

                // Add debtId, billId, installmentNumber to transactions
                try {
                    db.execSQL("ALTER TABLE `transactions` ADD COLUMN `debtId` INTEGER DEFAULT NULL")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `transactions` ADD COLUMN `billId` INTEGER DEFAULT NULL")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `transactions` ADD COLUMN `installmentNumber` INTEGER DEFAULT NULL")
                } catch (_: Exception) {}
            }
        }

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
