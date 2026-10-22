package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import au.com.shiftyjelly.pocketcasts.preferences.Settings

internal fun Context.newEpisodeNotificationsBlockedReason(): String? {
    val manager = NotificationManagerCompat.from(this)
    val channel = manager.getNotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_EPISODE.id)
    return when {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED -> "notification permission not granted"
        !manager.areNotificationsEnabled() -> "app notifications turned off"
        channel?.importance == NotificationManager.IMPORTANCE_NONE -> "new episodes channel turned off"
        else -> null
    }
}
