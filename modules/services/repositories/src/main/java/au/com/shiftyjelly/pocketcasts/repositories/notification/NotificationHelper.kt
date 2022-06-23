package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.app.Activity
import androidx.core.app.NotificationCompat

interface NotificationHelper {

    companion object {
        const val NOTIFICATION_ID_OPML = 21483646
        const val NOTIFICATION_ID_PLAYING = 21483647
        const val NOTIFICATION_ID_DOWNLOADING = 21483648
    }

    fun setupNotificationChannels()

    fun downloadChannelBuilder(): NotificationCompat.Builder
    fun playbackChannelBuilder(): NotificationCompat.Builder
    fun episodeNotificationChannelBuilder(): NotificationCompat.Builder
    fun playbackErrorChannelBuilder(): NotificationCompat.Builder
    fun podcastImportChannelBuilder(): NotificationCompat.Builder
    fun openEpisodeNotificationSettings(activity: Activity?)
    fun isShowing(notificationId: Int): Boolean
}
