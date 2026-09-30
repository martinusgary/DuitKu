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
        if (intent.action != QuickAddNotificationHelper.ACTION_REPLY) return

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
                // Strict 15-second hard timeout for battery saving & fast fail
                withTimeout(15_000L) {
                    processTransactionInBackground(appContext, userQuery, isId)
                }

                // Keep summary visible for 2.5 seconds, then auto-reset to default idle state
                kotlinx.coroutines.delay(2500L)
                QuickAddNotificationHelper.resetToDefaultInput(appContext)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                QuickAddNotificationHelper.showErrorNotification(
                    appContext,
                    if (isId) "Koneksi terputus. Silakan coba lagi." else "Connection timed out. Please try again."
                )
                kotlinx.coroutines.delay(2500L)
                QuickAddNotificationHelper.resetToDefaultInput(appContext)
            } catch (e: Exception) {
                QuickAddNotificationHelper.showErrorNotification(
                    appContext,
                    if (isId) "Data tidak dapat diproses: ${e.localizedMessage ?: "Kesalahan input"}" else "Could not process data: ${e.localizedMessage ?: "Input error"}"
                )
                kotlinx.coroutines.delay(2500L)
                QuickAddNotificationHelper.resetToDefaultInput(appContext)
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

        // 3. Construct System Prompt
        val systemPrompt = """
            You are a strict Indonesian financial parser assistant for the "DuitKu" app.
            Your job is to parse the user's natural language input into a JSON array of transactions.

            CONTEXT:
            - Current Time: $currentTimeReadable ($currentTimeIso)
            - Available Wallets: ${walletNames.joinToString(", ")}
            - Available Expense Categories: ${expenseCategoryNames.joinToString(", ")}
            - Available Income Categories: ${incomeCategoryNames.joinToString(", ")}
            - Default Wallet: $defaultWallet

            RULES:
            1. Output MUST BE a pure valid JSON array only: [{"type":"EXPENSE"|"INCOME"|"TRANSFER", "amount": 25000, "wallet":"$defaultWallet", "source_wallet":null, "dest_wallet":null, "category":"Makanan & Minuman", "note":"Beli nasi goreng", "timestamp":"$currentTimeIso"}]
            2. Match wallet and category to the available lists if possible. If no match, choose the closest logical one.
            3. For amount, parse informal Indonesian expressions (e.g. '25rb' -> 25000, '1.5jt' -> 1500000, '50k' -> 50000, 'cepek' -> 100000, 'goceng' -> 5000).
            4. Do not include markdown ticks or explanation. Return pure JSON array.
        """.trimIndent()

        // 4. Gemini AI REST API with responseMimeType = "application/json" and gemini-2.5-flash
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key Gemini belum disetel di Secrets")
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
            val errBody = response.body?.string() ?: ""
            throw IllegalStateException("AI Error ${response.code}: $errBody")
        }

        val respBody = response.body?.string() ?: throw IllegalStateException("Empty AI response")
        val jsonResp = JSONObject(respBody)
        val candidates = jsonResp.optJSONArray("candidates")
        val content = candidates?.optJSONObject(0)?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text")?.trim()
            ?: throw IllegalStateException("Format balasan AI tidak sesuai")

        // 5. Parse JSON array
        val cleanedJson = rawText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val jsonArray = JSONArray(cleanedJson)
        if (jsonArray.length() == 0) {
            throw IllegalArgumentException("Tidak ada transaksi yang terdeteksi dari teks Anda.")
        }

        val parsedItems = mutableListOf<ParsedTx>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            parsedItems.add(
                ParsedTx(
                    type = obj.optString("type", "EXPENSE").uppercase(),
                    amount = obj.optDouble("amount", 0.0),
                    wallet = obj.optString("wallet").takeIf { it.isNotBlank() },
                    source_wallet = obj.optString("source_wallet").takeIf { it.isNotBlank() },
                    dest_wallet = obj.optString("dest_wallet").takeIf { it.isNotBlank() },
                    category = obj.optString("category").takeIf { it.isNotBlank() },
                    note = obj.optString("note").takeIf { it.isNotBlank() }
                )
            )
        }

        // 6. Map and Insert directly into Room Database & Update Wallet Balance
        val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val summaryBuilder = StringBuilder()

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
                summaryBuilder.append("Transfer ${rupiahFormat.format(item.amount)}: $srcName ke $dstName\n")
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
                summaryBuilder.append("${item.note ?: "Transaksi"}: $sign${rupiahFormat.format(item.amount)} ($wName)\n")
            }
        }

        // 7. Show success feedback on notification and auto-dismiss
        QuickAddNotificationHelper.showSuccessNotification(
            context,
            summaryBuilder.toString().trim()
        )
    }
}
