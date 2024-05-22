package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.app.Activity
import android.os.Bundle
import androidx.core.app.NotificationCompat

interface NotificationHelper {

    fun setupNotificationChannels()

    fun downloadChannelBuilder(): NotificationCompat.Builder
    fun playbackChannelBuilder(): NotificationCompat.Builder
    fun episodeNotificationChannelBuilder(): NotificationCompat.Builder
    fun playbackErrorChannelBuilder(): NotificationCompat.Builder
    fun podcastImportChannelBuilder(): NotificationCompat.Builder
    fun bookmarkChannelBuilder(): NotificationCompat.Builder
    fun downloadsFixChannelBuilder(): NotificationCompat.Builder
    fun downloadsFixCompleteChannelBuilder(): NotificationCompat.Builder
    fun openEpisodeNotificationSettings(activity: Activity?)
    fun isShowing(notificationId: Int): Boolean
    fun removeNotification(intentExtras: Bundle?, notificationId: Int)
}
