package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat

interface PlayerNotificationManager {
    fun enteredForeground(notification: Notification)
    fun notify(notificationId: Int, notification: Notification)
}

class PlayerNotificationManagerImpl(context: Context) : PlayerNotificationManager {
    private val notificationManager = NotificationManagerCompat.from(context)

    override fun enteredForeground(notification: Notification) {
        // Not used, just used for a testing point
    }

    override fun notify(notificationId: Int, notification: Notification) {
        notificationManager.notify(notificationId, notification)
    }
}
