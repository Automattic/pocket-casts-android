package au.com.shiftyjelly.pocketcasts.voicecontrol.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class VoiceControlNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "voice_control_channel"
        private const val CHANNEL_NAME = "Voice Control"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Create the notification channel for voice control (required for Android O+).
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Voice control listening notification"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(channel)
            Timber.i("[VoicePipeline] notification channel created")
        }
    }

    /**
     * Create a notification indicating that voice control models are being downloaded.
     */
    fun createDownloadingNotification(): Notification {
        createNotificationChannel()

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Voice Control")
            .setContentText("Downloading voice control models…")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true)
            .build()
    }

    /**
     * Update the foreground notification (used once models are ready).
     */
    fun notify(notification: Notification) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Create a notification indicating that voice control is actively listening.
     *
     * @param wakeWordRequired whether the user must say the wake word before speaking a command
     */
    fun createListeningNotification(wakeWordRequired: Boolean): Notification {
        createNotificationChannel()

        val stopIntent = createStopIntent()
        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val contentText = if (wakeWordRequired) {
            "Listening — say wake word to start"
        } else {
            "Listening for voice commands"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Voice Control")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent,
            )
            .build()
    }

    /**
     * Create an intent to stop the voice control service.
     */
    private fun createStopIntent(): Intent {
        return Intent(context, VoiceControlService::class.java).apply {
            action = VoiceControlService.STOP_ACTION
        }
    }

    /**
     * Cancel the voice control notification.
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        Timber.i("Voice control notification cancelled")
    }

    /**
     * Get the notification ID for the voice control service.
     */
    val notificationId: Int
        get() = NOTIFICATION_ID
}
