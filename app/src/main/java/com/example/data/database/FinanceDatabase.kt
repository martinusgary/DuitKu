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
    version = 2,
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

        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
