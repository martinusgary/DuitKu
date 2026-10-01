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
import com.example.R
import com.example.receiver.QuickAddNotificationReceiver

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
     * Builds and shows the interactive Quick Add notification in status bar & lockscreen.
     * If [customStatusText] is provided, it displays that text (e.g. recent transaction summary)
     * while keeping the remote input action alive and notification pinned (ongoing).
     */
    fun showQuickAddInputNotification(context: Context, customStatusText: String? = null) {
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
        val titleText = if (customStatusText != null) {
            if (isId) "Transaksi Dicatat" else "Transaction Recorded"
        } else {
            if (isId) "Pencatatan Cepat" else "Quick Add"
        }
        val contentText = customStatusText ?: defaultSubtext

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

        if (customStatusText != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(customStatusText))
        }

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
     * showing the transaction summary.
     */
    fun showSuccessNotification(context: Context, summary: String) {
        // Immediately show the summary while keeping the notification alive with RemoteInput
        showQuickAddInputNotification(context, customStatusText = summary)
    }

    /**
     * Resets the notification content back to default idle text.
     */
    fun resetToDefaultInput(context: Context) {
        val prefs = context.getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        val isQuickAddEnabled = prefs.getBoolean("quick_add_notif_enabled", false)
        if (isQuickAddEnabled) {
            showQuickAddInputNotification(context, customStatusText = null)
        }
    }

    /**
     * Shows Error state with reason while keeping ongoing = true with RemoteInput.
     */
    fun showErrorNotification(context: Context, errorMessage: String) {
        showQuickAddInputNotification(context, customStatusText = errorMessage)
    }
}
