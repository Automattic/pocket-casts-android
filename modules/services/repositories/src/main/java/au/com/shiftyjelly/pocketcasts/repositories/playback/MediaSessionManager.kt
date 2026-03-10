package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkHelper
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoConverter
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.PackageValidator
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.getLaunchActivityPendingIntent
import au.com.shiftyjelly.pocketcasts.utils.extensions.roundedSpeed
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class MediaSessionManager(
    val playbackManager: PlaybackManager,
    val podcastManager: PodcastManager,
    val episodeManager: EpisodeManager,
    val playlistManager: PlaylistManager,
    val settings: Settings,
    val context: Context,
    val episodeAnalytics: EpisodeAnalytics,
    val bookmarkManager: BookmarkManager,
    val browseTreeProvider: BrowseTreeProvider,
    applicationScope: CoroutineScope,
) {
    companion object {
        const val ACTION_NOT_SUPPORTED = "action_not_supported"

        // These manufacturers have issues when the skip to next/previous track are missing from the media session.
        private val MANUFACTURERS_TO_HIDE_CUSTOM_SKIP_BUTTONS = listOf("mercedes-benz")

        fun calculateSearchQueryOptions(query: String): List<String> {
            val options = mutableListOf<String>()
            options.add(query)
            val parts = query.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size > 1) {
                for (i in parts.size - 1 downTo 1) {
                    val lessParts = arrayOfNulls<String>(i)
                    System.arraycopy(parts, 0, lessParts, 0, i)
                    options.add(lessParts.joinToString(separator = " "))
                }
            }
            return options
        }
    }

    val disposables = CompositeDisposable()
    private val source = SourceView.MEDIA_BUTTON_BROADCAST_ACTION
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val commandMutex = Mutex()

    internal val actions = MediaSessionActions(
        playbackManager = playbackManager,
        podcastManager = podcastManager,
        episodeManager = episodeManager,
        playlistManager = playlistManager,
        settings = settings,
        episodeAnalytics = episodeAnalytics,
        scope = scope,
        source = source,
        onSearchFailed = { message ->
            media3Session?.sendError(SessionError(SessionError.ERROR_UNKNOWN, message))
        },
    )

    private var bookmarkHelper: BookmarkHelper

    private var media3Session: MediaLibraryService.MediaLibrarySession? = null
    private var forwardingPlayer: PocketCastsForwardingPlayer? = null
    private var media3Callback: Media3SessionCallback? = null
    private var media3LibraryCallback: Media3LibrarySessionCallback? = null
    private var placeholderPlayer: androidx.media3.common.Player? = null
    private var pendingPlayer: androidx.media3.common.Player? = null

    internal fun getMedia3Session(): MediaLibraryService.MediaLibrarySession? = media3Session

    init {
        bookmarkHelper = BookmarkHelper(
            playbackManager,
            bookmarkManager,
            settings,
        )
    }

    fun startObserving() {
        observeForMedia3Updates()
    }

    /**
     * Creates the [MediaLibraryService.MediaLibrarySession] with a placeholder ExoPlayer.
     * Called from [PlaybackService.onCreate] so that [onGetSession] can return a session
     * before playback starts. The placeholder is released on the first [installPlayer] call.
     */
    @OptIn(UnstableApi::class)
    @MainThread
    fun createSession(service: MediaLibraryService) {
        val placeholder = ExoPlayer.Builder(service).build()
        placeholderPlayer = placeholder

        forwardingPlayer = PocketCastsForwardingPlayer(
            wrappedPlayer = placeholder,
            onSkipForward = { scope.launch { commandMutex.withLock { playbackManager.skipForwardSuspend() } } },
            onSkipBack = { scope.launch { commandMutex.withLock { playbackManager.skipBackwardSuspend() } } },
            onStop = {
                if (playbackManager.player !is CastPlayer) {
                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Media3: stop → pause")
                    scope.launch { commandMutex.withLock { playbackManager.pauseSuspend(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION) } }
                }
            },
            onPlay = {
                scope.launch { commandMutex.withLock { playbackManager.playQueueSuspend(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION) } }
            },
            onPause = {
                scope.launch { commandMutex.withLock { playbackManager.pauseSuspend(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION) } }
            },
            onSeekTo = { positionMs ->
                scope.launch {
                    commandMutex.withLock {
                        playbackManager.seekToTimeMsSuspend(positionMs.toInt())
                        playbackManager.trackPlaybackSeek(positionMs.toInt(), source)
                    }
                }
            },
            playGuard = {
                if (Util.isAutomotive(context) && !settings.automotiveConnectedToMediaSession()) {
                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Auto start playback ignored just after automotive app restart.")
                    false
                } else {
                    true
                }
            },
        )

        media3Callback = Media3SessionCallback(
            playbackManager = playbackManager,
            episodeManager = episodeManager,
            settings = settings,
            actions = actions,
            bookmarkHelper = bookmarkHelper,
            scope = scope,
            contextProvider = { context },
            commandMutex = commandMutex,
        )
        media3LibraryCallback = Media3LibrarySessionCallback(
            sessionCallback = media3Callback!!,
            browseTreeProvider = browseTreeProvider,
            playbackManager = playbackManager,
            settings = settings,
            packageValidator = if (!BuildConfig.DEBUG) {
                PackageValidator(context, LR.xml.allowed_media_browser_callers)
            } else {
                null
            },
            scope = scope,
            contextProvider = { context },
        )

        media3Session = MediaLibraryService.MediaLibrarySession.Builder(service, forwardingPlayer!!, media3LibraryCallback!!)
            .setId("PocketCastsMedia3Session")
            .setExtras(
                Bundle().apply {
                    putBoolean("com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_QUEUE", true)
                },
            )
            .apply {
                if (!Util.isAutomotive(context)) {
                    setSessionActivity(context.getLaunchActivityPendingIntent())
                }
            }
            .build()
        Timber.i("Media3 session created")

        updateMedia3CustomLayout()
        pendingPlayer?.let { installPlayer(it) }
    }

    /**
     * Called from [PlaybackManager] when a [SimplePlayer] is created, providing the
     * underlying ExoPlayer reference so the Media3 session can wrap it.
     */
    @OptIn(UnstableApi::class)
    @MainThread
    fun installPlayer(exoPlayer: androidx.media3.common.Player) {
        val currentPlayer = forwardingPlayer
        if (currentPlayer == null) {
            Timber.i("installPlayer: session not ready, deferring")
            pendingPlayer = exoPlayer
            return
        }
        // Avoid redundant swaps when called with the same player (e.g., multiple play() calls)
        if (currentPlayer.wrappedPlayer === exoPlayer) return
        pendingPlayer = null
        val swapped = currentPlayer.swapPlayer(exoPlayer)
        forwardingPlayer = swapped
        media3Session?.player = swapped
        placeholderPlayer?.release()
        placeholderPlayer = null
        Timber.i("Media3 session player swapped")
    }

    fun startServiceIfNeeded(context: Context) {
        if (media3Session != null) return
        try {
            context.startService(Intent(context, PlaybackService::class.java))
        } catch (e: Exception) {
            Timber.e(e, "Failed to start PlaybackService")
        }
    }

    @OptIn(UnstableApi::class)
    private fun observeForMedia3Updates() {
        playbackManager.playbackStateRelay
            .observeOn(Schedulers.io())
            .switchMap { state ->
                if (state.isEmpty) {
                    io.reactivex.Observable.just(Optional.empty<BaseEpisode>() to state)
                } else {
                    episodeManager.findEpisodeByUuidRxFlowable(state.episodeUuid)
                        .distinctUntilChanged(BaseEpisode.isMediaSessionEqual)
                        .map { Optional.of(it) to state }
                        .onErrorReturn { Optional.empty<BaseEpisode>() to state }
                        .toObservable()
                }
            }
            .observeOn(Schedulers.io())
            .map { (episodeOpt, state) ->
                val episode = episodeOpt.get()
                val podcast = when (episode) {
                    is PodcastEpisode -> podcastManager.findPodcastByUuidBlocking(episode.podcastUuid)
                    else -> null
                }
                Triple(episode, podcast, state)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { (episode, podcast, state) ->
                    val player = forwardingPlayer ?: return@subscribeBy
                    episode ?: return@subscribeBy
                    val showArtwork = settings.showArtworkOnLockScreen.value
                    val artworkBitmap = if (showArtwork && !Util.isWearOs(context) && !Util.isAutomotive(context)) {
                        AutoConverter.getPodcastArtworkBitmap(
                            episode,
                            context,
                            settings.artworkConfiguration.value.useEpisodeArtwork,
                        )
                    } else {
                        null
                    }
                    player.updateMetadata(episode, podcast, showArtwork, artworkBitmap)
                    player.isTransientLoss = state.transientLoss
                    updateMedia3CustomLayout()
                },
                onError = { Timber.e(it, "Error observing Media3 updates") },
            )
            .addTo(disposables)

        playbackManager.playbackStateRelay
            .map { it.playbackSpeed }
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { updateMedia3CustomLayout() },
                onError = { Timber.e(it, "Error observing speed changes") },
            )
            .addTo(disposables)

        combine(
            settings.customMediaActionsVisibility.flow,
            settings.mediaControlItems.flow,
        ) { visibility, items -> visibility to items }
            .onEach { updateMedia3CustomLayout() }
            .catch { Timber.e(it) }
            .launchIn(scope)

        playbackManager.upNextQueue.changesObservable
            .observeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    media3Session?.notifyChildrenChanged(UP_NEXT_ROOT, 0, null)
                },
                onError = { Timber.e(it, "Error observing Up Next changes") },
            )
            .addTo(disposables)
    }

    @OptIn(UnstableApi::class)
    private fun updateMedia3CustomLayout() {
        val session = media3Session ?: return
        if (Util.isWearOs(context)) return

        val buttons = mutableListOf<CommandButton>()
        val currentEpisode = playbackManager.getCurrentEpisode()

        if (useCustomSkipButtons()) {
            buttons.add(
                CommandButton.Builder(CommandButton.ICON_SKIP_BACK)
                    .setSessionCommand(SessionCommand(APP_ACTION_SKIP_BACK, Bundle.EMPTY))
                    .setDisplayName("Skip back")
                    .setCustomIconResId(IR.drawable.media_skipback)
                    .build(),
            )
            buttons.add(
                CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD)
                    .setSessionCommand(SessionCommand(APP_ACTION_SKIP_FWD, Bundle.EMPTY))
                    .setDisplayName("Skip forward")
                    .setCustomIconResId(IR.drawable.media_skipforward)
                    .build(),
            )
        }

        val visibleCount = if (settings.customMediaActionsVisibility.value) MediaNotificationControls.MAX_VISIBLE_OPTIONS else 0
        settings.mediaControlItems.value.take(visibleCount).forEach { mediaControl ->
            when (mediaControl) {
                MediaNotificationControls.Archive -> buttons.add(
                    CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                        .setSessionCommand(SessionCommand(APP_ACTION_ARCHIVE, Bundle.EMPTY))
                        .setDisplayName("Archive")
                        .setCustomIconResId(IR.drawable.ic_archive)
                        .build(),
                )

                MediaNotificationControls.MarkAsPlayed -> buttons.add(
                    CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                        .setSessionCommand(SessionCommand(APP_ACTION_MARK_AS_PLAYED, Bundle.EMPTY))
                        .setDisplayName("Mark as played")
                        .setCustomIconResId(IR.drawable.auto_markasplayed)
                        .build(),
                )

                MediaNotificationControls.PlayNext -> buttons.add(
                    CommandButton.Builder(CommandButton.ICON_NEXT)
                        .setSessionCommand(SessionCommand(APP_ACTION_PLAY_NEXT, Bundle.EMPTY))
                        .setDisplayName("Play next")
                        .setCustomIconResId(com.google.android.gms.cast.framework.R.drawable.cast_ic_mini_controller_skip_next)
                        .build(),
                )

                MediaNotificationControls.PlaybackSpeed -> {
                    if (playbackManager.isAudioEffectsAvailable()) {
                        buttons.add(
                            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                                .setSessionCommand(SessionCommand(APP_ACTION_CHANGE_SPEED, Bundle.EMPTY))
                                .setDisplayName("Change speed")
                                .setCustomIconResId(speedToDrawable(playbackManager.getPlaybackSpeed()))
                                .build(),
                        )
                    }
                }

                MediaNotificationControls.Star -> {
                    if (currentEpisode is PodcastEpisode) {
                        if (currentEpisode.isStarred) {
                            buttons.add(
                                CommandButton.Builder(CommandButton.ICON_HEART_FILLED)
                                    .setSessionCommand(SessionCommand(APP_ACTION_UNSTAR, Bundle.EMPTY))
                                    .setDisplayName("Unstar")
                                    .setCustomIconResId(IR.drawable.auto_starred)
                                    .build(),
                            )
                        } else {
                            buttons.add(
                                CommandButton.Builder(CommandButton.ICON_HEART_UNFILLED)
                                    .setSessionCommand(SessionCommand(APP_ACTION_STAR, Bundle.EMPTY))
                                    .setDisplayName("Star")
                                    .setCustomIconResId(IR.drawable.auto_star)
                                    .build(),
                            )
                        }
                    }
                }
            }
        }

        session.setCustomLayout(buttons)
        session.setMediaButtonPreferences(buttons)
    }

    fun release() {
        disposables.clear()
        scope.cancel()
        media3Session?.release()
        media3Session = null
        placeholderPlayer?.release()
        placeholderPlayer = null
    }

    fun playFromSearchExternal(query: String) {
        actions.performPlayFromSearch(query)
    }

    @DrawableRes
    private fun speedToDrawable(speed: Double): Int {
        return when (speed.roundedSpeed()) {
            in 0.0..<0.55 -> IR.drawable.auto_0_5
            in 0.55..<0.65 -> IR.drawable.auto_0_6
            in 0.65..<0.75 -> IR.drawable.auto_0_7
            in 0.75..<0.85 -> IR.drawable.auto_0_8
            in 0.85..<0.95 -> IR.drawable.auto_0_9
            in 0.95..<1.05 -> IR.drawable.auto_1
            in 1.05..<1.15 -> IR.drawable.auto_1_1
            in 1.15..<1.25 -> IR.drawable.auto_1_2
            in 1.25..<1.35 -> IR.drawable.auto_1_3
            in 1.35..<1.45 -> IR.drawable.auto_1_4
            in 1.45..<1.55 -> IR.drawable.auto_1_5
            in 1.55..<1.65 -> IR.drawable.auto_1_6
            in 1.65..<1.75 -> IR.drawable.auto_1_7
            in 1.75..<1.85 -> IR.drawable.auto_1_8
            in 1.85..<1.95 -> IR.drawable.auto_1_9
            in 1.95..<2.05 -> IR.drawable.auto_2
            in 2.05..<2.15 -> IR.drawable.auto_2_1
            in 2.15..<2.25 -> IR.drawable.auto_2_2
            in 2.25..<2.35 -> IR.drawable.auto_2_3
            in 2.35..<2.45 -> IR.drawable.auto_2_4
            in 2.45..<2.55 -> IR.drawable.auto_2_5
            in 2.55..<2.65 -> IR.drawable.auto_2_6
            in 2.65..<2.75 -> IR.drawable.auto_2_7
            in 2.75..<2.85 -> IR.drawable.auto_2_8
            in 2.85..<2.95 -> IR.drawable.auto_2_9
            in 2.95..<3.05 -> IR.drawable.auto_3
            in 3.05..<3.15 -> IR.drawable.auto_3_1
            in 3.15..<3.25 -> IR.drawable.auto_3_2
            in 3.25..<3.35 -> IR.drawable.auto_3_3
            in 3.35..<3.45 -> IR.drawable.auto_3_4
            in 3.45..<3.55 -> IR.drawable.auto_3_5
            in 3.55..<3.65 -> IR.drawable.auto_3_6
            in 3.65..<3.75 -> IR.drawable.auto_3_7
            in 3.75..<3.85 -> IR.drawable.auto_3_8
            in 3.85..<3.95 -> IR.drawable.auto_3_9
            in 3.95..<4.05 -> IR.drawable.auto_4
            in 4.05..<4.15 -> IR.drawable.auto_4_1
            in 4.15..<4.25 -> IR.drawable.auto_4_2
            in 4.25..<4.35 -> IR.drawable.auto_4_3
            in 4.35..<4.45 -> IR.drawable.auto_4_4
            in 4.45..<4.55 -> IR.drawable.auto_4_5
            in 4.55..<4.65 -> IR.drawable.auto_4_6
            in 4.65..<4.75 -> IR.drawable.auto_4_7
            in 4.75..<4.85 -> IR.drawable.auto_4_8
            in 4.85..<4.95 -> IR.drawable.auto_4_9
            in 4.95..<5.05 -> IR.drawable.auto_5
            else -> IR.drawable.auto_1
        }
    }

    private fun useCustomSkipButtons(): Boolean {
        return !MANUFACTURERS_TO_HIDE_CUSTOM_SKIP_BUTTONS.contains(Build.MANUFACTURER.lowercase()) &&
            !settings.nextPreviousTrackSkipButtons.value
    }
}

internal const val APP_ACTION_STAR = "star"
internal const val APP_ACTION_UNSTAR = "unstar"
internal const val APP_ACTION_SKIP_BACK = "jumpBack"
internal const val APP_ACTION_SKIP_FWD = "jumpFwd"
internal const val APP_ACTION_MARK_AS_PLAYED = "markAsPlayed"
internal const val APP_ACTION_CHANGE_SPEED = "changeSpeed"
internal const val APP_ACTION_ARCHIVE = "archive"
internal const val APP_ACTION_PLAY_NEXT = "playNext"
