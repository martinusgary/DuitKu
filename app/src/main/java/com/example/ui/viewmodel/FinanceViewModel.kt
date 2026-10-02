package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
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

private val Context.dataStore by preferencesDataStore(name = "finance_preferences")

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository

    companion object {
        val KEY_MONTHLY_VARIABLE_BUDGET = doublePreferencesKey("monthly_variable_budget")
    }

    // Jetpack DataStore Flow for Daily Variable Budget
    val dailyVariableBudget: StateFlow<Double> = getApplication<Application>()
        .dataStore
        .data
        .map { preferences ->
            preferences[KEY_MONTHLY_VARIABLE_BUDGET] ?: 0.0
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val monthlyVariableBudget: StateFlow<Double> get() = dailyVariableBudget

    fun setDailyVariableBudget(amount: Double) {
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { preferences ->
                preferences[KEY_MONTHLY_VARIABLE_BUDGET] = amount.coerceAtLeast(0.0)
            }
        }
    }

    fun setMonthlyVariableBudget(amount: Double) = setDailyVariableBudget(amount)

    val wallets: StateFlow<List<Wallet>>
    val categories: StateFlow<List<Category>>
    val transactions: StateFlow<List<Transaction>>
    val debts: StateFlow<List<Debt>>
    val activeDebts: StateFlow<List<Debt>>
    val archivedDebts: StateFlow<List<Debt>>
    val bills: StateFlow<List<Bill>>

    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    val appLanguage = MutableStateFlow(getSavedLanguage())

    val isAmountsHidden = MutableStateFlow(getSavedAmountsHidden())

    val appTheme = MutableStateFlow(getSavedTheme())

    val uiStyle = MutableStateFlow(getSavedUiStyle())

    val userGreetingName = MutableStateFlow(getSavedGreetingName())

    private fun getSavedGreetingName(): String {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        return prefs.getString("user_greeting_name", "Sobat Duit") ?: "Sobat Duit"
    }

    fun setUserGreetingName(name: String) {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("user_greeting_name", name).apply()
        userGreetingName.value = name
    }

    private fun getSavedUiStyle(): String {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        return prefs.getString("app_ui_style", "SOLID") ?: "SOLID"
    }

    fun setUiStyle(style: String) {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("app_ui_style", "SOLID").apply()
        uiStyle.value = "SOLID"
    }

    private fun getSavedTheme(): String {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        val defaultTheme = if (com.google.android.material.color.DynamicColors.isDynamicColorAvailable()) "DYNAMIC" else "CLASSIC"
        return prefs.getString("app_theme", defaultTheme) ?: defaultTheme
    }

    fun setAppTheme(theme: String) {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("app_theme", theme).apply()
        appTheme.value = theme
    }

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
        refreshQuickAddNotificationIfActive()
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
            packageInfo.versionName ?: "1.4"
        } catch (e: Exception) {
            "1.4"
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

        activeDebts = repository.activeDebts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        archivedDebts = repository.archivedDebts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        bills = repository.bills.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Try to prefill default data if db is brand new and run auto-maintenance checks
        viewModelScope.launch {
            repository.prepDefaultDataIfNeeded()
            checkAndArchiveZeroDebts()
            checkAndResetMonthlyBills()
        }
    }

    // --- METRICS CALCULATION ---

    /**
     * Total Balance:
     * Calculates the sum of all wallets except those categorized as savings (icon == "savings").
     */
    val totalBalance: Flow<Double> = wallets.map { list ->
        list.filter { it.icon != "savings" }.sumOf { it.balance }
    }

    val monthlyIncomeSum: Flow<Double> = transactions.map { list ->
        list.filter { it.type == "INCOME" && isCurrentMonth(it.date) }
            .sumOf { it.amount }
    }

    val monthlyExpenseSum: Flow<Double> = transactions.map { list ->
        list.filter { isCurrentMonth(it.date) }
            .sumOf {
                when (it.type) {
                    "EXPENSE" -> it.amount + it.adminFee
                    "TRANSFER" -> it.adminFee
                    else -> 0.0
                }
            }
    }

    /**
     * Total Variable Expenses This Month:
     * Only calculates expenses with isDailyBudget == true.
     */
    val monthlyVariableExpenseSum: Flow<Double> = transactions.map { list ->
        list.filter { it.type == "EXPENSE" && it.isDailyBudget && isCurrentMonth(it.date) }
            .sumOf { it.amount + it.adminFee }
    }

    /**
     * Total Variable Expenses Today:
     * Expenses made today with isDailyBudget == true.
     */
    val todayVariableExpenseSum: Flow<Double> = transactions.map { list ->
        list.filter { it.type == "EXPENSE" && it.isDailyBudget && isToday(it.date) }
            .sumOf { it.amount + it.adminFee }
    }

    /**
     * Today's Remaining Daily Budget (Direct Daily Budgeting):
     * 1. Start of day budget: set directly by the user (dailyVariableBudget).
     * 2. Decreases dynamically as expenses are recorded today: (Daily Budget - Today's Variable Expenses).
     * 3. Resets automatically every new calendar day (00:00) because today's expenses are filtered by isToday().
     */
    val todayRemainingDailyBudget: Flow<Double> = combine(
        dailyVariableBudget,
        transactions
    ) { dailyBudget, txList ->
        if (dailyBudget <= 0.0) return@combine 0.0

        // Expenses recorded today
        val todayExpenses = txList
            .filter { it.type == "EXPENSE" && it.isDailyBudget && isToday(it.date) }
            .sumOf { it.amount + it.adminFee }

        (dailyBudget - todayExpenses).coerceAtLeast(0.0)
    }

    /**
     * Start-of-day baseline limit allocated for today (equal to dailyVariableBudget)
     */
    val todayStartOfDayBudget: Flow<Double> = dailyVariableBudget.map { it.coerceAtLeast(0.0) }

    // --- TRANSACTION OPERATIONS ---

    fun addTransaction(
        amount: Double,
        type: String,
        walletId: Int,
        categoryId: Int,
        note: String,
        date: Long,
        targetWalletId: Int? = null,
        adminFee: Double = 0.0,
        isDailyBudget: Boolean = true
    ) {
        viewModelScope.launch {
            val finalCategoryId = if (type == "TRANSFER") 0 else categoryId
            val tx = Transaction(
                amount = amount,
                date = date,
                walletId = walletId,
                categoryId = finalCategoryId,
                type = type,
                note = note,
                targetWalletId = targetWalletId,
                adminFee = adminFee,
                isDailyBudget = isDailyBudget
            )
            repository.insertTransaction(tx)
            refreshQuickAddNotificationIfActive()
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            refreshQuickAddNotificationIfActive()
        }
    }

    fun deleteTransaction(transaction: Transaction, refund: Boolean) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction, refund)
            refreshQuickAddNotificationIfActive()
        }
    }

    fun deleteTransactionsBulk(list: List<Transaction>, refund: Boolean) {
        viewModelScope.launch {
            list.forEach { transaction ->
                repository.deleteTransaction(transaction, refund)
            }
            refreshQuickAddNotificationIfActive()
        }
    }

    private fun refreshQuickAddNotificationIfActive() {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("quick_add_notif_enabled", false)) {
            val isId = prefs.getString("app_language", "en") == "id"
            viewModelScope.launch {
                val budgetBadge = com.example.notification.QuickAddNotificationHelper.getRemainingDailyBudgetInfo(context, isId)
                com.example.notification.QuickAddNotificationHelper.showQuickAddInputNotification(context, budgetBadge = budgetBadge)
            }
        }
    }

    // --- WALLET OPERATIONS ---
    fun addWallet(name: String, balance: Double, icon: String, targetLimit: Double? = null, isLimitless: Boolean = true) {
        viewModelScope.launch {
            repository.insertWallet(
                Wallet(
                    name = name,
                    balance = balance,
                    icon = icon,
                    targetLimit = targetLimit,
                    isLimitless = isLimitless
                )
            )
        }
    }

    fun updateWallet(wallet: Wallet) {
        viewModelScope.launch {
            repository.updateWallet(wallet)
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

    // --- DEBT / LOAN OPERATIONS (INCLUDING REPAY AND CORRESPONDING TRANSACTION LOGGING) ---
    fun addDebt(personName: String, totalAmount: Double, dueDate: Long, type: String, notes: String) {
        viewModelScope.launch {
            val d = Debt(
                personName = personName,
                totalAmount = totalAmount,
                remainingAmount = totalAmount,
                dueDate = dueDate,
                type = type,
                notes = notes,
                isArchived = false
            )
            repository.insertDebt(d)
        }
    }

    fun payDebtInstallment(debt: Debt, amountPaid: Double, walletId: Int, note: String) {
        viewModelScope.launch {
            if (amountPaid <= 0) return@launch

            val newRemaining = (debt.remainingAmount - amountPaid).coerceAtLeast(0.0)
            val isNowArchived = (newRemaining <= 0.0)
            val updatedDebt = debt.copy(remainingAmount = newRemaining, isArchived = isNowArchived)
            repository.updateDebt(updatedDebt)

            // Log corresponding transaction
            // If HUTANG (I owe money) and I pay: it is money going OUT of my wallet (EXPENSE)
            // If PIUTANG (They owe me) and they pay: it is money coming INTO my wallet (INCOME)
            val txType = if (debt.type == "HUTANG") "EXPENSE" else "INCOME"
            
            // Calculate sequence of installment
            val priorPayments = transactions.value.count { 
                it.debtId == debt.id || (it.note.contains("Cicilan") && it.note.contains(debt.personName))
            }
            val installmentSeq = priorPayments + 1

            // Try to find a tagihan/hutang category or generic "Lain-lain" (or create one)
            val matchingCategories = categories.value
            val defaultCat = matchingCategories.firstOrNull { 
                it.type == txType && (it.name.contains("Hutang", true) || it.name.contains("Tagihan", true) || it.name.contains("Lain-lain", true))
            } ?: matchingCategories.firstOrNull { it.type == txType }
            
            val catId = defaultCat?.id ?: 1

            val extraNote = if (note.isNotBlank()) " - $note" else ""
            val fullNote = "Pembayaran Cicilan ke-$installmentSeq: ${debt.personName}$extraNote"

            val txn = Transaction(
                amount = amountPaid,
                date = System.currentTimeMillis(),
                walletId = walletId,
                categoryId = catId,
                type = txType,
                note = fullNote,
                debtId = debt.id,
                installmentNumber = installmentSeq
            )
            repository.insertTransaction(txn)
        }
    }

    fun unarchiveDebt(debt: Debt) {
        viewModelScope.launch {
            repository.updateDebt(debt.copy(isArchived = false))
        }
    }

    fun checkAndArchiveZeroDebts() {
        viewModelScope.launch {
            debts.value.forEach { debt ->
                if (debt.remainingAmount <= 0.0 && !debt.isArchived) {
                    repository.updateDebt(debt.copy(isArchived = true))
                }
            }
        }
    }

    fun checkAndResetMonthlyBills() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentMonthKey = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
            bills.value.forEach { bill ->
                if (bill.status == "LUNAS" && bill.lastPaidMonth != -1 && bill.lastPaidMonth < currentMonthKey) {
                    repository.updateBill(bill.copy(status = "BELUM_DIBAYAR"))
                }
            }
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
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            val currentMonthKey = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)

            val updatedBill = bill.copy(
                status = "LUNAS",
                lastPaidMonth = currentMonthKey,
                lastPaidDate = now,
                lastPaidWalletId = walletId
            )
            repository.updateBill(updatedBill)

            // Determine sequence/frequency
            val priorPayments = transactions.value.count {
                it.billId == bill.id || (it.note.contains("Tagihan") && it.note.contains(bill.name))
            }
            val paymentSeq = priorPayments + 1

            // Log corresponding EXPENSE transaction
            val matchingCategories = categories.value
            val tagihanCat = matchingCategories.firstOrNull {
                it.type == "EXPENSE" && (it.name.contains("Tagihan", true) || it.name.contains("Utilities", true) || it.name.contains("Lain-lain", true))
            } ?: matchingCategories.firstOrNull { it.type == "EXPENSE" }

            val catId = tagihanCat?.id ?: 1

            val txn = Transaction(
                amount = bill.amount,
                date = now,
                walletId = walletId,
                categoryId = catId,
                type = "EXPENSE",
                note = "Bayar Tagihan ke-$paymentSeq: ${bill.name}",
                billId = bill.id,
                installmentNumber = paymentSeq
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

    suspend fun getEncryptedBackup(): String {
        val rawJson = repository.exportToJson()
        return com.example.ui.util.CryptoHelper.encrypt(rawJson)
    }

    fun importBackupJson(jsonStr: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val trimmed = jsonStr.trim()
            var dataToImport = trimmed
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                val decrypted = com.example.ui.util.CryptoHelper.decrypt(trimmed)
                if (decrypted.isNotEmpty()) {
                    dataToImport = decrypted
                }
            }
            val result = repository.importFromJson(dataToImport)
            _importStatus.value = if (result) "Data berhasil diimpor!" else "Gagal mengimpor data. Format salah."
            onComplete(result)
        }
    }

    fun importEncryptedBackup(encryptedStr: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val trimmed = encryptedStr.trim()
            var dataToImport = trimmed
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                val decrypted = com.example.ui.util.CryptoHelper.decrypt(trimmed)
                if (decrypted.isNotEmpty()) {
                    dataToImport = decrypted
                }
            }
            val result = repository.importFromJson(dataToImport)
            _importStatus.value = if (result) "Data berhasil dikembalikan dari cadangan!" else "Gagal mengimpor data. Format salah atau berkas rusak."
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

    private fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val txCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return txCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
               txCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun isPriorDaysInCurrentMonth(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val txCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val isSameYear = txCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        val isSameMonth = txCal.get(Calendar.MONTH) == today.get(Calendar.MONTH)
        val isPriorDay = txCal.get(Calendar.DAY_OF_MONTH) < today.get(Calendar.DAY_OF_MONTH)
        return isSameYear && isSameMonth && isPriorDay
    }
}
