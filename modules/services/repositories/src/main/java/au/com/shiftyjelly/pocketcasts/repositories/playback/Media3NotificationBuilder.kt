package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PAUSE
import android.support.v4.media.session.PlaybackStateCompat.ACTION_PLAY
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_NEXT
import android.support.v4.media.session.PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
import android.support.v4.media.session.PlaybackStateCompat.ACTION_STOP
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import coil3.executeBlocking
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class Media3NotificationBuilder(
    private val context: Context,
    private val notificationHelper: NotificationHelper,
    settings: Settings,
) {
    private val skipBackAction = NotificationCompat.Action(
        IR.drawable.notification_skipbackwards,
        context.getString(LR.string.player_notification_skip_back, settings.skipBackInSecs.value),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_PREVIOUS),
    )
    private val skipForwardAction = NotificationCompat.Action(
        IR.drawable.notification_skipforward,
        context.getString(LR.string.player_notification_skip_forward, settings.skipForwardInSecs.value),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_SKIP_TO_NEXT),
    )
    private val playAction = NotificationCompat.Action(
        IR.drawable.notification_play,
        context.getString(LR.string.play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PLAY),
    )
    private val pauseAction = NotificationCompat.Action(
        IR.drawable.notification_pause,
        context.getString(LR.string.pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_PAUSE),
    )
    private val stopPendingIntent =
        MediaButtonReceiver.buildMediaButtonPendingIntent(context, ACTION_STOP)

    private var cachedMediaId: String? = null
    private var cachedArtwork: Bitmap? = null

    fun build(
        player: Player,
        compatToken: MediaSessionCompat.Token,
        sessionActivity: PendingIntent?,
    ): Notification? {
        val mediaItem = player.currentMediaItem ?: return null
        if (mediaItem.mediaId.isEmpty() || mediaItem == MediaItem.EMPTY) return null

        val metadata = player.mediaMetadata
        val isPlaying = player.isPlaying || player.playbackState == Player.STATE_BUFFERING

        val artwork = loadArtwork(mediaItem.mediaId, metadata.artworkUri)

        val builder = notificationHelper.playbackChannelBuilder()
        builder.addAction(skipBackAction)
        builder.addAction(if (isPlaying) pauseAction else playAction)
        builder.addAction(skipForwardAction)

        val mediaStyle = MediaStyle()
            .setCancelButtonIntent(stopPendingIntent)
            .setMediaSession(compatToken)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)

        return builder
            .setContentIntent(sessionActivity)
            .setContentTitle(metadata.artist ?: "")
            .setContentText(metadata.title ?: "")
            .setDeleteIntent(stopPendingIntent)
            .setLargeIcon(artwork)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSmallIcon(IR.drawable.notification)
            .setStyle(mediaStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun loadArtwork(mediaId: String, artworkUri: Uri?): Bitmap? {
        if (mediaId == cachedMediaId && cachedArtwork != null) return cachedArtwork
        artworkUri ?: return null

        val request = ImageRequest.Builder(context)
            .data(artworkUri.toString())
            .size(128)
            .build()

        val bitmap = when (val result = context.imageLoader.executeBlocking(request)) {
            is SuccessResult -> result.image.toBitmap()
            is ErrorResult -> null
        }
        cachedMediaId = mediaId
        cachedArtwork = bitmap
        return bitmap
    }
}
