package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationHelperImpl @Inject constructor(@ApplicationContext private val context: Context) : NotificationHelper {

    companion object {
        private const val PLAYBACK_CHANNEL_ID = "playback"
        private const val DOWNLOAD_CHANNEL_ID = "download"
        private const val EPISODE_NOTIFICATION_CHANNEL_ID = "episode"
        private const val PLAYBACK_ERROR_CHANNEL_ID = "playbackError"
        private const val PODCAST_IMPORT_CHANNEL_ID = "podcastImport"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    override fun isShowing(notificationId: Int): Boolean {
        return notificationManager?.activeNotifications?.firstOrNull { it.id == notificationId } != null
    }

    override fun setupNotificationChannels() {
        // we only need to create notification channels in Android O and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelList = ArrayList<NotificationChannel>()
        // set up playback channel
        val playbackChannel = NotificationChannel(PLAYBACK_CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows while Pocket Casts is playing audio"
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(playbackChannel)

        // set up download channel
        val downloadChannel = NotificationChannel(DOWNLOAD_CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows while Pocket Casts is downloading episodes"
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(downloadChannel)

        // set up new episode channel
        val episodeChannel = NotificationChannel(EPISODE_NOTIFICATION_CHANNEL_ID, "New Episodes", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Shows when a new episode comes out"
            setShowBadge(true)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(episodeChannel)

        val playbackErrorChannel = NotificationChannel(PLAYBACK_ERROR_CHANNEL_ID, "Playback Errors", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Errors during playback"
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(playbackErrorChannel)

        val podcastImportChannel = NotificationChannel(PODCAST_IMPORT_CHANNEL_ID, "Podcast Import", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Import podcast collections"
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(podcastImportChannel)

        notificationManager.createNotificationChannels(channelList)
    }

    override fun downloadChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
    }

    override fun playbackChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, PLAYBACK_CHANNEL_ID)
    }

    override fun episodeNotificationChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, EPISODE_NOTIFICATION_CHANNEL_ID)
    }

    override fun playbackErrorChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, PLAYBACK_ERROR_CHANNEL_ID).setPriority(NotificationCompat.PRIORITY_MAX)
    }

    override fun podcastImportChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, PODCAST_IMPORT_CHANNEL_ID)
    }

    /**
     * Opens the system notification activity for the episode channel.
     */
    override fun openEpisodeNotificationSettings(activity: Activity?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity == null) return

        val intent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, activity.packageName)
        intent.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, EPISODE_NOTIFICATION_CHANNEL_ID)
        activity.startActivity(intent)
    }
}
