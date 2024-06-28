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
import androidx.core.graphics.drawable.toBitmap
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.extensions.isPlaying
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import coil.executeBlocking
import coil.imageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class NotificationDrawerImpl @Inject constructor(
    settings: Settings,
    private val notificationHelper: NotificationHelper,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    @ApplicationContext private val context: Context,
) : NotificationDrawer {

    private val playAction = NotificationCompat.Action(IR.drawable.notification_play, context.getString(LR.string.play), MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PLAY))
    private val pauseAction = NotificationCompat.Action(IR.drawable.notification_pause, context.getString(LR.string.pause), MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PAUSE))
    private val skipBackAction = NotificationCompat.Action(IR.drawable.notification_skipbackwards, context.getString(LR.string.player_notification_skip_back, settings.skipBackInSecs.value), MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_PREVIOUS))
    private val skipForwardAction = NotificationCompat.Action(IR.drawable.notification_skipforward, context.getString(LR.string.player_notification_skip_forward, settings.skipForwardInSecs.value), MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_NEXT))
    private val stopPendingIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_STOP)

    private var notificationData: NotificationData? = null

    private val imageRequestFactory = PocketCastsImageRequestFactory(
        context,
        isDarkTheme = true,
        size = 128,
        placeholderType = PlaceholderType.Small,
    )

    override fun buildPlayingNotification(
        sessionToken: MediaSessionCompat.Token,
        useEpisodeArtwork: Boolean,
    ): Notification {
        val controller = MediaControllerCompat(context, sessionToken)
        val description = controller.metadata.description
        val playbackState = controller.playbackState
        val data = getNotificationData(description.mediaId, useEpisodeArtwork)

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

    private fun loadArtwork(episode: BaseEpisode, useEpisodeArtwork: Boolean): Bitmap? {
        val request = imageRequestFactory.create(episode, useEpisodeArtwork)
        return context.imageLoader.executeBlocking(request).drawable?.toBitmap() ?: loadPlaceholderBitmap()
    }

    private fun loadPlaceholderBitmap(): Bitmap? {
        val request = imageRequestFactory.createForPodcast(podcastUuid = null)
        return context.imageLoader.executeBlocking(request).drawable?.toBitmap()
    }

    private fun getNotificationData(episodeUuid: String?, useEpisodeArtwork: Boolean): NotificationData {
        val episode: BaseEpisode? = if (episodeUuid == null) null else runBlocking { episodeManager.findEpisodeByUuid(episodeUuid) }
        val podcast: Podcast? = if (episode == null || episode !is PodcastEpisode) null else podcastManager.findPodcastByUuid(episode.podcastUuid)

        if (episodeUuid == null || episode == null) {
            return NotificationData()
        }

        val currentNotificationData = notificationData
        val currentEpisodeUuid = currentNotificationData?.episodeUuid
        val currentUseEpisodeArtwork = currentNotificationData?.useEpisodeArtwork
        if (currentEpisodeUuid != null && currentEpisodeUuid == episodeUuid && currentUseEpisodeArtwork == useEpisodeArtwork) {
            return currentNotificationData
        }

        val bitmap = loadArtwork(episode, useEpisodeArtwork)
        val podcastTitle = (if (episode is PodcastEpisode) podcast?.title else Podcast.userPodcast.title) ?: ""

        val data = NotificationData(
            episodeUuid = episodeUuid,
            title = podcastTitle,
            text = episode.title,
            icon = bitmap,
            useEpisodeArtwork = useEpisodeArtwork,
        )
        notificationData = data
        return data
    }

    data class NotificationData(
        val episodeUuid: String = "",
        val title: String = "",
        val text: String = "",
        val icon: Bitmap? = null,
        val useEpisodeArtwork: Boolean? = null,
    )
}
