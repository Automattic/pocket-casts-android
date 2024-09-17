package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat

interface PlayerNotificationManager {
    fun enteredForeground(notification: Notification)
    fun notify(notificationId: Int, notification: Notification)
}

class PlayerNotificationManagerImpl(private val context: Context) : PlayerNotificationManager {
    private val notificationManager = NotificationManagerCompat.from(context)

    override fun enteredForeground(notification: Notification) {
        // Not used, just used for a testing point
    }

    override fun notify(notificationId: Int, notification: Notification) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(notificationId, notification)
        }
    }
}
