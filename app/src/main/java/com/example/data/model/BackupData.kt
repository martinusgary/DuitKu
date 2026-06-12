package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val wallets: List<Wallet>,
    val categories: List<Category>,
    val transactions: List<Transaction>,
    val debts: List<Debt>,
    val bills: List<Bill>
)
