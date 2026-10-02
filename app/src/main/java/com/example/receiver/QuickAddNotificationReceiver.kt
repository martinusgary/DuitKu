package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.example.BuildConfig
import com.example.data.database.FinanceDatabase
import com.example.data.model.Transaction
import com.example.notification.QuickAddNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class QuickAddNotificationReceiver : BroadcastReceiver() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    data class ParsedTx(
        val type: String,
        val amount: Double,
        val wallet: String?,
        val source_wallet: String?,
        val dest_wallet: String?,
        val category: String?,
        val note: String?
    )

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // If user or OS attempted to swipe/dismiss while feature is still enabled in Settings, immediately restore it
        if (action == QuickAddNotificationHelper.ACTION_DISMISSED) {
            val prefs = context.getSharedPreferences("security_settings", Context.MODE_PRIVATE)
            val isQuickAddEnabled = prefs.getBoolean("quick_add_notif_enabled", false)
            if (isQuickAddEnabled) {
                val appContext = context.applicationContext
                val isId = appContext.getSharedPreferences("security_settings", Context.MODE_PRIVATE)
                    .getString("app_language", "en") == "id"
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val budgetBadge = QuickAddNotificationHelper.getRemainingDailyBudgetInfo(appContext, isId)
                        QuickAddNotificationHelper.showQuickAddInputNotification(appContext, budgetBadge = budgetBadge)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            return
        }

        if (action != QuickAddNotificationHelper.ACTION_REPLY) return

        // 1. Extract text from RemoteInput
        val remoteInputBundle = RemoteInput.getResultsFromIntent(intent)
        val userQuery = remoteInputBundle?.getCharSequence(QuickAddNotificationHelper.KEY_TEXT_REPLY)?.toString()

        if (userQuery.isNullOrBlank()) {
            return
        }

        // 2. Immediate feedback: update notification to Processing state
        QuickAddNotificationHelper.showProcessingNotification(context)

        // 3. Keep receiver alive with goAsync()
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        val isId = appContext.getSharedPreferences("security_settings", Context.MODE_PRIVATE)
            .getString("app_language", "en") == "id"

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Generous 25-second timeout for complex multi-transaction reasoning
                withTimeout(25_000L) {
                    processTransactionInBackground(appContext, userQuery, isId)
                }

                // Keep summary visible for 2.5 seconds, then auto-reset to default idle state with fresh realtime budget
                kotlinx.coroutines.delay(2500L)
                val budgetBadge = QuickAddNotificationHelper.getRemainingDailyBudgetInfo(appContext, isId)
                QuickAddNotificationHelper.resetToDefaultInput(appContext, budgetBadge = budgetBadge)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                QuickAddNotificationHelper.showErrorNotification(
                    appContext,
                    if (isId) "Koneksi terputus. Silakan coba lagi." else "Connection timed out. Please try again."
                )
                kotlinx.coroutines.delay(3500L)
                val budgetBadge = QuickAddNotificationHelper.getRemainingDailyBudgetInfo(appContext, isId)
                QuickAddNotificationHelper.resetToDefaultInput(appContext, budgetBadge = budgetBadge)
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: (if (isId) "Kesalahan input" else "Input error")
                QuickAddNotificationHelper.showErrorNotification(
                    appContext,
                    if (isId) "Gagal memproses: $errorMsg" else "Failed to process: $errorMsg"
                )
                kotlinx.coroutines.delay(4000L)
                val budgetBadge = QuickAddNotificationHelper.getRemainingDailyBudgetInfo(appContext, isId)
                QuickAddNotificationHelper.resetToDefaultInput(appContext, budgetBadge = budgetBadge)
            } finally {
                // Always finish pendingResult to prevent ANR and release system wakelocks
                pendingResult.finish()
            }
        }
    }

    private suspend fun processTransactionInBackground(context: Context, userInput: String, isId: Boolean) {
        val database = FinanceDatabase.getDatabase(context)
        val dao = database.financeDao()

        // 1. Dynamic Data Layer: fetch active wallets and categories directly from Room
        val wallets = dao.getAllWalletsDirect()
        val categories = dao.getAllCategoriesDirect()

        val walletNames = wallets.map { it.name }
        val expenseCategoryNames = categories.filter { it.type == "EXPENSE" }.map { it.name }
        val incomeCategoryNames = categories.filter { it.type == "INCOME" }.map { it.name }
        val defaultWallet = wallets.firstOrNull()?.name ?: "Cash"

        // 2. Exact current date & time formatted
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
        val readableFormat = SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm", Locale("id", "ID"))
        val now = Date()
        val currentTimeIso = isoFormat.format(now)
        val currentTimeReadable = readableFormat.format(now)

        // 3. Construct System Prompt that handles single or multiple combined transactions
        val systemPrompt = """
            You are an expert Indonesian financial assistant for the "DuitKu" app.
            Your job is to parse the user's natural language input into a JSON array of transactions.
            User may input SINGLE or MULTIPLE transactions in a single sentence connected by 'lalu', 'kemudian', 'dan', 'setelah itu', etc.

            CONTEXT:
            - Current Time: $currentTimeReadable ($currentTimeIso)
            - Available Wallets: ${walletNames.joinToString(", ")}
            - Available Expense Categories: ${expenseCategoryNames.joinToString(", ")}
            - Available Income Categories: ${incomeCategoryNames.joinToString(", ")}
            - Default Wallet: $defaultWallet

            RULES:
            1. Output MUST ALWAYS be a valid JSON array only: [ {...}, {...} ]
            2. Each transaction object fields:
               - "type": "EXPENSE" | "INCOME" | "TRANSFER"
               - "amount": number (MUST be greater than 0. If user forgot amount for something like esteh/kopi, give reasonable standard estimate like 5000 or 10000).
               - "wallet": string (for EXPENSE or INCOME, match against Available Wallets)
               - "source_wallet": string or null (for TRANSFER, source account)
               - "dest_wallet": string or null (for TRANSFER, destination account)
               - "category": string (match against Available Categories)
               - "note": string (clean concise description)
            3. For amount parsing, handle informal Indonesian slang:
               '25rb' -> 25000, '1.5jt' -> 1500000, '50k' -> 50000, 'cepek' -> 100000, 'goceng' -> 5000, 'ceban' -> 10000, 'noban' -> 20000.
            4. If user says "transfer dari A ke B" without amount, check if another amount is mentioned or estimate.
            5. Return PURE JSON array only. NO markdown ticks, NO conversational commentary.
        """.trimIndent()

        // 4. Gemini AI REST API
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException(if (isId) "Kunci API Gemini belum disetel" else "Gemini API key is not configured")
        }

        val payload = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        val textPart = JSONObject().apply {
                            put("text", userInput)
                        }
                        put(textPart)
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val systemInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                }
                put("parts", partsArray)
            }
            put("systemInstruction", systemInstructionObj)

            val generationConfigObj = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1f)
            }
            put("generationConfig", generationConfigObj)
        }

        val requestBody = payload.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            if (code == 429) {
                throw IllegalStateException(if (isId) "Kuota AI penuh (429). Tunggu beberapa detik." else "AI Rate limit reached (429). Please wait a moment.")
            }
            throw IllegalStateException("AI Error $code")
        }

        val respBody = response.body?.string() ?: throw IllegalStateException("Empty AI response")
        val jsonResp = JSONObject(respBody)
        val candidates = jsonResp.optJSONArray("candidates")
        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text")?.trim()
            ?: throw IllegalStateException(if (isId) "Format balasan AI tidak sesuai" else "Invalid AI response structure")

        // 5. Robust JSON Parser (handles array [...], object {...}, or wrapped {"transactions": [...]})
        val cleanedJson = rawText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val jsonArray = when {
            cleanedJson.startsWith("[") -> JSONArray(cleanedJson)
            cleanedJson.startsWith("{") -> {
                val obj = JSONObject(cleanedJson)
                when {
                    obj.has("transactions") -> obj.optJSONArray("transactions") ?: JSONArray().put(obj)
                    obj.has("data") -> obj.optJSONArray("data") ?: JSONArray().put(obj)
                    else -> JSONArray().put(obj)
                }
            }
            else -> throw IllegalArgumentException(if (isId) "Teks tidak dapat dikenali sebagai transaksi" else "Could not recognize transaction structure")
        }

        if (jsonArray.length() == 0) {
            throw IllegalArgumentException(if (isId) "Tidak ada transaksi yang terdeteksi dari teks Anda." else "No transactions detected.")
        }

        val parsedItems = mutableListOf<ParsedTx>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            var amt = obj.optDouble("amount", 0.0)
            if (amt <= 0.0) {
                // If amount is missing/zero, try to detect any numbers from userInput as fallback
                val numbers = Regex("""\d+""").findAll(userInput).map { it.value.toDoubleOrNull() ?: 0.0 }.toList()
                if (numbers.isNotEmpty()) {
                    amt = numbers.getOrNull(i) ?: numbers.first()
                }
            }

            parsedItems.add(
                ParsedTx(
                    type = obj.optString("type", "EXPENSE").uppercase(),
                    amount = amt,
                    wallet = obj.optString("wallet").takeIf { it.isNotBlank() },
                    source_wallet = obj.optString("source_wallet").takeIf { it.isNotBlank() },
                    dest_wallet = obj.optString("dest_wallet").takeIf { it.isNotBlank() },
                    category = obj.optString("category").takeIf { it.isNotBlank() },
                    note = obj.optString("note").takeIf { it.isNotBlank() }
                )
            )
        }

        // 6. Map and Insert directly into Room Database & Update Wallet Balance
        val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        val summaryBuilder = StringBuilder()
        var insertedCount = 0

        for (item in parsedItems) {
            if (item.amount <= 0) continue

            if (item.type == "TRANSFER") {
                val srcWallet = wallets.find { it.name.equals(item.source_wallet ?: item.wallet, ignoreCase = true) }
                    ?: wallets.firstOrNull()
                val dstWallet = wallets.find { it.name.equals(item.dest_wallet, ignoreCase = true) }

                val srcWalletId = srcWallet?.id ?: 1
                val dstWalletId = dstWallet?.id

                val transferTx = Transaction(
                    walletId = srcWalletId,
                    categoryId = 0,
                    amount = item.amount,
                    type = "TRANSFER",
                    date = now.time,
                    note = item.note ?: "Transfer dana",
                    targetWalletId = dstWalletId,
                    adminFee = 0.0,
                    isDailyBudget = false
                )
                dao.insertTransaction(transferTx)

                // Update balances
                srcWallet?.let { dao.adjustWalletBalanceSql(it.id, -item.amount) }
                dstWallet?.let { dao.adjustWalletBalanceSql(it.id, item.amount) }

                val srcName = srcWallet?.name ?: "Dompet"
                val dstName = dstWallet?.name ?: "Tujuan"
                val formattedAmt = rupiahFormat.format(item.amount).replace("Rp", "Rp ")
                summaryBuilder.append("Transfer $formattedAmt: $srcName → $dstName\n")
                insertedCount++
            } else {
                val matchedWallet = wallets.find { it.name.equals(item.wallet, ignoreCase = true) }
                    ?: wallets.firstOrNull()
                val matchedCategory = categories.find {
                    it.name.equals(item.category, ignoreCase = true) && it.type == item.type
                } ?: categories.find { it.type == item.type }

                val walletId = matchedWallet?.id ?: 1
                val categoryId = matchedCategory?.id ?: 1

                val tx = Transaction(
                    walletId = walletId,
                    categoryId = categoryId,
                    amount = item.amount,
                    type = item.type,
                    date = now.time,
                    note = item.note ?: userInput,
                    adminFee = 0.0,
                    isDailyBudget = item.type == "EXPENSE"
                )
                dao.insertTransaction(tx)

                val diff = if (item.type == "INCOME") item.amount else -item.amount
                dao.adjustWalletBalanceSql(walletId, diff)

                val sign = if (item.type == "INCOME") "+" else "-"
                val wName = matchedWallet?.name ?: "Cash"
                val formattedAmt = rupiahFormat.format(item.amount).replace("Rp", "Rp ")
                summaryBuilder.append("${item.note ?: "Transaksi"}: $sign$formattedAmt ($wName)\n")
                insertedCount++
            }
        }

        if (insertedCount == 0) {
            throw IllegalArgumentException(if (isId) "Nominal belum dicantumkan. Contoh: beli esteh 5rb lalu transfer 50rb" else "Missing amount. Example: buy tea 5k then transfer 50k")
        }

        // 7. Calculate fresh remaining daily budget synchronously (zero battery drain)
        val budgetBadge = QuickAddNotificationHelper.getRemainingDailyBudgetInfo(context, isId)

        // 8. Show success feedback on notification with updated remaining daily limit
        QuickAddNotificationHelper.showSuccessNotification(
            context,
            summaryBuilder.toString().trim(),
            budgetBadge = budgetBadge
        )
    }
}
