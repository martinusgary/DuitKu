package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val wallets: List<Wallet> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val bills: List<Bill> = emptyList()
)
