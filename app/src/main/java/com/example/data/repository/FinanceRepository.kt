package com.example.data.repository

import android.util.Log
import com.example.data.dao.FinanceDao
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class FinanceRepository(private val financeDao: FinanceDao) {

    val wallets: Flow<List<Wallet>> = financeDao.getAllWallets()
    val categories: Flow<List<Category>> = financeDao.getAllCategories()
    val transactions: Flow<List<Transaction>> = financeDao.getAllTransactions()
    val debts: Flow<List<Debt>> = financeDao.getAllDebts()
    val activeDebts: Flow<List<Debt>> = financeDao.getActiveDebts()
    val archivedDebts: Flow<List<Debt>> = financeDao.getArchivedDebts()
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
        financeDao.insertTransaction(transaction)
        adjustWalletBalance(transaction, isReversal = false)
    }

    suspend fun deleteTransaction(transaction: Transaction, refund: Boolean) = withContext(Dispatchers.IO) {
        if (refund) {
            adjustWalletBalance(transaction, isReversal = true)
        }
        financeDao.deleteTransaction(transaction)
    }

    suspend fun updateTransaction(newTransaction: Transaction) = withContext(Dispatchers.IO) {
        val oldTransaction = financeDao.getTransactionById(newTransaction.id)
        if (oldTransaction != null) {
            adjustWalletBalance(oldTransaction, isReversal = true)
        }
        financeDao.updateTransaction(newTransaction)
        adjustWalletBalance(newTransaction, isReversal = false)
    }

    private suspend fun adjustWalletBalance(transaction: Transaction, isReversal: Boolean) {
        val multiplier = if (isReversal) -1.0 else 1.0
        val totalCost = transaction.amount + transaction.adminFee

        when (transaction.type) {
            "INCOME" -> {
                val netIncome = (transaction.amount - transaction.adminFee).coerceAtLeast(0.0)
                adjustWalletBalanceInternal(transaction.walletId, netIncome * multiplier)
            }
            "EXPENSE" -> {
                adjustWalletBalanceInternal(transaction.walletId, -totalCost * multiplier)
            }
            "TRANSFER" -> {
                adjustWalletBalanceInternal(transaction.walletId, -totalCost * multiplier)
                transaction.targetWalletId?.let { targetId ->
                    adjustWalletBalanceInternal(targetId, transaction.amount * multiplier)
                }
            }
        }
    }

    // --- CRUD OPERATIONS ---

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

            var parsedWallets = mutableListOf<Wallet>()
            var parsedCategories = mutableListOf<Category>()
            var parsedTransactions = mutableListOf<Transaction>()
            var parsedDebts = mutableListOf<Debt>()
            var parsedBills = mutableListOf<Bill>()

            // 1. Try Moshi adapter first
            try {
                val backup = backupAdapter.fromJson(cleanJson)
                if (backup != null) {
                    if (backup.wallets.isNotEmpty()) parsedWallets.addAll(backup.wallets)
                    if (backup.categories.isNotEmpty()) parsedCategories.addAll(backup.categories)
                    if (backup.transactions.isNotEmpty()) parsedTransactions.addAll(backup.transactions)
                    if (backup.debts.isNotEmpty()) parsedDebts.addAll(backup.debts)
                    if (backup.bills.isNotEmpty()) parsedBills.addAll(backup.bills)

                    Log.d("FinanceRepository", "Moshi parsed -> Wallets: ${parsedWallets.size}, Categories: ${parsedCategories.size}, Transactions: ${parsedTransactions.size}, Debts: ${parsedDebts.size}, Bills: ${parsedBills.size}")

                    if (parsedWallets.isNotEmpty() || parsedTransactions.isNotEmpty() || parsedCategories.isNotEmpty() || parsedDebts.isNotEmpty() || parsedBills.isNotEmpty()) {
                        financeDao.clearAllWallets()
                        financeDao.clearAllCategories()
                        financeDao.clearAllTransactions()
                        financeDao.clearAllDebts()
                        financeDao.clearAllBills()

                        if (parsedWallets.isNotEmpty()) financeDao.insertWallets(parsedWallets)
                        if (parsedCategories.isNotEmpty()) financeDao.insertCategories(parsedCategories)
                        if (parsedTransactions.isNotEmpty()) financeDao.insertTransactions(parsedTransactions)
                        if (parsedDebts.isNotEmpty()) financeDao.insertDebts(parsedDebts)
                        if (parsedBills.isNotEmpty()) financeDao.insertBills(parsedBills)

                        prepDefaultDataIfNeeded()
                        Log.d("FinanceRepository", "Database overwritten successfully with parsed Moshi lists.")
                        return@withContext true
                    }
                }
            } catch (e: Exception) {
                Log.e("FinanceRepository", "Moshi parsing failed: ${e.message}", e)
            }

            // 2. If standard Moshi didn't populate completely or format differs, use robust JSON parser
            if (parsedTransactions.isEmpty() && parsedWallets.isEmpty()) {
                try {
                    if (cleanJson.startsWith("[")) {
                        // Root is an array of objects
                        val rootArray = JSONArray(cleanJson)
                        parseArrayDirectly(rootArray, parsedWallets, parsedCategories, parsedTransactions, parsedDebts, parsedBills)
                    } else {
                        var root = JSONObject(cleanJson)
                        // Check if nested in "data", "backup", "payload", "items"
                        if (!root.has("transactions") && !root.has("wallets") && !root.has("categories")) {
                            for (key in listOf("data", "backup", "payload", "items", "records", "result")) {
                                if (root.has(key)) {
                                    val candidate = root.optJSONObject(key)
                                    if (candidate != null) {
                                        root = candidate
                                        break
                                    }
                                }
                            }
                        }

                        // Parse Wallets
                        val walletKeys = listOf("wallets", "dompet", "accounts", "rekening", "walletList")
                        for (wk in walletKeys) {
                            if (root.has(wk)) {
                                val arr = root.optJSONArray(wk)
                                if (arr != null) {
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.optJSONObject(i) ?: continue
                                        parsedWallets.add(
                                            Wallet(
                                                id = parseFlexibleInt(obj, listOf("id", "wallet_id", "walletId"), 0),
                                                name = parseFlexibleString(obj, listOf("name", "wallet_name", "nama", "title"), "Dompet"),
                                                balance = parseFlexibleDouble(obj, listOf("balance", "saldo", "amount", "currentBalance"), 0.0),
                                                icon = parseFlexibleString(obj, listOf("icon", "icon_name", "type"), "wallet")
                                            )
                                        )
                                    }
                                    break
                                }
                            }
                        }

                        // Parse Categories
                        val catKeys = listOf("categories", "kategori", "categoryList")
                        for (ck in catKeys) {
                            if (root.has(ck)) {
                                val arr = root.optJSONArray(ck)
                                if (arr != null) {
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.optJSONObject(i) ?: continue
                                        val cTypeRaw = parseFlexibleString(obj, listOf("type", "category_type", "categoryType", "tipe"), "EXPENSE")
                                        val normalizedType = if (cTypeRaw.contains("INCOME", ignoreCase = true) || cTypeRaw.contains("PEMASUKAN", ignoreCase = true)) "INCOME" else "EXPENSE"
                                        parsedCategories.add(
                                            Category(
                                                id = parseFlexibleInt(obj, listOf("id", "category_id", "categoryId"), 0),
                                                name = parseFlexibleString(obj, listOf("name", "category_name", "nama", "title"), "Lain-lain"),
                                                type = normalizedType
                                            )
                                        )
                                    }
                                    break
                                }
                            }
                        }

                        // Parse Transactions
                        val txKeys = listOf("transactions", "transaksi", "records", "items", "history", "txList")
                        for (tk in txKeys) {
                            if (root.has(tk)) {
                                val arr = root.optJSONArray(tk)
                                if (arr != null) {
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.optJSONObject(i) ?: continue
                                        val rawType = parseFlexibleString(obj, listOf("type", "tx_type", "tipe", "transactionType"), "EXPENSE").uppercase(Locale.ROOT)
                                        val normalizedType = when {
                                            rawType.contains("INCOME") || rawType.contains("PEMASUKAN") || obj.optBoolean("isIncome", false) || obj.optBoolean("is_income", false) -> "INCOME"
                                            rawType.contains("TRANSFER") || rawType.contains("PINDAH") -> "TRANSFER"
                                            else -> "EXPENSE"
                                        }

                                        val targetWId = if (obj.has("targetWalletId") && !obj.isNull("targetWalletId")) {
                                            obj.optInt("targetWalletId")
                                        } else if (obj.has("target_wallet_id") && !obj.isNull("target_wallet_id")) {
                                            obj.optInt("target_wallet_id")
                                        } else null

                                        parsedTransactions.add(
                                            Transaction(
                                                id = parseFlexibleInt(obj, listOf("id", "tx_id", "transactionId"), 0),
                                                amount = parseFlexibleDouble(obj, listOf("amount", "nominal", "total", "value"), 0.0),
                                                date = parseFlexibleDate(obj, listOf("date", "timestamp", "tanggal", "created_at", "createdAt")),
                                                walletId = parseFlexibleInt(obj, listOf("walletId", "wallet_id", "wallet", "dompet_id"), 1),
                                                categoryId = parseFlexibleInt(obj, listOf("categoryId", "category_id", "category", "kategori_id"), 1),
                                                type = normalizedType,
                                                note = parseFlexibleString(obj, listOf("note", "notes", "keterangan", "catatan", "description", "title"), ""),
                                                targetWalletId = targetWId,
                                                adminFee = parseFlexibleDouble(obj, listOf("adminFee", "admin_fee", "fee", "biaya_admin"), 0.0)
                                            )
                                        )
                                    }
                                    break
                                }
                            }
                        }

                        // Parse Debts
                        val debtKeys = listOf("debts", "hutang", "piutang", "debtList")
                        for (dk in debtKeys) {
                            if (root.has(dk)) {
                                val arr = root.optJSONArray(dk)
                                if (arr != null) {
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.optJSONObject(i) ?: continue
                                        val rawDType = parseFlexibleString(obj, listOf("type", "debt_type", "tipe"), "HUTANG").uppercase(Locale.ROOT)
                                        val normalizedDType = if (rawDType.contains("PIUTANG") || rawDType.contains("LENT") || rawDType.contains("RECEIVABLE")) "PIUTANG" else "HUTANG"
                                        val totalAmt = parseFlexibleDouble(obj, listOf("totalAmount", "total_amount", "amount", "nominal"), 0.0)
                                        val remAmt = parseFlexibleDouble(obj, listOf("remainingAmount", "remaining_amount", "remaining", "sisa"), totalAmt)
                                        parsedDebts.add(
                                            Debt(
                                                id = parseFlexibleInt(obj, listOf("id", "debt_id"), 0),
                                                personName = parseFlexibleString(obj, listOf("personName", "person_name", "name", "nama", "contact"), ""),
                                                totalAmount = totalAmt,
                                                remainingAmount = remAmt,
                                                dueDate = parseFlexibleDate(obj, listOf("dueDate", "due_date", "deadline", "jatuh_tempo")),
                                                type = normalizedDType,
                                                notes = parseFlexibleString(obj, listOf("notes", "note", "keterangan", "catatan"), "")
                                            )
                                        )
                                    }
                                    break
                                }
                            }
                        }

                        // Parse Bills
                        val billKeys = listOf("bills", "tagihan", "billList")
                        for (bk in billKeys) {
                            if (root.has(bk)) {
                                val arr = root.optJSONArray(bk)
                                if (arr != null) {
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.optJSONObject(i) ?: continue
                                        val rawStatus = parseFlexibleString(obj, listOf("status", "payment_status"), "BELUM_DIBAYAR").uppercase(Locale.ROOT)
                                        val normalizedStatus = if (rawStatus.contains("LUNAS") || rawStatus.contains("PAID")) "LUNAS" else "BELUM_DIBAYAR"
                                        parsedBills.add(
                                            Bill(
                                                id = parseFlexibleInt(obj, listOf("id", "bill_id"), 0),
                                                name = parseFlexibleString(obj, listOf("name", "bill_name", "nama", "title"), ""),
                                                amount = parseFlexibleDouble(obj, listOf("amount", "nominal", "biaya"), 0.0),
                                                dueDateValue = parseFlexibleString(obj, listOf("dueDateValue", "due_date_value", "dueDate", "due_date", "jadwal"), ""),
                                                status = normalizedStatus
                                            )
                                        )
                                    }
                                    break
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Ensure we parsed at least something meaningful or non-empty valid JSON structure
            if (parsedWallets.isEmpty() && parsedCategories.isEmpty() && parsedTransactions.isEmpty() && parsedDebts.isEmpty() && parsedBills.isEmpty()) {
                return@withContext false
            }

            // If parsed wallets are empty but transactions exist, auto-create wallets for IDs used in transactions
            val finalWallets = if (parsedWallets.isEmpty() && parsedTransactions.isNotEmpty()) {
                val distinctWalletIds = (parsedTransactions.map { it.walletId } + parsedTransactions.mapNotNull { it.targetWalletId }).distinct()
                distinctWalletIds.map { wId ->
                    Wallet(id = if (wId > 0) wId else 1, name = if (wId == 1) "Dompet Utama" else "Dompet $wId", balance = 0.0, icon = "wallet")
                }.ifEmpty {
                    listOf(Wallet(id = 1, name = "Dompet Utama", balance = 0.0, icon = "wallet"))
                }
            } else if (parsedWallets.isEmpty()) {
                listOf(Wallet(id = 1, name = "Dompet Utama", balance = 0.0, icon = "wallet"))
            } else {
                parsedWallets
            }

            // Verify wallet balances: if a wallet has 0 balance but transactions exist, calculate net balance from ledger
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
            Log.d("FinanceRepository", "Fallback commit -> Wallets: ${calculatedWallets.size}, Categories: ${parsedCategories.size}, Transactions: ${parsedTransactions.size}")
            financeDao.clearAllWallets()
            financeDao.clearAllCategories()
            financeDao.clearAllTransactions()
            financeDao.clearAllDebts()
            financeDao.clearAllBills()

            if (calculatedWallets.isNotEmpty()) financeDao.insertWallets(calculatedWallets)
            if (parsedCategories.isNotEmpty()) financeDao.insertCategories(parsedCategories)
            if (parsedTransactions.isNotEmpty()) financeDao.insertTransactions(parsedTransactions)
            if (parsedDebts.isNotEmpty()) financeDao.insertDebts(parsedDebts)
            if (parsedBills.isNotEmpty()) financeDao.insertBills(parsedBills)

            // If default categories are now missing, ensure base categories are restored
            prepDefaultDataIfNeeded()

            Log.d("FinanceRepository", "Fallback database restore completed successfully.")
            true
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Fatal failure during importFromJson: ${e.message}", e)
            false
        }
    }

    private fun parseArrayDirectly(
        rootArray: JSONArray,
        parsedWallets: MutableList<Wallet>,
        parsedCategories: MutableList<Category>,
        parsedTransactions: MutableList<Transaction>,
        parsedDebts: MutableList<Debt>,
        parsedBills: MutableList<Bill>
    ) {
        for (i in 0 until rootArray.length()) {
            val obj = rootArray.optJSONObject(i) ?: continue
            if (obj.has("amount") && (obj.has("walletId") || obj.has("wallet_id") || obj.has("categoryId") || obj.has("category_id") || obj.has("type"))) {
                val rawType = parseFlexibleString(obj, listOf("type", "tx_type", "tipe"), "EXPENSE").uppercase(Locale.ROOT)
                val normalizedType = when {
                    rawType.contains("INCOME") || rawType.contains("PEMASUKAN") -> "INCOME"
                    rawType.contains("TRANSFER") || rawType.contains("PINDAH") -> "TRANSFER"
                    else -> "EXPENSE"
                }
                parsedTransactions.add(
                    Transaction(
                        id = parseFlexibleInt(obj, listOf("id", "tx_id"), 0),
                        amount = parseFlexibleDouble(obj, listOf("amount", "nominal"), 0.0),
                        date = parseFlexibleDate(obj, listOf("date", "timestamp", "tanggal")),
                        walletId = parseFlexibleInt(obj, listOf("walletId", "wallet_id", "wallet"), 1),
                        categoryId = parseFlexibleInt(obj, listOf("categoryId", "category_id", "category"), 1),
                        type = normalizedType,
                        note = parseFlexibleString(obj, listOf("note", "notes", "keterangan", "catatan", "description"), ""),
                        targetWalletId = if (obj.has("targetWalletId")) obj.optInt("targetWalletId") else null,
                        adminFee = parseFlexibleDouble(obj, listOf("adminFee", "admin_fee"), 0.0)
                    )
                )
            } else if (obj.has("balance") || (obj.has("icon") && obj.has("name"))) {
                parsedWallets.add(
                    Wallet(
                        id = parseFlexibleInt(obj, listOf("id", "wallet_id"), 0),
                        name = parseFlexibleString(obj, listOf("name", "wallet_name", "nama"), "Dompet"),
                        balance = parseFlexibleDouble(obj, listOf("balance", "saldo"), 0.0),
                        icon = parseFlexibleString(obj, listOf("icon", "icon_name"), "wallet")
                    )
                )
            }
        }
    }

    private fun parseFlexibleInt(obj: JSONObject, keys: List<String>, default: Int): Int {
        for (k in keys) {
            if (obj.has(k)) {
                val v = obj.opt(k)
                if (v is Number) return v.toInt()
                if (v is String) {
                    val parsed = v.trim().toIntOrNull()
                    if (parsed != null) return parsed
                }
            }
        }
        return default
    }

    private fun parseFlexibleDouble(obj: JSONObject, keys: List<String>, default: Double): Double {
        for (k in keys) {
            if (obj.has(k)) {
                val v = obj.opt(k)
                if (v is Number) return v.toDouble()
                if (v is String) {
                    val clean = v.replace("Rp", "", ignoreCase = true)
                        .replace("IDR", "", ignoreCase = true)
                        .replace("$", "")
                        .replace(" ", "")
                        .replace(".", "")
                        .replace(",", ".")
                        .trim()
                    val parsed = clean.toDoubleOrNull()
                    if (parsed != null) return parsed
                }
            }
        }
        return default
    }

    private fun parseFlexibleString(obj: JSONObject, keys: List<String>, default: String): String {
        for (k in keys) {
            if (obj.has(k)) {
                val v = obj.optString(k, "")
                if (v.isNotEmpty() && v != "null") return v
            }
        }
        return default
    }

    private fun parseFlexibleDate(obj: JSONObject, keys: List<String>): Long {
        for (k in keys) {
            if (obj.has(k)) {
                val v = obj.opt(k)
                if (v is Number) {
                    val l = v.toLong()
                    // If in seconds instead of milliseconds
                    return if (l < 10000000000L) l * 1000L else l
                }
                if (v is String && v.isNotEmpty()) {
                    val l = v.toLongOrNull()
                    if (l != null) return if (l < 10000000000L) l * 1000L else l
                    // Try parsing date string formats
                    val formats = listOf(
                        "yyyy-MM-dd'T'HH:mm:ss",
                        "yyyy-MM-dd HH:mm:ss",
                        "yyyy-MM-dd",
                        "dd/MM/yyyy HH:mm",
                        "dd/MM/yyyy",
                        "dd-MM-yyyy"
                    )
                    for (fmt in formats) {
                        try {
                            val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                            val d = sdf.parse(v)
                            if (d != null) return d.time
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        return System.currentTimeMillis()
    }
}
