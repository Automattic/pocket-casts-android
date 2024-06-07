package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.NotificationBroadcastReceiver.Companion.INTENT_EXTRA_NOTIFICATION_TAG
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class NotificationHelperImpl @Inject constructor(@ApplicationContext private val context: Context) : NotificationHelper {

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
        val playbackChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PLAYBACK.id, "Playback", NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(LR.string.notification_channel_description_playback)
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(playbackChannel)

        // set up download channel
        val downloadChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_DOWNLOAD.id, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(LR.string.notification_channel_description_download)
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(downloadChannel)

        // set up new episode channel
        val episodeChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_EPISODE.id, "New Episodes", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(LR.string.notification_channel_description_episode)
            setShowBadge(true)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(episodeChannel)

        val playbackErrorChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PLAYBACK_ERROR.id, "Playback Errors", NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(LR.string.notification_channel_description_playback_error)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(playbackErrorChannel)

        val podcastImportChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PODCAST.id, "Podcast Import", NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(LR.string.notification_channel_description_podcast_import)
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(podcastImportChannel)

        val signInErrorChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_SIGN_IN_ERROR.id, "Sign-in Error", NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(LR.string.notification_channel_description_sign_in_error)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(signInErrorChannel)

        val bookmarkChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_BOOKMARK.id, "Bookmark", NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(LR.string.notification_channel_description_bookmark)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(bookmarkChannel)

        val fixDownloadsChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_FIX_DOWNLOADS.id, "Fix Downloads", NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(LR.string.notification_channel_description_fix_downloads)
            setShowBadge(false)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(fixDownloadsChannel)

        val fixDownloadsCompleteChannel = NotificationChannel(Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_FIX_DOWNLOADS_COMPLETE.id, "Fix Downloads Complete", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(LR.string.notification_channel_description_fix_downloads_complete)
            setShowBadge(false)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        channelList.add(fixDownloadsCompleteChannel)

        notificationManager.createNotificationChannels(channelList)
    }

    override fun downloadChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_DOWNLOAD.id)
    }

    override fun playbackChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PLAYBACK.id)
    }

    override fun episodeNotificationChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_EPISODE.id)
    }

    override fun playbackErrorChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PLAYBACK_ERROR.id).setPriority(NotificationCompat.PRIORITY_MAX)
    }

    override fun podcastImportChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_PODCAST.id)
    }

    override fun bookmarkChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_BOOKMARK.id)
    }

    override fun downloadsFixChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_FIX_DOWNLOADS.id)
    }

    override fun downloadsFixCompleteChannelBuilder(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_FIX_DOWNLOADS_COMPLETE.id)
    }

    /**
     * Opens the system notification activity for the episode channel.
     */
    override fun openEpisodeNotificationSettings(activity: Activity?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity == null) return

        val intent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, activity.packageName)
        intent.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, Settings.NotificationChannel.NOTIFICATION_CHANNEL_ID_EPISODE.id)
        activity.startActivity(intent)
    }

    override fun removeNotification(intentExtras: Bundle?, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationTag = intentExtras?.getString(INTENT_EXTRA_NOTIFICATION_TAG, null)
        if (!notificationTag.isNullOrBlank()) {
            manager.cancel(notificationId)
        }
    }
}
