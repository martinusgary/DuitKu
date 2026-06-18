package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.FinanceDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import com.example.ui.util.UpdateResult
import com.example.ui.util.UpdateChecker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    val wallets: StateFlow<List<Wallet>>
    val categories: StateFlow<List<Category>>
    val transactions: StateFlow<List<Transaction>>
    val debts: StateFlow<List<Debt>>
    val bills: StateFlow<List<Bill>>

    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    val appLanguage = MutableStateFlow(getSavedLanguage())

    val isAmountsHidden = MutableStateFlow(getSavedAmountsHidden())

    private fun getSavedLanguage(): String {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        return prefs.getString("app_language", "en") ?: "en"
    }

    private fun getSavedAmountsHidden(): Boolean {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_amounts_hidden", false)
    }

    fun toggleHideAmounts() {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        val newValue = !isAmountsHidden.value
        prefs.edit().putBoolean("is_amounts_hidden", newValue).apply()
        isAmountsHidden.value = newValue
    }

    fun setLanguage(lang: String) {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("app_language", lang).apply()
        appLanguage.value = lang
    }

    private val _updateResult = MutableStateFlow<UpdateResult?>(null)
    val updateResult: StateFlow<UpdateResult?> = _updateResult.asStateFlow()

    fun getAppVersionName(): String {
        return try {
            val context = getApplication<Application>()
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.2"
        } catch (e: Exception) {
            "1.2"
        }
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            _updateResult.value = null
            val currentVersion = getAppVersionName()
            val result = UpdateChecker.check(currentVersion)
            _updateResult.value = result
        }
    }

    fun clearUpdateState() {
        _updateResult.value = null
    }

    init {
        val database = FinanceDatabase.getDatabase(application)
        repository = FinanceRepository(database.financeDao())

        // Mappings
        wallets = repository.wallets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        categories = repository.categories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        transactions = repository.transactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        debts = repository.debts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        bills = repository.bills.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Try to prefill default data if db is brand new
        viewModelScope.launch {
            repository.prepDefaultDataIfNeeded()
        }
    }

    // --- METRICS CALCULATION ---

    val totalBalance: Flow<Double> = wallets.map { list ->
        list.sumOf { it.balance }
    }

    val monthlyIncomeSum: Flow<Double> = transactions.map { list ->
        list.filter { it.type == "INCOME" && isCurrentMonth(it.date) }
            .sumOf { it.amount }
    }

    val monthlyExpenseSum: Flow<Double> = transactions.map { list ->
        list.filter { it.type == "EXPENSE" && isCurrentMonth(it.date) }
            .sumOf { it.amount }
    }

    // --- TRANSACTION OPERATIONS ---

    fun addTransaction(
        amount: Double,
        type: String,
        walletId: Int,
        categoryId: Int,
        note: String,
        date: Long,
        targetWalletId: Int? = null
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                amount = amount,
                date = date,
                walletId = walletId,
                categoryId = categoryId,
                type = type,
                note = note,
                targetWalletId = targetWalletId
            )
            repository.insertTransaction(tx)
        }
    }

    fun deleteTransaction(transaction: Transaction, refund: Boolean) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction, refund)
        }
    }

    fun deleteTransactionsBulk(list: List<Transaction>, refund: Boolean) {
        viewModelScope.launch {
            list.forEach { transaction ->
                repository.deleteTransaction(transaction, refund)
            }
        }
    }

    // --- WALLET OPERATIONS ---
    fun addWallet(name: String, balance: Double, icon: String) {
        viewModelScope.launch {
            repository.insertWallet(Wallet(name = name, balance = balance, icon = icon))
        }
    }

    fun deleteWallet(wallet: Wallet) {
        viewModelScope.launch {
            repository.deleteWallet(wallet)
        }
    }

    // --- CATEGORY OPERATIONS ---
    fun addCategory(name: String, type: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, type = type))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // --- DEB / LOAN OPERATIONS (INCLUDING REPAY AND CORRESPONDING TRANSACTION LOGGING) ---
    fun addDebt(personName: String, totalAmount: Double, dueDate: Long, type: String, notes: String) {
        viewModelScope.launch {
            val d = Debt(
                personName = personName,
                totalAmount = totalAmount,
                remainingAmount = totalAmount,
                dueDate = dueDate,
                type = type,
                notes = notes
            )
            repository.insertDebt(d)
        }
    }

    fun payDebtInstallment(debt: Debt, amountPaid: Double, walletId: Int, note: String) {
        viewModelScope.launch {
            if (amountPaid <= 0) return@launch

            val newRemaining = (debt.remainingAmount - amountPaid).coerceAtLeast(0.0)
            val updatedDebt = debt.copy(remainingAmount = newRemaining)
            repository.updateDebt(updatedDebt)

            // Log corresponding transaction
            // If HUTANG (I owe money) and I pay: it is money going OUT of my wallet (EXPENSE)
            // If PIUTANG (They owe me) and they pay: it is money coming INTO my wallet (INCOME)
            val txType = if (debt.type == "HUTANG") "EXPENSE" else "INCOME"
            
            // Try to find a tagihan/hutang category or generic "Lain-lain" (or create one)
            val matchingCategories = categories.value
            val isExpense = (txType == "EXPENSE")
            val defaultCat = matchingCategories.firstOrNull { 
                it.type == txType && (it.name.contains("Tagihan", true) || it.name.contains("Lain-lain", true))
            } ?: matchingCategories.firstOrNull { it.type == txType }
            
            val catId = defaultCat?.id ?: 1

            val txn = Transaction(
                amount = amountPaid,
                date = System.currentTimeMillis(),
                walletId = walletId,
                categoryId = catId,
                type = txType,
                note = "Pembayaran Cicilan: ${debt.personName} - $note"
            )
            repository.insertTransaction(txn)
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    // --- BILL OPERATIONS (INCLUDING RECORDING PAYMENT TRANSACTION LOGGING) ---
    fun addBill(name: String, amount: Double, dueDateValue: String) {
        viewModelScope.launch {
            val b = Bill(
                name = name,
                amount = amount,
                dueDateValue = dueDateValue,
                status = "BELUM_DIBAYAR"
            )
            repository.insertBill(b)
        }
    }

    fun payBill(bill: Bill, walletId: Int) {
        viewModelScope.launch {
            val updatedBill = bill.copy(status = "LUNAS")
            repository.updateBill(updatedBill)

            // Log corresponding EXPENSE transaction
            val matchingCategories = categories.value
            val tagihanCat = matchingCategories.firstOrNull {
                it.type == "EXPENSE" && (it.name.contains("Tagihan", true) || it.name.contains("Utilities", true) || it.name.contains("Lain-lain", true))
            } ?: matchingCategories.firstOrNull { it.type == "EXPENSE" }

            val catId = tagihanCat?.id ?: 1

            val txn = Transaction(
                amount = bill.amount,
                date = System.currentTimeMillis(),
                walletId = walletId,
                categoryId = catId,
                type = "EXPENSE",
                note = "Bayar Tagihan: ${bill.name}"
            )
            repository.insertTransaction(txn)
        }
    }

    fun resetBillStatus(bill: Bill) {
        viewModelScope.launch {
            repository.updateBill(bill.copy(status = "BELUM_DIBAYAR"))
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
        }
    }

    // --- BACKUP & RESTORE ACTIONS ---

    suspend fun getBackupJson(): String {
        return repository.exportToJson()
    }

    fun importBackupJson(jsonStr: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.importFromJson(jsonStr)
            _importStatus.value = if (result) "Data berhasil diimpor!" else "Gagal mengimpor data. Format salah."
            onComplete(result)
        }
    }

    fun clearImportStatus() {
        _importStatus.value = null
    }

    // --- UTILITIES FOR SCREEN ---

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.maximumFractionDigits = 0
        // Clean currency symbol and spaced layout
        return format.format(amount).replace("Rp", "Rp ")
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        return sdf.format(Date(timestamp))
    }

    private fun isCurrentMonth(timestamp: Long): Boolean {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val txCal = Calendar.getInstance()
        txCal.timeInMillis = timestamp
        return txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
    }
}
