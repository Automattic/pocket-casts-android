package au.com.shiftyjelly.pocketcasts.repositories.notification

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
import android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import au.com.shiftyjelly.pocketcasts.models.db.helper.UserEpisodePodcastSubstitute
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.extensions.isPlaying
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class NotificationDrawerImpl @Inject constructor(
    settings: Settings,
    private val notificationHelper: NotificationHelper,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    @ApplicationContext private val context: Context
) : NotificationDrawer {

    private val playAction = NotificationCompat.Action(IR.drawable.notification_play, context.getString(LR.string.play), MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PLAY))
    private val pauseAction = NotificationCompat.Action(IR.drawable.notification_pause, context.getString(LR.string.pause), MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PAUSE))
    private val skipBackAction = NotificationCompat.Action(IR.drawable.notification_skipbackwards, context.getString(LR.string.player_notification_skip_back, settings.skipBackInSecs.value), MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_PREVIOUS))
    private val skipForwardAction = NotificationCompat.Action(IR.drawable.notification_skipforward, context.getString(LR.string.player_notification_skip_forward, settings.skipForwardInSecs.value), MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_NEXT))
    private val stopPendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_STOP)

    private var notificationData: NotificationData? = null

    override fun buildPlayingNotification(sessionToken: MediaSessionCompat.Token): Notification {
        val controller = MediaControllerCompat(context, sessionToken)
        val description = controller.metadata.description
        val playbackState = controller.playbackState
        val data = getNotificationData(description.mediaId)

        val builder = notificationHelper.playbackChannelBuilder()

        builder.addAction(skipBackAction)
        builder.addAction(if (playbackState.isPlaying) pauseAction else playAction)
        builder.addAction(skipForwardAction)

        val mediaStyle = MediaStyle()
            .setCancelButtonIntent(stopPendingIntent)
            .setMediaSession(sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)

        return builder.setContentIntent(controller.sessionActivity)
            .setContentText(data.text)
            .setContentTitle(data.title)
            .setDeleteIntent(stopPendingIntent)
            .setLargeIcon(data.icon)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSmallIcon(IR.drawable.notification)
            .setStyle(mediaStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun loadArtwork(podcast: Podcast): Bitmap? {
        val imageLoader = PodcastImageLoader(context = context, isDarkTheme = true, transformations = emptyList()).smallPlaceholder()
        val imageSize = (128 * context.resources.displayMetrics.density).toInt()
        return imageLoader.getBitmap(podcast, imageSize)
    }

    private fun loadUserEpisodeArtwork(episode: UserEpisode): Bitmap? {
        val imageLoader = PodcastImageLoader(context = context, isDarkTheme = true, transformations = emptyList())
        val imageSize = (128 * context.resources.displayMetrics.density).toInt()
        return imageLoader.getBitmap(episode, imageSize)
    }

    private fun getNotificationData(episodeUuid: String?): NotificationData {
        val episode: BaseEpisode? = if (episodeUuid == null) null else runBlocking { episodeManager.findEpisodeByUuid(episodeUuid) }
        val podcast: Podcast? = if (episode == null || episode !is PodcastEpisode) null else podcastManager.findPodcastByUuid(episode.podcastUuid)

        if (episodeUuid == null || episode == null) {
            return NotificationData()
        }

        val currentNotificationData = notificationData
        val currentEpisodeUuid = currentNotificationData?.episodeUuid
        if (currentEpisodeUuid != null && currentEpisodeUuid == episodeUuid) {
            return currentNotificationData
        }

        val bitmap = if (podcast != null) loadArtwork(podcast) else if (episode is UserEpisode) loadUserEpisodeArtwork(episode) else null
        val podcastTitle = (if (episode is PodcastEpisode) podcast?.title else UserEpisodePodcastSubstitute.substituteTitle) ?: ""

        val data = NotificationData(
            episodeUuid = episodeUuid,
            title = podcastTitle,
            text = episode.title,
            icon = bitmap
        )
        notificationData = data
        return data
    }

    data class NotificationData(
        var episodeUuid: String = "",
        var title: String = "",
        var text: String = "",
        var icon: Bitmap? = null
    )
}
