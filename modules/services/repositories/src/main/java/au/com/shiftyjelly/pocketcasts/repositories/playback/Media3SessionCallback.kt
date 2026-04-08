package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
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
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoMediaId
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val contextProvider: () -> Context,
    private val source: SourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION,
    private val commandMutex: Mutex = Mutex(),
) : MediaSession.Callback {

    private val mediaEventQueue = MediaEventQueue(scope = scope)

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        val sessionCommands = SessionCommands.Builder()
            // Library browsing commands required for AAOS / Android Auto browse tree
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_SUBSCRIBE))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_UNSUBSCRIBE))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_CHILDREN))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_ITEM))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_SEARCH))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT))
            // Custom app commands
            .add(SessionCommand(APP_ACTION_SKIP_BACK, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_SKIP_FWD, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_MARK_AS_PLAYED, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_STAR, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_UNSTAR, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_CHANGE_SPEED, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_ARCHIVE, Bundle.EMPTY))
            .add(SessionCommand(APP_ACTION_PLAY_NEXT, Bundle.EMPTY))
            .add(SessionCommand(SessionCommand.COMMAND_CODE_SESSION_SET_RATING))
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
                Player.COMMAND_SET_MEDIA_ITEM,
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
        val future = SettableFuture.create<SessionResult>()
        scope.launch {
            try {
                commandMutex.withLock {
                    when (customCommand.customAction) {
                        APP_ACTION_SKIP_BACK -> playbackManager.skipBackwardSuspend()

                        APP_ACTION_SKIP_FWD -> playbackManager.skipForwardSuspend()

                        APP_ACTION_MARK_AS_PLAYED -> actions.markAsPlayed()

                        APP_ACTION_STAR -> actions.starEpisode()

                        APP_ACTION_UNSTAR -> actions.unstarEpisode()

                        APP_ACTION_CHANGE_SPEED -> actions.changePlaybackSpeed()

                        APP_ACTION_ARCHIVE -> actions.archive()

                        APP_ACTION_PLAY_NEXT -> playbackManager.playNextInQueue()

                        else -> {
                            Timber.w("Unknown custom command: ${customCommand.customAction}")
                            future.set(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                            return@launch
                        }
                    }
                }
                future.set(SessionResult(SessionResult.RESULT_SUCCESS))
            } catch (e: Exception) {
                Timber.e(e, "Custom command failed: ${customCommand.customAction}")
                future.set(SessionResult(SessionError.ERROR_UNKNOWN))
            }
        }
        return future
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
                commandMutex.withLock {
                    try {
                        val autoMediaId = AutoMediaId.fromMediaId(mediaId)
                        val episodeId = autoMediaId.episodeId
                        episodeManager.findEpisodeByUuid(episodeId)?.let { episode ->
                            playbackManager.playNowSuspend(episode = episode, sourceView = source)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Play from media ID failed: $mediaId")
                    }
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
        val future = SettableFuture.create<SessionResult>()
        scope.launch {
            try {
                commandMutex.withLock {
                    if (rating is HeartRating) {
                        if (rating.isHeart) {
                            actions.starEpisode()
                        } else {
                            actions.unstarEpisode()
                        }
                        future.set(SessionResult(SessionResult.RESULT_SUCCESS))
                    } else {
                        future.set(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Set rating failed")
                future.set(SessionResult(SessionError.ERROR_UNKNOWN))
            }
        }
        return future
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

        // PiP skip buttons use dedicated key codes that always skip forward/back,
        // bypassing headphone control settings.
        when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> {
                scope.launch {
                    try {
                        playbackManager.skipForwardSuspend(
                            sourceView = source,
                            jumpAmountSeconds = settings.skipForwardInSecs.value,
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "PiP skip forward failed")
                    }
                }
                return true
            }

            KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> {
                scope.launch {
                    try {
                        playbackManager.skipBackwardSuspend(
                            sourceView = source,
                            jumpAmountSeconds = settings.skipBackInSecs.value,
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "PiP skip backward failed")
                    }
                }
                return true
            }
        }

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
                try {
                    val outputEvent = mediaEventQueue.consumeEvent(inputEvent)
                    when (outputEvent) {
                        MediaEvent.SingleTap -> handleMediaButtonSingleTap()
                        MediaEvent.DoubleTap -> handleMediaButtonDoubleTap()
                        MediaEvent.TripleTap -> handleMediaButtonTripleTap()
                        null -> Unit
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Media button event handling failed")
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
                val context = contextProvider()
                scope.launch(Dispatchers.Main) {
                    try {
                        val isAutoConnected = Util.isAndroidAutoConnectedFlow(context).first()
                        bookmarkHelper.handleAddBookmarkAction(context, isAutoConnected)
                    } catch (e: Exception) {
                        Timber.e(e, "Add bookmark failed")
                    }
                }
            }

            HeadphoneAction.SKIP_FORWARD -> {
                scope.launch {
                    try {
                        playbackManager.skipForwardSuspend(
                            sourceView = source,
                            jumpAmountSeconds = settings.skipForwardInSecs.value,
                        )
                        if (!playbackManager.isPlaying()) {
                            playbackManager.playQueueSuspend(source)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Skip forward failed")
                    }
                }
            }

            HeadphoneAction.SKIP_BACK -> {
                scope.launch {
                    try {
                        playbackManager.skipBackwardSuspend(
                            sourceView = source,
                            jumpAmountSeconds = settings.skipBackInSecs.value,
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Skip back failed")
                    }
                }
            }

            HeadphoneAction.NEXT_CHAPTER,
            HeadphoneAction.PREVIOUS_CHAPTER,
            -> Timber.e(MediaSessionManager.ACTION_NOT_SUPPORTED)
        }
    }
}
