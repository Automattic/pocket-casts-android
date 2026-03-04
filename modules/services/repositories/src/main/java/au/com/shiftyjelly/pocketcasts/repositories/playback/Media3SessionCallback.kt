package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.content.IntentCompat
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Rating
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoMediaId
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Media3 [MediaSession.Callback] that mirrors the behaviour of the legacy
 * [MediaSessionManager.MediaSessionCallback].
 *
 * Play/pause/seek/stop are handled automatically by the [PocketCastsForwardingPlayer] —
 * this callback only needs to handle custom commands, media-button events,
 * `playFromMediaId` / `playFromSearch`, and rating changes.
 */
@OptIn(UnstableApi::class)
internal class Media3SessionCallback(
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val settings: Settings,
    private val actions: MediaSessionActions,
    private val bookmarkHelper: BookmarkHelper,
    private val scope: CoroutineScope,
    private val source: SourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION,
) : MediaSession.Callback {

    private val mediaEventQueue = MediaEventQueue(scope = scope)

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val sessionCommands = SessionCommands.Builder()
            .add(SessionCommand(APP_ACTION_SKIP_BACK, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_SKIP_FWD, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_MARK_AS_PLAYED, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_STAR, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_UNSTAR, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_CHANGE_SPEED, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_ARCHIVE, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_PLAY_NEXT, Bundle.EMPTY))
            .build()

        val playerCommands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_SEEK_BACK,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_STOP,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA,
            )
            .build()

        return MediaSession.ConnectionResult.accept(sessionCommands, playerCommands)
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        when (customCommand.customAction) {
            APP_ACTION_SKIP_BACK -> scope.launch { playbackManager.skipBackwardSuspend() }
            APP_ACTION_SKIP_FWD -> scope.launch { playbackManager.skipForwardSuspend() }
            APP_ACTION_MARK_AS_PLAYED -> actions.markAsPlayed()
            APP_ACTION_STAR -> actions.starEpisode()
            APP_ACTION_UNSTAR -> actions.unstarEpisode()
            APP_ACTION_CHANGE_SPEED -> actions.changePlaybackSpeed()
            APP_ACTION_ARCHIVE -> actions.archive()
            APP_ACTION_PLAY_NEXT -> scope.launch { playbackManager.playNextInQueue() }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
    ): ListenableFuture<List<MediaItem>> {
        val item = mediaItems.firstOrNull()
            ?: return Futures.immediateFuture(emptyList())

        val searchQuery = item.requestMetadata.searchQuery
        if (!searchQuery.isNullOrEmpty()) {
            actions.performPlayFromSearch(searchQuery)
            return Futures.immediateFuture(emptyList())
        }

        val mediaId = item.mediaId
        if (mediaId.isNotEmpty()) {
            scope.launch {
                val autoMediaId = AutoMediaId.fromMediaId(mediaId)
                val episodeId = autoMediaId.episodeId
                episodeManager.findEpisodeByUuid(episodeId)?.let { episode ->
                    playbackManager.playNowSuspend(episode = episode, sourceView = source)
                }
            }
        }

        return Futures.immediateFuture(emptyList())
    }

    override fun onSetRating(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        rating: Rating,
    ): ListenableFuture<SessionResult> {
        if (rating is HeartRating) {
            if (rating.isHeart) {
                actions.starEpisode()
            } else {
                actions.unstarEpisode()
            }
        }
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
    }

    override fun onMediaButtonEvent(
        session: MediaSession,
        controllerInfo: MediaSession.ControllerInfo,
        intent: Intent,
    ): Boolean {
        if (Intent.ACTION_MEDIA_BUTTON != intent.action) {
            return false
        }

        val keyEvent = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            ?: return false

        if (keyEvent.action != KeyEvent.ACTION_DOWN) {
            return true
        }

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Media3 media button event: keyCode=${keyEvent.keyCode}")

        val inputEvent = when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK,
            -> MediaEvent.SingleTap

            KeyEvent.KEYCODE_MEDIA_NEXT -> MediaEvent.DoubleTap

            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaEvent.TripleTap

            else -> null
        }

        if (inputEvent != null) {
            scope.launch {
                val outputEvent = mediaEventQueue.consumeEvent(inputEvent)
                when (outputEvent) {
                    MediaEvent.SingleTap -> handleMediaButtonSingleTap()
                    MediaEvent.DoubleTap -> handleMediaButtonDoubleTap()
                    MediaEvent.TripleTap -> handleMediaButtonTripleTap()
                    null -> Unit
                }
            }
            return true
        }

        return false
    }

    private fun handleMediaButtonSingleTap() {
        playbackManager.playPause(sourceView = source)
    }

    private fun handleMediaButtonDoubleTap() {
        handleMediaButtonAction(settings.headphoneControlsNextAction.value)
    }

    private fun handleMediaButtonTripleTap() {
        handleMediaButtonAction(settings.headphoneControlsPreviousAction.value)
    }

    private fun handleMediaButtonAction(action: HeadphoneAction) {
        when (action) {
            HeadphoneAction.ADD_BOOKMARK -> {
                // Bookmark handling is done via the BookmarkHelper in the legacy callback.
                // For Media3 we keep the same pattern.
                Timber.d("Media3: bookmark action not yet wired")
            }

            HeadphoneAction.SKIP_FORWARD -> {
                scope.launch {
                    playbackManager.skipForwardSuspend(sourceView = source)
                    if (!playbackManager.isPlaying()) {
                        playbackManager.playQueueSuspend(source)
                    }
                }
            }

            HeadphoneAction.SKIP_BACK -> {
                scope.launch { playbackManager.skipBackwardSuspend(sourceView = source) }
            }

            HeadphoneAction.NEXT_CHAPTER,
            HeadphoneAction.PREVIOUS_CHAPTER,
            -> Timber.e(MediaSessionManager.ACTION_NOT_SUPPORTED)
        }
    }
}
