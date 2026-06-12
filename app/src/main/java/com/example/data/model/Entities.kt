package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "wallets")
@JsonClass(generateAdapter = true)
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double,
    val icon: String // e.g. "wallet", "credit_card", "cash", "bank", "savings"
)

@Entity(tableName = "categories")
@JsonClass(generateAdapter = true)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String // "INCOME" (Pemasukan) or "EXPENSE" (Pengeluaran)
)

@Entity(tableName = "transactions")
@JsonClass(generateAdapter = true)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val date: Long, // timestamp
    val walletId: Int,
    val categoryId: Int,
    val type: String, // "INCOME", "EXPENSE", "TRANSFER"
    val note: String,
    val targetWalletId: Int? = null // only for TRANSFER
)

@Entity(tableName = "debts")
@JsonClass(generateAdapter = true)
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val dueDate: Long, // timestamp
    val type: String, // "HUTANG" (owed by me), "PIUTANG" (owed to me)
    val notes: String
)

@Entity(tableName = "bills")
@JsonClass(generateAdapter = true)
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val dueDateValue: String, // e.g., "Setiap tanggal 15", "15 Juli 2026"
    val status: String // "BELUM_DIBAYAR" (Unpaid), "LUNAS" (Paid)
)
