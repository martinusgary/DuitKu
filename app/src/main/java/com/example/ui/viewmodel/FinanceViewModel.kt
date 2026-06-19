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
        return prefs.getString("app_ui_style", "FRESH") ?: "FRESH"
    }

    fun setUiStyle(style: String) {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("app_ui_style", style).apply()
        uiStyle.value = style
    }

    private fun getSavedTheme(): String {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        return prefs.getString("app_theme", "CLASSIC") ?: "CLASSIC"
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

    suspend fun getEncryptedBackup(): String {
        val rawJson = repository.exportToJson()
        return com.example.ui.util.CryptoHelper.encrypt(rawJson)
    }

    fun importBackupJson(jsonStr: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.importFromJson(jsonStr)
            _importStatus.value = if (result) "Data berhasil diimpor!" else "Gagal mengimpor data. Format salah."
            onComplete(result)
        }
    }

    fun importEncryptedBackup(encryptedStr: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val decryptedJson = com.example.ui.util.CryptoHelper.decrypt(encryptedStr.trim())
            val result = if (decryptedJson.isNotEmpty()) {
                repository.importFromJson(decryptedJson)
            } else {
                false
            }
            _importStatus.value = if (result) "Data berhasil dikembalikan dari cadangan terenkripsi!" else "Gagal mengimpor data. Format salah atau berkas rusak."
            onComplete(result)
        }
    }

    fun clearImportStatus() {
        _importStatus.value = null
    }

    // --- GOOGLE DRIVE SYNC SUPPORT (LOCAL SECURE ENGINE) ---



    private val _gdriveSyncState = MutableStateFlow<String?>(null)
    val gdriveSyncState: StateFlow<String?> = _gdriveSyncState.asStateFlow()

    fun getGDriveLastSync(): String? {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        return prefs.getString("gdrive_last_sync", null)
    }

    fun getGDriveAccount(): String? {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        return prefs.getString("gdrive_account", null)
    }

    fun connectGDrive(email: String) {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("gdrive_account", email).apply()
        saveAccountHistory(email)
    }

    fun disconnectGDrive() {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        prefs.edit().remove("gdrive_account").remove("gdrive_last_sync").apply()
    }

    fun fetchCloudBackup(email: String, onComplete: (String?, String?) -> Unit) {
        viewModelScope.launch {
            _gdriveSyncState.value = "LOGGING_IN"
            var cloudRes = readFromCloud(email)
            if (cloudRes == null) {
                cloudRes = readFromLocalFallback(email)
            }
            _gdriveSyncState.value = null
            if (cloudRes != null) {
                onComplete(cloudRes.first, cloudRes.second)
            } else {
                onComplete(null, null)
            }
        }
    }

    fun restoreFromCloud(email: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _gdriveSyncState.value = "RESTORING"
            var cloudRes = readFromCloud(email)
            if (cloudRes == null) {
                cloudRes = readFromLocalFallback(email)
            }
            if (cloudRes != null) {
                val timestamp = cloudRes.first
                val data = cloudRes.second
                val decryptedJson = com.example.ui.util.CryptoHelper.decrypt(data.trim())
                val result = if (decryptedJson.isNotEmpty()) {
                    repository.importFromJson(decryptedJson)
                } else {
                    false
                }
                if (result) {
                    val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("gdrive_account", email)
                        .putString("gdrive_last_sync", timestamp)
                        .apply()
                }
                _gdriveSyncState.value = null
                onComplete(result)
            } else {
                _gdriveSyncState.value = null
                onComplete(false)
            }
        }
    }

    fun syncGDriveNow(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _gdriveSyncState.value = "SYNCING"
            val email = getGDriveAccount() ?: ""
            if (email.isNotEmpty()) {
                val currentTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
                val encryptedBackup = getEncryptedBackup()
                
                writeToCloud(email, currentTime, encryptedBackup)
                writeToLocalFallback(email, currentTime, encryptedBackup)
                
                val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
                prefs.edit().putString("gdrive_last_sync", currentTime).apply()
                _gdriveSyncState.value = "SUCCESS"
                onComplete(true)
            } else {
                _gdriveSyncState.value = null
                onComplete(false)
            }
        }
    }

    fun clearGDriveSyncState() {
        _gdriveSyncState.value = null
    }

    fun getAccountHistory(): List<String> {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        val localSet = prefs.getStringSet("saved_google_accounts", emptySet())?.toMutableSet() ?: mutableSetOf()
        
        val fileAccounts = readRegistryFromSharedFiles()
        localSet.addAll(fileAccounts)
        
        return localSet.toList().sorted()
    }

    fun saveAccountHistory(email: String) {
        val prefs = getApplication<Application>().getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        val localSet = prefs.getStringSet("saved_google_accounts", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (!localSet.contains(email)) {
            localSet.add(email)
            prefs.edit().putStringSet("saved_google_accounts", localSet).apply()
        }
        writeRegistryToSharedFiles(email)
    }

    private fun writeRegistryToSharedFiles(email: String) {
        try {
            val file = java.io.File("/sdcard/Download/.duitku_accounts.txt")
            val existing = if (file.exists()) file.readLines(Charsets.UTF_8).map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet() else mutableSetOf()
            existing.add(email)
            file.writeText(existing.joinToString("\n"), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readRegistryFromSharedFiles(): List<String> {
        try {
            val file = java.io.File("/sdcard/Download/.duitku_accounts.txt")
            if (file.exists()) {
                return file.readLines(Charsets.UTF_8).map { it.trim() }.filter { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return emptyList()
    }

    private suspend fun writeToCloud(email: String, timestamp: String, encryptedData: String): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val hashed = com.example.ui.util.CryptoHelper.md5(email)
            val urlStr = "https://kvdb.io/duitku_cloud_v5_nzdpfkmgtv4/$hashed"
            val payload = "$timestamp:::$encryptedData"
            var connection: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL(urlStr)
                connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 7000
                connection.readTimeout = 7000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "text/plain")
                connection.outputStream.use { os ->
                    os.write(payload.toByteArray(Charsets.UTF_8))
                    os.flush()
                }
                val responseCode = connection.responseCode
                responseCode in 200..299
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                connection?.disconnect()
            }
        }
    }

    private suspend fun readFromCloud(email: String): Pair<String, String>? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val hashed = com.example.ui.util.CryptoHelper.md5(email)
            val urlStr = "https://kvdb.io/duitku_cloud_v5_nzdpfkmgtv4/$hashed"
            var connection: java.net.HttpURLConnection? = null
            try {
                val url = java.net.URL(urlStr)
                connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 7000
                connection.readTimeout = 7000
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val content = connection.inputStream.bufferedReader().use { it.readText() }
                    val parts = content.split(":::", limit = 2)
                    if (parts.size == 2) {
                        Pair(parts[0], parts[1])
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun writeToLocalFallback(email: String, timestamp: String, encryptedData: String) {
        try {
            val hashed = com.example.ui.util.CryptoHelper.md5(email)
            val file = java.io.File("/sdcard/Download/.duitku_cloud_cache_$hashed")
            file.parentFile?.mkdirs()
            file.writeText("$timestamp:::$encryptedData", Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readFromLocalFallback(email: String): Pair<String, String>? {
        try {
            val hashed = com.example.ui.util.CryptoHelper.md5(email)
            val file = java.io.File("/sdcard/Download/.duitku_cloud_cache_$hashed")
            if (file.exists()) {
                val content = file.readText(Charsets.UTF_8)
                val parts = content.split(":::", limit = 2)
                if (parts.size == 2) {
                    return Pair(parts[0], parts[1])
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
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
