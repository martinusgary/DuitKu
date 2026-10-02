package com.example.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.R
import com.example.data.database.FinanceDatabase
import com.example.receiver.QuickAddNotificationReceiver
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "finance_preferences")
private val KEY_MONTHLY_VARIABLE_BUDGET = doublePreferencesKey("monthly_variable_budget")

object QuickAddNotificationHelper {

    const val CHANNEL_ID = "duitku_quick_add_channel"
    const val NOTIFICATION_ID = 2026
    const val KEY_TEXT_REPLY = "key_quick_add_text_reply"
    const val ACTION_REPLY = "com.example.duitku.ACTION_QUICK_ADD_REPLY"
    const val ACTION_DISMISSED = "com.example.duitku.ACTION_QUICK_ADD_DISMISSED"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Quick Add"
            val descriptionText = "Pencatatan transaksi cepat dari bilah status"
            val importance = NotificationManager.IMPORTANCE_LOW // Silent notification by default
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Dismisses/cancels the Quick Add notification.
     */
    fun cancelQuickAddNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }

    /**
     * Efficiently reads daily budget limit and today's expenses from Room DB and DataStore.
     * ZERO battery impact: No background services, no repeating alarms, no loops.
     * Evaluated synchronously only when notification is rendered or updated.
     */
    suspend fun getRemainingDailyBudgetInfo(context: Context, isId: Boolean): String? {
        return try {
            val dailyBudget = context.dataStore.data.map { it[KEY_MONTHLY_VARIABLE_BUDGET] ?: 0.0 }.firstOrNull() ?: 0.0
            if (dailyBudget <= 0.0) return null

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = calendar.timeInMillis

            val database = FinanceDatabase.getDatabase(context)
            val allTxs = database.financeDao().getAllTransactionsDirect()
            val todayExpenses = allTxs.filter {
                it.type == "EXPENSE" && it.isDailyBudget && it.date in startOfDay until endOfDay
            }.sumOf { it.amount + it.adminFee }

            val remaining = (dailyBudget - todayExpenses).coerceAtLeast(0.0)
            val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                maximumFractionDigits = 0
            }
            val formatted = rupiahFormat.format(remaining).replace("Rp", "Rp ")
            if (isId) "Sisa limit harian: $formatted" else "Remaining daily limit: $formatted"
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Builds and shows the interactive Quick Add notification in status bar & lockscreen.
     * If [customStatusText] is provided, it displays that text (e.g. recent transaction summary)
     * while keeping the remote input action alive and notification pinned (ongoing).
     */
    fun showQuickAddInputNotification(
        context: Context,
        customStatusText: String? = null,
        budgetBadge: String? = null
    ) {
        createNotificationChannel(context)

        val isId = context.getSharedPreferences("security_settings", Context.MODE_PRIVATE)
            .getString("app_language", "en") == "id"

        val replyLabel = if (isId) "Tulis transaksi..." else "Type transaction..."
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        val replyIntent = Intent(context, QuickAddNotificationReceiver::class.java).apply {
            action = ACTION_REPLY
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_input_add,
            if (isId) "Catat Cepat" else "Quick Add",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAuthenticationRequired(false)
            .build()

        val deleteIntent = Intent(context, QuickAddNotificationReceiver::class.java).apply {
            action = ACTION_DISMISSED
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val defaultSubtext = if (isId) "Ketik transaksi langsung dari notifikasi." else "Type transactions directly from notification."
        
        // Header / Title: Keep clean without budget
        val titleText = if (customStatusText != null) {
            if (isId) "Transaksi Dicatat" else "Transaction Recorded"
        } else {
            if (isId) "Pencatatan Cepat" else "Quick Add"
        }

        // Body Content: Place remaining daily budget limit in the body
        val contentText = when {
            customStatusText != null && !budgetBadge.isNullOrBlank() -> {
                "$customStatusText\n$budgetBadge"
            }
            customStatusText != null -> {
                customStatusText
            }
            !budgetBadge.isNullOrBlank() -> {
                "$budgetBadge\n$defaultSubtext"
            }
            else -> {
                defaultSubtext
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setOngoing(true) // ALWAYS pinned like TeraBox / WhatsApp, never dismissed by OS
            .setShowWhen(false) // Instructs system to not display timestamp
            .setWhen(0L) // Crucial for ColorOS / Realme UI / OxygenOS / MIUI to completely remove "Now"
            .setPriority(NotificationCompat.PRIORITY_LOW) // Silent priority
            .setSilent(true) // Explicitly silent
            .addAction(replyAction)
            .setAutoCancel(false)
            .setDeleteIntent(deletePendingIntent) // Auto-restores if user/system tries to swipe

        // BigTextStyle ensures multi-line body (custom text + budget badge) is fully visible
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))

        val notification = builder.build().apply {
            // Low-level OS flags to prevent dismissal on OEM systems
            flags = flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    /**
     * Updates notification to Processing / Loading state while AI evaluates.
     * Retains ongoing = true so it never disappears during computation.
     */
    fun showProcessingNotification(context: Context) {
        val isId = context.getSharedPreferences("security_settings", Context.MODE_PRIVATE)
            .getString("app_language", "en") == "id"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(if (isId) "Memproses Transaksi" else "Processing Transaction")
            .setContentText(if (isId) "Menyimpan data transaksi..." else "Saving transaction data...")
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setShowWhen(false)
            .setWhen(0L)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    /**
     * WhatsApp-style Success update:
     * Immediately keeps the notification pinned (ongoing = true) with the RemoteInput active,
     * showing the transaction summary and updated budget badge.
     */
    fun showSuccessNotification(context: Context, summary: String, budgetBadge: String? = null) {
        // Immediately show the summary while keeping the notification alive with RemoteInput
        showQuickAddInputNotification(context, customStatusText = summary, budgetBadge = budgetBadge)
    }

    /**
     * Resets the notification content back to default idle text with current budget badge.
     */
    fun resetToDefaultInput(context: Context, budgetBadge: String? = null) {
        val prefs = context.getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        val isQuickAddEnabled = prefs.getBoolean("quick_add_notif_enabled", false)
        if (isQuickAddEnabled) {
            showQuickAddInputNotification(context, customStatusText = null, budgetBadge = budgetBadge)
        }
    }

    /**
     * Shows Error state with reason while keeping ongoing = true with RemoteInput.
     */
    fun showErrorNotification(context: Context, errorMessage: String) {
        showQuickAddInputNotification(context, customStatusText = errorMessage)
    }
}
