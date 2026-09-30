package com.example.notification

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
     */
    fun showQuickAddInputNotification(context: Context) {
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (isId) "Pencatatan Cepat" else "Quick Add")
            .setContentText(if (isId) "Ketik transaksi langsung dari notifikasi." else "Type transactions directly from notification.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Silent priority
            .setSilent(true) // Explicitly silent
            .addAction(replyAction)
            .setAutoCancel(false)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    /**
     * Updates notification to Processing / Loading state while AI evaluates.
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    /**
     * Shows Success state with summary and auto-resets / dismisses.
     */
    fun showSuccessNotification(context: Context, summary: String) {
        val prefs = context.getSharedPreferences("security_settings", Context.MODE_PRIVATE)
        val isId = prefs.getString("app_language", "en") == "id"
        val isQuickAddEnabled = prefs.getBoolean("quick_add_notif_enabled", false)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(if (isId) "Transaksi Berhasil Dicatat" else "Transaction Recorded")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setTimeoutAfter(4000)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            // If user still has quick add enabled, restore the input notification after brief feedback
            if (isQuickAddEnabled) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val stillEnabled = context.getSharedPreferences("security_settings", Context.MODE_PRIVATE)
                        .getBoolean("quick_add_notif_enabled", false)
                    if (stillEnabled) {
                        showQuickAddInputNotification(context)
                    }
                }, 4200)
            }
        } catch (_: SecurityException) {}
    }

    /**
     * Shows Error state with reason.
     */
    fun showErrorNotification(context: Context, errorMessage: String) {
        val isId = context.getSharedPreferences("security_settings", Context.MODE_PRIVATE)
            .getString("app_language", "en") == "id"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(if (isId) "Gagal Mencatat Transaksi" else "Transaction Failed")
            .setContentText(errorMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setTimeoutAfter(6000)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }
}
