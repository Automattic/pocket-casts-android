package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.core.content.IntentCompat
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Rating
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkHelper
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getArtworkUrl
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoMediaId
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
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
    private val podcastManager: PodcastManager,
    private val settings: Settings,
    private val actions: MediaSessionActions,
    private val bookmarkHelper: BookmarkHelper,
    private val scopeProvider: () -> CoroutineScope,
    private val contextProvider: () -> Context,
    private val source: SourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION,
    internal val commandMutex: Mutex = Mutex(),
) : MediaSession.Callback {

    private val scope: CoroutineScope get() = scopeProvider()

    private val mediaEventQueue = MediaEventQueue(scopeProvider = scopeProvider)

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

        return MediaSession.ConnectionResult.accept(sessionCommands, TRANSPORT_PLAYER_COMMANDS)
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        return when (customCommand.customAction) {
            APP_ACTION_SKIP_BACK -> launchCommandFuture("Skip back") { playbackManager.skipBackwardSuspend() }
            APP_ACTION_SKIP_FWD -> launchCommandFuture("Skip forward") { playbackManager.skipForwardSuspend() }
            APP_ACTION_MARK_AS_PLAYED -> launchCommandFuture("Mark as played") { actions.markAsPlayedSuspend() }
            APP_ACTION_STAR -> launchCommandFuture("Star") { actions.starEpisodeSuspend() }
            APP_ACTION_UNSTAR -> launchCommandFuture("Unstar") { actions.unstarEpisodeSuspend() }
            APP_ACTION_CHANGE_SPEED -> launchCommandFuture("Change speed") { actions.changePlaybackSpeedSuspend() }
            APP_ACTION_ARCHIVE -> launchCommandFuture("Archive") { actions.archiveSuspend() }
            APP_ACTION_PLAY_NEXT -> launchCommandFuture("Play next") { playbackManager.playNextInQueue() }
            else -> Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
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
            launchCommand("Play from search") { actions.performPlayFromSearchSuspend(searchQuery) }
            return Futures.immediateFuture(emptyList())
        }

        val mediaId = item.mediaId
        if (mediaId.isEmpty()) {
            return Futures.immediateFuture(emptyList())
        }

        val future = SettableFuture.create<List<MediaItem>>()
        scope.launch {
            commandMutex.withLock {
                try {
                    val autoMediaId = AutoMediaId.fromMediaId(mediaId)
                    val episodeId = autoMediaId.episodeId
                    val episode = episodeManager.findEpisodeByUuid(episodeId)
                    if (episode == null) {
                        future.set(emptyList())
                        return@withLock
                    }

                    val podcast = (episode as? PodcastEpisode)?.let {
                        podcastManager.findPodcastByUuid(it.podcastUuid)
                    }

                    val resolvedItem = buildEpisodeMediaItem(episode, podcast, mediaId)
                    future.set(listOf(resolvedItem))

                    playbackManager.playNowSuspend(episode = episode, sourceView = source)
                } catch (e: Exception) {
                    Timber.e(e, "Play from media ID failed")
                    future.set(emptyList())
                }
            }
        }
        return future
    }

    override fun onSetRating(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        rating: Rating,
    ): ListenableFuture<SessionResult> {
        if (rating is HeartRating) {
            return launchCommandFuture("Set rating") {
                if (rating.isHeart) {
                    actions.starEpisodeSuspend()
                } else {
                    actions.unstarEpisodeSuspend()
                }
            }
        }
        return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
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

        // Dedicated pause key has explicit semantics — handle it directly.
        // KEYCODE_MEDIA_PLAY is routed through the multi-tap system because some
        // Bluetooth headphones (e.g. Pixel Buds) send it spuriously after multi-tap
        // sequences, and the MediaEventQueue suppresses those duplicates.
        when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                scope.launch { playbackManager.pauseSuspend(sourceView = source) }
                return true
            }

            // PiP skip buttons use dedicated key codes that always skip forward/back,
            // bypassing headphone control settings.
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

    private fun launchCommand(tag: String, block: suspend () -> Unit) {
        scope.launch {
            commandMutex.withLock {
                try {
                    block()
                } catch (e: Exception) {
                    Timber.e(e, "$tag failed")
                }
            }
        }
    }

    /**
     * Like [launchCommand] but returns a [ListenableFuture] that resolves with the
     * actual outcome — [SessionResult.RESULT_SUCCESS] on completion or
     * [SessionError.ERROR_UNKNOWN] on failure.
     */
    private fun launchCommandFuture(tag: String, block: suspend () -> Unit): ListenableFuture<SessionResult> {
        val future = SettableFuture.create<SessionResult>()
        scope.launch {
            commandMutex.withLock {
                try {
                    block()
                    future.set(SessionResult(SessionResult.RESULT_SUCCESS))
                } catch (e: Exception) {
                    Timber.e(e, "$tag failed")
                    future.set(SessionResult(SessionError.ERROR_UNKNOWN))
                }
            }
        }
        return future
    }

    private fun handleMediaButtonAction(action: HeadphoneAction) {
        when (action) {
            HeadphoneAction.ADD_BOOKMARK -> {
                scope.launch(Dispatchers.Main) {
                    try {
                        val isAutoConnected = Util.isAndroidAutoConnectedFlow(contextProvider()).first()
                        bookmarkHelper.handleAddBookmarkAction(contextProvider(), isAutoConnected)
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

/**
 * Player commands granted to all connected controllers (known and unknown).
 * Covers basic transport controls: play/pause, stop, seek, and metadata retrieval.
 */
@OptIn(UnstableApi::class)
@Suppress("UnsafeOptInUsageError")
internal val TRANSPORT_PLAYER_COMMANDS: Player.Commands = Player.Commands.Builder()
    .addAll(
        Player.COMMAND_PLAY_PAUSE,
        Player.COMMAND_SET_MEDIA_ITEM,
        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
        Player.COMMAND_STOP,
        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
        Player.COMMAND_GET_METADATA,
    )
    .build()

internal fun resolveArtworkUri(episode: BaseEpisode, podcast: Podcast?): Uri? {
    return when (episode) {
        is PodcastEpisode -> {
            val url = episode.imageUrl?.takeIf { it.isNotBlank() }
                ?: podcast?.getArtworkUrl(480)?.takeIf { it.isNotBlank() }
            url?.let(Uri::parse)
        }

        is UserEpisode -> {
            episode.artworkUrl?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        }
    }
}

internal fun buildEpisodeMediaItem(
    episode: BaseEpisode,
    podcast: Podcast?,
    mediaId: String = episode.uuid,
): MediaItem {
    val artworkUri = resolveArtworkUri(episode, podcast)
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(episode.title)
                .setArtist(episode.displaySubtitle(podcast))
                .setArtworkUri(artworkUri)
                .setDurationMs(episode.durationMs.toLong())
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE)
                .build(),
        )
        .build()
}
