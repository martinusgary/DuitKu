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

    private suspend fun adjustWalletBalanceInternal(walletId: Int, diff: Double) {
        val wallet = financeDao.getWalletById(walletId)
        if (wallet != null) {
            val newBalance = (wallet.balance + diff).coerceAtLeast(0.0)
            financeDao.updateWallet(wallet.copy(balance = newBalance))
        }
    }

    suspend fun insertTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        // Insert transaction
        financeDao.insertTransaction(transaction)
        
        // Update wallet balance based on transaction type
        adjustWalletBalance(transaction, isReversal = false)
    }

    suspend fun deleteTransaction(transaction: Transaction, refund: Boolean) = withContext(Dispatchers.IO) {
        if (refund) {
            // Reverse wallet balance contribution first
            adjustWalletBalance(transaction, isReversal = true)
        }
        
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
        val fee = tx.adminFee
        when (tx.type) {
            "INCOME" -> {
                val netIncome = (amount - fee).coerceAtLeast(0.0)
                val balanceDiff = if (isReversal) -netIncome else netIncome
                adjustWalletBalanceInternal(tx.walletId, balanceDiff)
            }
            "EXPENSE" -> {
                val totalExpense = amount + fee
                val balanceDiff = if (isReversal) totalExpense else -totalExpense
                adjustWalletBalanceInternal(tx.walletId, balanceDiff)
            }
            "TRANSFER" -> {
                // Source Wallet (decrease on transfer: amount + fee, increase on reversal)
                val totalSourceDeduction = amount + fee
                val sourceDiff = if (isReversal) totalSourceDeduction else -totalSourceDeduction
                adjustWalletBalanceInternal(tx.walletId, sourceDiff)

                // Target Wallet (increase on transfer: amount only without fee, decrease on reversal)
                if (tx.targetWalletId != null) {
                    val targetDiff = if (isReversal) -amount else amount
                    adjustWalletBalanceInternal(tx.targetWalletId, targetDiff)
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
            val cleanJson = jsonStr.trim()
            if (cleanJson.isEmpty()) return@withContext false

            var parsedWallets: List<Wallet> = emptyList()
            var parsedCategories: List<Category> = emptyList()
            var parsedTransactions: List<Transaction> = emptyList()
            var parsedDebts: List<Debt> = emptyList()
            var parsedBills: List<Bill> = emptyList()

            // 1. Try Moshi adapter first
            try {
                val backup = backupAdapter.fromJson(cleanJson)
                if (backup != null) {
                    parsedWallets = backup.wallets
                    parsedCategories = backup.categories
                    parsedTransactions = backup.transactions
                    parsedDebts = backup.debts
                    parsedBills = backup.bills
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. If Moshi returned empty or threw, parse via org.json.JSONObject as fallback
            if (parsedWallets.isEmpty() && parsedCategories.isEmpty() && parsedTransactions.isEmpty()) {
                try {
                    val root = org.json.JSONObject(cleanJson)
                    
                    if (root.has("wallets")) {
                        val arr = root.getJSONArray("wallets")
                        val list = mutableListOf<Wallet>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                Wallet(
                                    id = obj.optInt("id", 0),
                                    name = obj.optString("name", "Dompet"),
                                    balance = obj.optDouble("balance", 0.0),
                                    icon = obj.optString("icon", "wallet")
                                )
                            )
                        }
                        parsedWallets = list
                    }

                    if (root.has("categories")) {
                        val arr = root.getJSONArray("categories")
                        val list = mutableListOf<Category>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                Category(
                                    id = obj.optInt("id", 0),
                                    name = obj.optString("name", "Lain-lain"),
                                    type = obj.optString("type", "EXPENSE")
                                )
                            )
                        }
                        parsedCategories = list
                    }

                    if (root.has("transactions")) {
                        val arr = root.getJSONArray("transactions")
                        val list = mutableListOf<Transaction>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                Transaction(
                                    id = obj.optInt("id", 0),
                                    amount = obj.optDouble("amount", 0.0),
                                    date = obj.optLong("date", System.currentTimeMillis()),
                                    walletId = obj.optInt("walletId", 1),
                                    categoryId = obj.optInt("categoryId", 1),
                                    type = obj.optString("type", "EXPENSE"),
                                    note = obj.optString("note", ""),
                                    targetWalletId = if (obj.has("targetWalletId") && !obj.isNull("targetWalletId")) obj.getInt("targetWalletId") else null,
                                    adminFee = obj.optDouble("adminFee", 0.0)
                                )
                            )
                        }
                        parsedTransactions = list
                    }

                    if (root.has("debts")) {
                        val arr = root.getJSONArray("debts")
                        val list = mutableListOf<Debt>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                Debt(
                                    id = obj.optInt("id", 0),
                                    personName = obj.optString("personName", ""),
                                    totalAmount = obj.optDouble("totalAmount", 0.0),
                                    remainingAmount = obj.optDouble("remainingAmount", 0.0),
                                    dueDate = obj.optLong("dueDate", System.currentTimeMillis()),
                                    type = obj.optString("type", "HUTANG"),
                                    notes = obj.optString("notes", "")
                                )
                            )
                        }
                        parsedDebts = list
                    }

                    if (root.has("bills")) {
                        val arr = root.getJSONArray("bills")
                        val list = mutableListOf<Bill>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                Bill(
                                    id = obj.optInt("id", 0),
                                    name = obj.optString("name", ""),
                                    amount = obj.optDouble("amount", 0.0),
                                    dueDateValue = obj.optString("dueDateValue", ""),
                                    status = obj.optString("status", "BELUM_DIBAYAR")
                                )
                            )
                        }
                        parsedBills = list
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Ensure we parsed at least something meaningful or non-empty valid JSON structure
            if (parsedWallets.isEmpty() && parsedCategories.isEmpty() && parsedTransactions.isEmpty() && parsedDebts.isEmpty() && parsedBills.isEmpty()) {
                // Return false only if absolutely no data could be extracted
                return@withContext false
            }

            // If parsed wallets are empty but transactions exist, auto-create a default wallet
            val finalWallets = if (parsedWallets.isEmpty() && parsedTransactions.isNotEmpty()) {
                val distinctWalletIds = parsedTransactions.map { it.walletId }.distinct()
                distinctWalletIds.map { wId ->
                    Wallet(id = wId, name = "Dompet $wId", balance = 0.0, icon = "wallet")
                }
            } else {
                parsedWallets
            }

            // Verify wallet balances: if a wallet has 0 balance but transactions exist, calculate net balance
            val calculatedWallets = finalWallets.map { w ->
                if (w.balance == 0.0 && parsedTransactions.isNotEmpty()) {
                    var net = 0.0
                    for (t in parsedTransactions) {
                        if (t.walletId == w.id) {
                            when (t.type) {
                                "INCOME" -> net += (t.amount - t.adminFee).coerceAtLeast(0.0)
                                "EXPENSE" -> net -= (t.amount + t.adminFee)
                                "TRANSFER" -> net -= (t.amount + t.adminFee)
                            }
                        }
                        if (t.targetWalletId == w.id && t.type == "TRANSFER") {
                            net += t.amount
                        }
                    }
                    if (net > 0.0) w.copy(balance = net) else w
                } else {
                    w
                }
            }

            // Perform atomic database replacement
            financeDao.clearAllWallets()
            financeDao.clearAllCategories()
            financeDao.clearAllTransactions()
            financeDao.clearAllDebts()
            financeDao.clearAllBills()

            for (w in calculatedWallets) financeDao.insertWallet(w)
            for (c in parsedCategories) financeDao.insertCategory(c)
            for (t in parsedTransactions) financeDao.insertTransaction(t)
            for (d in parsedDebts) financeDao.insertDebt(d)
            for (b in parsedBills) financeDao.insertBill(b)

            // If default categories are now missing, ensure base categories are restored
            prepDefaultDataIfNeeded()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
