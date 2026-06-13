package com.example.data.repository

import com.example.data.dao.FinanceDao
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FinanceRepository(private val financeDao: FinanceDao) {

    val wallets: Flow<List<Wallet>> = financeDao.getAllWallets()
    val categories: Flow<List<Category>> = financeDao.getAllCategories()
    val transactions: Flow<List<Transaction>> = financeDao.getAllTransactions()
    val debts: Flow<List<Debt>> = financeDao.getAllDebts()
    val bills: Flow<List<Bill>> = financeDao.getAllBills()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val backupAdapter = moshi.adapter(BackupData::class.java)

    // --- CODES FOR AUTO CALCULATING BALANCES ---

    suspend fun insertTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        // Insert transaction
        financeDao.insertTransaction(transaction)
        
        // Update wallet balance based on transaction type
        adjustWalletBalance(transaction, isReversal = false)
    }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        // Reverse wallet balance contribution first
        adjustWalletBalance(transaction, isReversal = true)
        
        // Delete transaction
        financeDao.deleteTransaction(transaction)
    }

    suspend fun updateTransaction(newTransaction: Transaction) = withContext(Dispatchers.IO) {
        val oldTransaction = financeDao.getTransactionById(newTransaction.id)
        if (oldTransaction != null) {
            // 1. Reverse the effect of the old transaction
            adjustWalletBalance(oldTransaction, isReversal = true)
        }
        
        // 2. Save the new transaction
        financeDao.updateTransaction(newTransaction)
        
        // 3. Apply the effect of the new transaction
        adjustWalletBalance(newTransaction, isReversal = false)
    }

    private suspend fun adjustWalletBalance(tx: Transaction, isReversal: Boolean) {
        val amount = tx.amount
        when (tx.type) {
            "INCOME" -> {
                val wallet = financeDao.getWalletById(tx.walletId)
                if (wallet != null) {
                    val balanceDiff = if (isReversal) -amount else amount
                    financeDao.updateWallet(wallet.copy(balance = wallet.balance + balanceDiff))
                }
            }
            "EXPENSE" -> {
                val wallet = financeDao.getWalletById(tx.walletId)
                if (wallet != null) {
                    val balanceDiff = if (isReversal) amount else -amount
                    financeDao.updateWallet(wallet.copy(balance = wallet.balance + balanceDiff))
                }
            }
            "TRANSFER" -> {
                // Source Wallet (decrease on transfer, increase on reversal)
                val sourceWallet = financeDao.getWalletById(tx.walletId)
                if (sourceWallet != null) {
                    val sourceDiff = if (isReversal) amount else -amount
                    financeDao.updateWallet(sourceWallet.copy(balance = sourceWallet.balance + sourceDiff))
                }
                // Target Wallet (increase on transfer, decrease on reversal)
                if (tx.targetWalletId != null) {
                    val targetWallet = financeDao.getWalletById(tx.targetWalletId)
                    if (targetWallet != null) {
                        val targetDiff = if (isReversal) -amount else amount
                        financeDao.updateWallet(targetWallet.copy(balance = targetWallet.balance + targetDiff))
                    }
                }
            }
        }
    }

    // --- GENERAL DATABASE MODIFICATIONS ---

    suspend fun insertWallet(wallet: Wallet) = withContext(Dispatchers.IO) {
        financeDao.insertWallet(wallet)
    }

    suspend fun updateWallet(wallet: Wallet) = withContext(Dispatchers.IO) {
        financeDao.updateWallet(wallet)
    }

    suspend fun deleteWallet(wallet: Wallet) = withContext(Dispatchers.IO) {
        financeDao.deleteWallet(wallet)
    }

    suspend fun insertCategory(category: Category) = withContext(Dispatchers.IO) {
        financeDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        financeDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        financeDao.deleteCategory(category)
    }

    suspend fun insertDebt(debt: Debt) = withContext(Dispatchers.IO) {
        financeDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: Debt) = withContext(Dispatchers.IO) {
        financeDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: Debt) = withContext(Dispatchers.IO) {
        financeDao.deleteDebt(debt)
    }

    suspend fun insertBill(bill: Bill) = withContext(Dispatchers.IO) {
        financeDao.insertBill(bill)
    }

    suspend fun updateBill(bill: Bill) = withContext(Dispatchers.IO) {
        financeDao.updateBill(bill)
    }

    suspend fun deleteBill(bill: Bill) = withContext(Dispatchers.IO) {
        financeDao.deleteBill(bill)
    }

    // --- CHECK & PRE-POPULATE DEFAULT DATA ---

    suspend fun prepDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        val existingCategories = financeDao.getAllCategoriesDirect()
        if (existingCategories.isEmpty()) {
            val defaultCategories = listOf(
                Category(name = "Makanan & Minuman", type = "EXPENSE"),
                Category(name = "Transportasi", type = "EXPENSE"),
                Category(name = "Belanja Harian", type = "EXPENSE"),
                Category(name = "Tagihan & Utilities", type = "EXPENSE"),
                Category(name = "Hiburan", type = "EXPENSE"),
                Category(name = "Gaji", type = "INCOME"),
                Category(name = "Bonus", type = "INCOME"),
                Category(name = "Investasi", type = "INCOME"),
                Category(name = "Klaim & Refund", type = "INCOME"),
                Category(name = "Lain-lain", type = "EXPENSE")
            )
            for (c in defaultCategories) {
                financeDao.insertCategory(c)
            }
        }
    }

    // --- EXPORT AND IMPORT JSON ---

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val backup = BackupData(
            wallets = financeDao.getAllWalletsDirect(),
            categories = financeDao.getAllCategoriesDirect(),
            transactions = financeDao.getAllTransactionsDirect(),
            debts = financeDao.getAllDebtsDirect(),
            bills = financeDao.getAllBillsDirect()
        )
        backupAdapter.toJson(backup)
    }

    suspend fun importFromJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = backupAdapter.fromJson(jsonStr) ?: return@withContext false
            
            // Clear current data first
            financeDao.clearAllWallets()
            financeDao.clearAllCategories()
            financeDao.clearAllTransactions()
            financeDao.clearAllDebts()
            financeDao.clearAllBills()

            // Insert restored values
            for (w in backup.wallets) financeDao.insertWallet(w)
            for (c in backup.categories) financeDao.insertCategory(c)
            for (t in backup.transactions) financeDao.insertTransaction(t)
            for (d in backup.debts) financeDao.insertDebt(d)
            for (b in backup.bills) financeDao.insertBill(b)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
