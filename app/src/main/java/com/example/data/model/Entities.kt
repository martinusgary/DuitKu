package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "wallets")
@JsonClass(generateAdapter = true)
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "Dompet Utama",
    val balance: Double = 0.0,
    val icon: String = "wallet" // e.g. "wallet", "credit_card", "cash", "bank", "savings"
)

@Entity(tableName = "categories")
@JsonClass(generateAdapter = true)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "Lain-lain",
    val type: String = "EXPENSE" // "INCOME" (Pemasukan) or "EXPENSE" (Pengeluaran)
)

@Entity(tableName = "transactions")
@JsonClass(generateAdapter = true)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(), // timestamp
    val walletId: Int = 1,
    val categoryId: Int = 1,
    val type: String = "EXPENSE", // "INCOME", "EXPENSE", "TRANSFER"
    val note: String = "",
    val targetWalletId: Int? = null, // only for TRANSFER
    val adminFee: Double = 0.0
)

@Entity(tableName = "debts")
@JsonClass(generateAdapter = true)
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String = "",
    val totalAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val dueDate: Long = System.currentTimeMillis(), // timestamp
    val type: String = "HUTANG", // "HUTANG" (owed by me), "PIUTANG" (owed to me)
    val notes: String = ""
)

@Entity(tableName = "bills")
@JsonClass(generateAdapter = true)
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val amount: Double = 0.0,
    val dueDateValue: String = "", // e.g., "Setiap tanggal 15", "15 Juli 2026"
    val status: String = "BELUM_DIBAYAR" // "BELUM_DIBAYAR" (Unpaid), "LUNAS" (Paid)
)
