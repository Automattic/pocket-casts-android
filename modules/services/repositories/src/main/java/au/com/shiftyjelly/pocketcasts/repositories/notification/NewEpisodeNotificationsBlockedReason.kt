package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import au.com.shiftyjelly.pocketcasts.preferences.Settings

internal fun Context.newEpisodeNotificationsBlockedReason(): String? {
    val manager = NotificationManagerCompat.from(this)
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED -> "notification permission not granted"

        !manager.areNotificationsEnabled() -> "app notifications turned off"

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_EPISODE.id)?.importance == NotificationManager.IMPORTANCE_NONE -> "new episodes channel turned off"

        else -> null
    }
}
