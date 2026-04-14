package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.RatingCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.core.content.IntentCompat
import androidx.media.utils.MediaConstants.PLAYBACK_STATE_EXTRAS_KEY_MEDIA_ID
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.repositories.BuildConfig
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
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.eventhorizon.EventHorizon
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asObservable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    val eventHorizon: EventHorizon,
    val bookmarkManager: BookmarkManager,
    val browseTreeProvider: BrowseTreeProvider,
    private val applicationScope: CoroutineScope,
) {
    companion object {
        const val EXTRA_TRANSIENT = "pocketcasts_transient_loss"
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

    // Evaluated lazily on first access — must not be read before FeatureFlag.initialize().
    // In practice the first access is in startObserving(), which runs after FeatureFlag init.
    // Toggling requires a process restart; swapping at runtime is not supported.
    // On automotive, always use Media3: AAOS never uses app-managed notifications,
    // so the legacy compat session is unnecessary and having a single service avoids
    // the race condition where AAOS discovers the wrong service before the toggle runs.
    private val useMedia3Session by lazy {
        FeatureFlag.isEnabled(Feature.MEDIA3_SESSION) || Util.isAutomotive(context)
    }

    val mediaSession: MediaSessionCompat? by lazy {
        if (!useMedia3Session) {
            MediaSessionCompat(context, "PocketCastsMediaSession").also { session ->
                if (!Util.isAutomotive(context)) {
                    session.setSessionActivity(context.getLaunchActivityPendingIntent())
                }
                session.setRatingType(RatingCompat.RATING_HEART)
                session.setExtras(
                    Bundle().apply {
                        putBoolean("com.google.android.gms.car.media.ALWAYS_RESERVE_SPACE_FOR.ACTION_QUEUE", true)
                    },
                )
            }
        } else {
            null
        }
    }

    val disposables = CompositeDisposable()
    private val source = SourceView.MEDIA_BUTTON_BROADCAST_ACTION

    @Volatile
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val commandMutex = Mutex()

    @OptIn(UnstableApi::class)
    internal val actions = MediaSessionActions(
        playbackManager = playbackManager,
        podcastManager = podcastManager,
        episodeManager = episodeManager,
        playlistManager = playlistManager,
        settings = settings,
        eventHorizon = eventHorizon,
        scopeProvider = { scope },
        source = source,
        onSearchFailed = { message -> sendSearchError(message) },
    )

    @OptIn(UnstableApi::class)
    private fun sendSearchError(message: String) {
        media3Session?.sendError(SessionError(SessionError.ERROR_UNKNOWN, message))
    }

    private var bookmarkHelper: BookmarkHelper

    @Volatile
    private var media3Session: MediaLibraryService.MediaLibrarySession? = null

    @Volatile
    private var media3Service: MediaLibraryService? = null

    @Volatile
    private var forwardingPlayer: PocketCastsForwardingPlayer? = null

    @Volatile
    internal var media3Callback: Media3SessionCallback? = null

    @Volatile
    private var media3LibraryCallback: Media3LibrarySessionCallback? = null

    @Volatile
    private var placeholderPlayer: androidx.media3.common.Player? = null

    @Volatile
    private var pendingPlayer: androidx.media3.common.Player? = null

    @Volatile
    private var castStatePlayer: CastStatePlayer? = null

    internal fun getMedia3Session(): MediaLibraryService.MediaLibrarySession? = media3Session

    private val commandQueue: MutableSharedFlow<QueuedCommand> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 10,
    )

    init {
        bookmarkHelper = BookmarkHelper(
            playbackManager,
            bookmarkManager,
            settings,
        )
    }

    fun startObserving() {
        if (!useMedia3Session) {
            applicationScope.launch(Dispatchers.Main) {
                mediaSession!!.setCallback(
                    MediaSessionCallback(
                        playbackManager,
                        episodeManager,
                        enqueueCommand = { tag, command ->
                            val added = commandQueue.tryEmit(Pair(tag, command))
                            if (added) {
                                Timber.i("Added command to queue: $tag")
                            } else {
                                LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Failed to add command to queue: $tag")
                            }
                        },
                    ),
                )
            }

            applicationScope.launch {
                commandQueue.collect { (tag, command) ->
                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Executing queued command: $tag")
                    command()
                }
            }
        }

        if (useMedia3Session) {
            observeForMedia3Updates()
        } else {
            connect()
            observePlaybackState()
            observeCustomMediaActionsVisibility()
            observeMediaNotificationControls()

            val upNextQueueChanges = playbackManager.upNextQueue.getChangesFlowWithLiveCurrentEpisode(episodeManager, podcastManager)
                .distinctUntilChanged { stateOne, stateTwo ->
                    UpNextQueue.State.isEqualWithEpisodeCompare(stateOne, stateTwo) { episodeOne, episodeTwo ->
                        episodeOne.uuid == episodeTwo.uuid &&
                            episodeOne.duration == episodeTwo.duration &&
                            episodeOne.isStarred == episodeTwo.isStarred
                    }
                }

            combine(upNextQueueChanges, settings.artworkConfiguration.flow) { queueState, artworkConfiguration -> queueState to artworkConfiguration }
                .onEach { (queueState, artworkConfiguration) -> updateUpNext(queueState, artworkConfiguration.useEpisodeArtwork) }
                .catch { Timber.e(it) }
                .launchIn(this.scope)
        }
    }

    /**
     * Creates the [MediaLibraryService.MediaLibrarySession] with a [SeedStatePlayer] placeholder.
     * Called from [PlaybackService.onCreate] so that [onGetSession] can return a session
     * before playback starts. The placeholder is released on the first [installPlayer] call.
     */
    @OptIn(UnstableApi::class)
    @MainThread
    fun createSession(service: MediaLibraryService) {
        if (!useMedia3Session) return
        media3Service = service
        // Recreate scope in case release() was called previously (service restart).
        if (scope.coroutineContext[Job]?.isActive != true) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
        val seedPlayer = SeedStatePlayer(Looper.getMainLooper())
        placeholderPlayer = seedPlayer
        val placeholder = seedPlayer

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
            podcastManager = podcastManager,
            settings = settings,
            actions = actions,
            bookmarkHelper = bookmarkHelper,
            scopeProvider = { scope },
            contextProvider = { context },
            commandMutex = commandMutex,
        )
        media3LibraryCallback = Media3LibrarySessionCallback(
            sessionCallback = media3Callback!!,
            browseTreeProvider = browseTreeProvider,
            playbackManager = playbackManager,
            episodeManager = episodeManager,
            podcastManager = podcastManager,
            settings = settings,
            packageValidator = if (!BuildConfig.DEBUG) {
                PackageValidator(context, LR.xml.allowed_media_browser_callers)
            } else {
                null
            },
            scopeProvider = { scope },
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
        pendingPlayer?.let { player ->
            if (player is CastStatePlayer) {
                installCastPlayerInternal(player)
            } else {
                installPlayer(player)
            }
        }

        // Synchronously seed the SeedStatePlayer so the timeline is non-empty and AAOS
        // shows a Now Playing screen immediately on cold start.
        val episode = playbackManager.getCurrentEpisode()
        if (episode != null && placeholderPlayer != null) {
            seedPlayer.seed(episode.playedUpToMs.toLong())
        }

        // Asynchronously enrich metadata with podcast name + artwork.
        forwardingPlayer?.let { replayMetadataToPlayer(it) }
    }

    /**
     * Called from [PlaybackManager] when a [SimplePlayer] is created, providing the
     * underlying ExoPlayer reference so the Media3 session can wrap it.
     */
    @OptIn(UnstableApi::class)
    @MainThread
    fun installPlayer(exoPlayer: androidx.media3.common.Player) {
        if (!useMedia3Session) return
        val currentPlayer = forwardingPlayer
        if (currentPlayer == null) {
            Timber.i("installPlayer: session not ready, deferring")
            pendingPlayer = exoPlayer
            return
        }
        // Avoid redundant swaps when called with the same player (e.g., multiple play() calls)
        if (currentPlayer.wrappedPlayer === exoPlayer) return
        pendingPlayer = null
        castStatePlayer = null
        val swapped = currentPlayer.swapPlayer(exoPlayer)
        forwardingPlayer = swapped
        media3Session?.player = swapped
        placeholderPlayer?.release()
        placeholderPlayer = null
        replayMetadataToPlayer(swapped)
        Timber.i("Media3 session player swapped")
    }

    /**
     * Creates a [CastStatePlayer] and installs it into the Media3 session so that
     * notifications and lock screen controls reflect cast playback state.
     */
    @OptIn(UnstableApi::class)
    @MainThread
    fun installCastPlayer() {
        if (!useMedia3Session) return
        val player = CastStatePlayer(
            applicationLooper = android.os.Looper.getMainLooper(),
            onPlay = { scope.launch { commandMutex.withLock { playbackManager.playQueueSuspend(sourceView = source) } } },
            onPause = { scope.launch { commandMutex.withLock { playbackManager.pauseSuspend(sourceView = source) } } },
            onSeekTo = { ms ->
                scope.launch {
                    commandMutex.withLock {
                        playbackManager.seekToTimeMsSuspend(ms.toInt())
                        playbackManager.trackPlaybackSeek(ms.toInt(), source)
                    }
                }
            },
            onStop = { scope.launch { commandMutex.withLock { playbackManager.pauseSuspend(sourceView = source) } } },
        )
        castStatePlayer = player
        installCastPlayerInternal(player)
    }

    /**
     * Installs a [CastStatePlayer] into the Media3 session without ForwardingPlayer
     * callbacks. CastStatePlayer already delegates transport commands (play, pause, seek,
     * stop) to PlaybackManager via its own callbacks, so the ForwardingPlayer must NOT
     * also have callbacks — otherwise every command fires twice.
     */
    @OptIn(UnstableApi::class)
    @MainThread
    private fun installCastPlayerInternal(castPlayer: CastStatePlayer) {
        val currentPlayer = forwardingPlayer
        if (currentPlayer == null) {
            Timber.i("installCastPlayer: session not ready, deferring")
            pendingPlayer = castPlayer
            return
        }
        if (currentPlayer.wrappedPlayer === castPlayer) return
        pendingPlayer = null
        val swapped = PocketCastsForwardingPlayer(
            wrappedPlayer = castPlayer,
            onSkipForward = { scope.launch { commandMutex.withLock { playbackManager.skipForwardSuspend() } } },
            onSkipBack = { scope.launch { commandMutex.withLock { playbackManager.skipBackwardSuspend() } } },
            playGuard = currentPlayer.playGuard,
        ).also {
            it.currentMediaItem = currentPlayer.currentMediaItem
            it.previousMediaId = currentPlayer.previousMediaId
            it.isTransientLoss = currentPlayer.isTransientLoss
        }
        forwardingPlayer = swapped
        media3Session?.player = swapped
        placeholderPlayer?.release()
        placeholderPlayer = null
        replayMetadataToPlayer(swapped)
        Timber.i("Media3 session cast player installed (no transport callbacks)")
    }

    /**
     * Asynchronously fetches the current episode metadata and artwork, then applies
     * it to the given [player]. Guarded by an identity check so that a stale replay
     * (e.g., if another player swap happened during the async work) is discarded.
     *
     * Called after every player swap ([installPlayer], [installCastPlayerInternal],
     * [createSession]) to ensure the Media3 notification has content. This is
     * necessary because [observeForMedia3Updates] may have dropped the playback
     * state event while [forwardingPlayer] was still null.
     */
    @OptIn(UnstableApi::class)
    private fun replayMetadataToPlayer(player: PocketCastsForwardingPlayer) {
        scope.launch(Dispatchers.IO) {
            try {
                val state = playbackManager.playbackStateRelay.blockingFirst()
                if (state.isEmpty) return@launch
                val episode = episodeManager.findEpisodeByUuid(state.episodeUuid) ?: return@launch
                val podcast = when (episode) {
                    is PodcastEpisode -> podcastManager.findPodcastByUuidBlocking(episode.podcastUuid)
                    else -> null
                }
                val showArtwork = settings.showArtworkOnLockScreen.value
                val useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork
                val artworkData = if (showArtwork && !Util.isWearOs(context) && !Util.isAutomotive(context)) {
                    AutoConverter.getPodcastArtworkBitmap(episode, context, useEpisodeArtwork)?.let { bitmap ->
                        java.io.ByteArrayOutputStream().use { stream ->
                            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                android.graphics.Bitmap.CompressFormat.WEBP_LOSSY
                            } else {
                                @Suppress("DEPRECATION")
                                android.graphics.Bitmap.CompressFormat.WEBP
                            }
                            bitmap.compress(format, 80, stream)
                            stream.toByteArray()
                        }
                    }
                } else {
                    null
                }
                withContext(Dispatchers.Main) {
                    if (forwardingPlayer === player) {
                        player.updateMetadata(episode, podcast, showArtwork, useEpisodeArtwork, artworkData)
                        player.isTransientLoss = state.transientLoss
                        updateMedia3CustomLayout()
                        media3Service?.triggerNotificationUpdate()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to replay metadata after player install")
            }
        }
    }

    /**
     * Forwards cast playback state changes to the [CastStatePlayer] so that the
     * Media3 session reflects the correct play/pause/buffering state during cast.
     */
    @MainThread
    fun updateCastState(isPlaying: Boolean, isBuffering: Boolean, positionMs: Long) {
        if (!useMedia3Session) return
        castStatePlayer?.updateCastState(isPlaying, isBuffering, positionMs)
    }

    fun startServiceIfNeeded(context: Context) {
        if (useMedia3Session) {
            if (media3Session != null) return
        }
        val component = resolveMediaBrowserServiceComponent(context)
        if (component == null) {
            Timber.e("No enabled media browser service found in manifest")
            return
        }
        try {
            context.startService(Intent().setComponent(component))
        } catch (e: Exception) {
            Timber.e(e, "Failed to start ${component.className}")
        }
    }

    @OptIn(UnstableApi::class)
    private fun observeForMedia3Updates() {
        val episodeAndState = playbackManager.playbackStateRelay
            .distinctUntilChanged { old, new ->
                old.episodeUuid == new.episodeUuid &&
                    old.state == new.state &&
                    old.isPlaying == new.isPlaying &&
                    old.transientLoss == new.transientLoss &&
                    old.isBuffering == new.isBuffering
            }
            .observeOn(Schedulers.io())
            .switchMap { state ->
                if (state.isEmpty) {
                    Observable.just(Optional.empty<BaseEpisode>() to state)
                } else {
                    episodeManager.findEpisodeByUuidRxFlowable(state.episodeUuid)
                        .distinctUntilChanged(BaseEpisode.isMediaSessionEqual)
                        .map { Optional.of(it) to state }
                        .onErrorReturn { Optional.empty<BaseEpisode>() to state }
                        .toObservable()
                }
            }

        val artworkConfig = settings.artworkConfiguration.flow.asObservable()
        val showArtworkOnLockScreen = settings.showArtworkOnLockScreen.flow.asObservable()

        Observables.combineLatest(episodeAndState, artworkConfig, showArtworkOnLockScreen) { episodeState, config, showArtwork ->
            Triple(episodeState, config.useEpisodeArtwork, showArtwork)
        }
            .observeOn(Schedulers.io())
            .map<Optional<MediaUpdateData>> { (episodeState, useEpisodeArtwork, showArtwork) ->
                val (episodeOpt, state) = episodeState
                if (!episodeOpt.isPresent()) {
                    return@map Optional.empty()
                }
                val episode = episodeOpt.get()!!
                val podcast = when (episode) {
                    is PodcastEpisode -> podcastManager.findPodcastByUuidBlocking(episode.podcastUuid)
                    else -> null
                }
                val artworkData = if (showArtwork && !Util.isWearOs(context) && !Util.isAutomotive(context)) {
                    AutoConverter.getPodcastArtworkBitmap(
                        episode,
                        context,
                        useEpisodeArtwork,
                    )?.let { bitmap ->
                        java.io.ByteArrayOutputStream().use { stream ->
                            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                android.graphics.Bitmap.CompressFormat.WEBP_LOSSY
                            } else {
                                @Suppress("DEPRECATION")
                                android.graphics.Bitmap.CompressFormat.WEBP
                            }
                            bitmap.compress(format, 80, stream)
                            stream.toByteArray()
                        }
                    }
                } else {
                    null
                }
                Optional.of(MediaUpdateData(episode, podcast, state, showArtwork, useEpisodeArtwork, artworkData))
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { dataOpt ->
                    val player = forwardingPlayer ?: return@subscribeBy
                    val data = dataOpt.get()
                    if (data == null) {
                        player.clearMetadata()
                        return@subscribeBy
                    }
                    player.updateMetadata(data.episode, data.podcast, data.showArtwork, data.useEpisodeArtwork, data.artworkData)
                    player.isTransientLoss = data.state.transientLoss
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
            .onEach { withContext(Dispatchers.Main) { updateMedia3CustomLayout() } }
            .catch { Timber.e(it) }
            .launchIn(scope)

        playbackManager.upNextQueue.changesObservable
            .observeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    media3Session?.notifyChildrenChanged(UP_NEXT_ROOT, Int.MAX_VALUE, null)
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

        if (Util.isAutomotive(context)) {
            // Automotive: use circular seek icons matching the configured skip duration.
            // Playback speed first (gets the extra slot), then skip buttons, then custom actions.
            if (playbackManager.isAudioEffectsAvailable()) {
                buttons.add(
                    CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                        .setSessionCommand(SessionCommand(APP_ACTION_CHANGE_SPEED, Bundle.EMPTY))
                        .setDisplayName(context.getString(LR.string.playback_speed))
                        .setCustomIconResId(speedToDrawable(playbackManager.getPlaybackSpeed()))
                        .build(),
                )
            }
            buttons.add(
                CommandButton.Builder(skipBackIconForDuration(settings.skipBackInSecs.value))
                    .setSessionCommand(SessionCommand(APP_ACTION_SKIP_BACK, Bundle.EMPTY))
                    .setDisplayName(context.getString(LR.string.skip_back))
                    .setCustomIconResId(IR.drawable.media_skipback)
                    .build(),
            )
            buttons.add(
                CommandButton.Builder(skipForwardIconForDuration(settings.skipForwardInSecs.value))
                    .setSessionCommand(SessionCommand(APP_ACTION_SKIP_FWD, Bundle.EMPTY))
                    .setDisplayName(context.getString(LR.string.skip_forward))
                    .setCustomIconResId(IR.drawable.media_skipforward)
                    .build(),
            )
            val visibleCount = if (settings.customMediaActionsVisibility.value) MediaNotificationControls.MAX_VISIBLE_OPTIONS else 0
            settings.mediaControlItems.value.take(visibleCount).forEach { mediaControl ->
                if (mediaControl != MediaNotificationControls.PlaybackSpeed) {
                    buildCustomActionButton(mediaControl, currentEpisode)?.let(buttons::add)
                }
            }
        } else {
            // Mobile/other: existing behavior unchanged
            if (useCustomSkipButtons()) {
                buttons.add(
                    CommandButton.Builder(CommandButton.ICON_SKIP_BACK)
                        .setSessionCommand(SessionCommand(APP_ACTION_SKIP_BACK, Bundle.EMPTY))
                        .setDisplayName(context.getString(LR.string.skip_back))
                        .setCustomIconResId(IR.drawable.media_skipback)
                        .build(),
                )
                buttons.add(
                    CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD)
                        .setSessionCommand(SessionCommand(APP_ACTION_SKIP_FWD, Bundle.EMPTY))
                        .setDisplayName(context.getString(LR.string.skip_forward))
                        .setCustomIconResId(IR.drawable.media_skipforward)
                        .build(),
                )
            }

            val visibleCount = if (settings.customMediaActionsVisibility.value) MediaNotificationControls.MAX_VISIBLE_OPTIONS else 0
            settings.mediaControlItems.value.take(visibleCount).forEach { mediaControl ->
                buildCustomActionButton(mediaControl, currentEpisode)?.let(buttons::add)
            }
        }

        session.setCustomLayout(buttons)
        session.setMediaButtonPreferences(buttons)
    }

    private fun buildCustomActionButton(mediaControl: MediaNotificationControls, currentEpisode: BaseEpisode?): CommandButton? {
        return when (mediaControl) {
            MediaNotificationControls.Archive ->
                CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                    .setSessionCommand(SessionCommand(APP_ACTION_ARCHIVE, Bundle.EMPTY))
                    .setDisplayName(context.getString(LR.string.archive))
                    .setCustomIconResId(IR.drawable.ic_archive)
                    .build()

            MediaNotificationControls.MarkAsPlayed ->
                CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                    .setSessionCommand(SessionCommand(APP_ACTION_MARK_AS_PLAYED, Bundle.EMPTY))
                    .setDisplayName(context.getString(LR.string.mark_as_played))
                    .setCustomIconResId(IR.drawable.auto_markasplayed)
                    .build()

            MediaNotificationControls.PlayNext ->
                CommandButton.Builder(CommandButton.ICON_NEXT)
                    .setSessionCommand(SessionCommand(APP_ACTION_PLAY_NEXT, Bundle.EMPTY))
                    .setDisplayName(context.getString(LR.string.play_next))
                    .setCustomIconResId(com.google.android.gms.cast.framework.R.drawable.cast_ic_mini_controller_skip_next)
                    .build()

            MediaNotificationControls.PlaybackSpeed -> {
                if (playbackManager.isAudioEffectsAvailable()) {
                    CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                        .setSessionCommand(SessionCommand(APP_ACTION_CHANGE_SPEED, Bundle.EMPTY))
                        .setDisplayName(context.getString(LR.string.playback_speed))
                        .setCustomIconResId(speedToDrawable(playbackManager.getPlaybackSpeed()))
                        .build()
                } else {
                    null
                }
            }

            MediaNotificationControls.Star -> {
                if (currentEpisode is PodcastEpisode) {
                    if (currentEpisode.isStarred) {
                        CommandButton.Builder(CommandButton.ICON_HEART_FILLED)
                            .setSessionCommand(SessionCommand(APP_ACTION_UNSTAR, Bundle.EMPTY))
                            .setDisplayName(context.getString(LR.string.unstar))
                            .setCustomIconResId(IR.drawable.auto_starred)
                            .build()
                    } else {
                        CommandButton.Builder(CommandButton.ICON_HEART_UNFILLED)
                            .setSessionCommand(SessionCommand(APP_ACTION_STAR, Bundle.EMPTY))
                            .setDisplayName(context.getString(LR.string.star))
                            .setCustomIconResId(IR.drawable.auto_star)
                            .build()
                    }
                } else {
                    null
                }
            }
        }
    }

    private fun skipBackIconForDuration(seconds: Int): @CommandButton.Icon Int = when (seconds) {
        in Int.MIN_VALUE..7 -> CommandButton.ICON_SKIP_BACK_5
        in 8..12 -> CommandButton.ICON_SKIP_BACK_10
        in 13..22 -> CommandButton.ICON_SKIP_BACK_15
        else -> CommandButton.ICON_SKIP_BACK_30
    }

    private fun skipForwardIconForDuration(seconds: Int): @CommandButton.Icon Int = when (seconds) {
        in Int.MIN_VALUE..7 -> CommandButton.ICON_SKIP_FORWARD_5
        in 8..12 -> CommandButton.ICON_SKIP_FORWARD_10
        in 13..22 -> CommandButton.ICON_SKIP_FORWARD_15
        else -> CommandButton.ICON_SKIP_FORWARD_30
    }

    fun release() {
        disposables.clear()
        scope.cancel()
        if (useMedia3Session) {
            media3Session?.release()
            media3Session = null
            media3Service = null
            forwardingPlayer = null
            placeholderPlayer?.release()
            placeholderPlayer = null
            castStatePlayer = null
            media3Callback = null
            media3LibraryCallback = null
            pendingPlayer = null
        } else {
            mediaSession?.release()
        }
    }

    private fun observeCustomMediaActionsVisibility() {
        scope.launch {
            settings.customMediaActionsVisibility.flow.collect {
                withContext(Dispatchers.Main) {
                    val playbackStateCompat = getPlaybackStateCompat(playbackManager.playbackStateRelay.blockingFirst(), currentEpisode = playbackManager.getCurrentEpisode())
                    updatePlaybackState(playbackStateCompat)
                }
            }
        }
    }

    private fun observeMediaNotificationControls() {
        scope.launch {
            settings.mediaControlItems.flow.collect {
                withContext(Dispatchers.Main) {
                    val playbackStateCompat = getPlaybackStateCompat(playbackManager.playbackStateRelay.blockingFirst(), currentEpisode = playbackManager.getCurrentEpisode())
                    updatePlaybackState(playbackStateCompat)
                }
            }
        }
    }

    private fun connect() {
        // MediaBrowserCompat must be created on a thread with a Looper (main thread).
        // startObserving() may be called from a background dispatcher (e.g., AutomotiveApplication).
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val component = resolveMediaBrowserServiceComponent(context) ?: run {
                Timber.e("No enabled media browser service found for connect()")
                return@post
            }
            val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {}
            val mediaBrowser = MediaBrowserCompat(context, component, connectionCallback, null)
            mediaBrowser.connect()
        }
    }

    /**
     * Resolves the currently-enabled media browser service from the manifest.
     * This avoids hardcoding service class names, which differ between app variants
     * (e.g., phone uses [PlaybackService]/[LegacyPlaybackService],
     * automotive uses AutoPlaybackService).
     */
    private fun resolveMediaBrowserServiceComponent(context: Context): ComponentName? {
        val intent = Intent("android.media.browse.MediaBrowserService").apply {
            setPackage(context.packageName)
        }
        val services = context.packageManager.queryIntentServices(intent, 0)
        return services.firstOrNull()?.serviceInfo?.let {
            ComponentName(it.packageName, it.name)
        }
    }

    private fun getPlaybackStateRx(playbackState: PlaybackState, currentEpisode: Optional<BaseEpisode>): io.reactivex.Single<PlaybackStateCompat> {
        return io.reactivex.Single.fromCallable {
            getPlaybackStateCompat(playbackState, currentEpisode.get())
        }
    }

    private fun updatePlaybackState(playbackState: PlaybackStateCompat) {
        Timber.i("MediaSession playback state. $playbackState")
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun getPlaybackStateCompat(playbackState: PlaybackState, currentEpisode: BaseEpisode?): PlaybackStateCompat {
        if (playbackState.isError) {
            mediaSession?.isActive = false
            return PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_ERROR, 0, 0f)
                .setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, playbackState.lastErrorMessage)
                .build()
        }

        if (playbackState.isPlaying || playbackState.transientLoss) {
            mediaSession?.isActive = true
        }

        if (playbackState.isEmpty || currentEpisode == null) {
            val stateBuilder = PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
            return stateBuilder.build()
        }

        val state = if (playbackState.isPlaying) {
            if (playbackState.isBuffering) PlaybackStateCompat.STATE_BUFFERING else PlaybackStateCompat.STATE_PLAYING
        } else {
            if (playbackState.state == PlaybackState.State.STOPPED) PlaybackStateCompat.STATE_STOPPED else PlaybackStateCompat.STATE_PAUSED
        }

        val currentSpeed = playbackState.playbackSpeed
        val stateBuilder = PlaybackStateCompat.Builder()
            .setState(state, playbackState.positionMs.toLong(), currentSpeed.toFloat(), SystemClock.elapsedRealtime())
            .setActions(getSupportedActions(playbackState))
            .setExtras(
                Bundle().apply {
                    putString(PLAYBACK_STATE_EXTRAS_KEY_MEDIA_ID, currentEpisode.uuid)
                    putBoolean(EXTRA_TRANSIENT, playbackState.transientLoss)
                },
            )

        if (!Util.isWearOs(context)) {
            addCustomActions(stateBuilder, currentEpisode, playbackState)
        }

        return stateBuilder.build()
    }

    private fun getSupportedActions(playbackState: PlaybackState): Long {
        val prepareActions = PlaybackStateCompat.ACTION_PREPARE or
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
            PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH

        if (playbackState.isEmpty) {
            return PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                prepareActions
        } else {
            val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_REWIND or
                prepareActions

            return if (useCustomSkipButtons()) {
                actions
            } else {
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    actions
            }
        }
    }

    private fun updateUpNext(upNext: UpNextQueue.State, useEpisodeArtwork: Boolean) {
        try {
            mediaSession?.setQueueTitle("Up Next")
            if (upNext is UpNextQueue.State.Loaded) {
                updateMetadata(upNext.episode, useEpisodeArtwork)

                val items = upNext.queue.map { episode ->
                    val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else null
                    val podcast = podcastUuid?.let { podcastManager.findPodcastByUuidBlocking(it) }
                    val podcastTitle = episode.displaySubtitle(podcast)
                    val localUri = AutoConverter.getPodcastArtworkUri(podcast, episode, context, settings.artworkConfiguration.value.useEpisodeArtwork)
                    val description = MediaDescriptionCompat.Builder()
                        .setDescription(episode.episodeDescription)
                        .setTitle(episode.title)
                        .setSubtitle(podcastTitle)
                        .setMediaId(episode.uuid)
                        .setIconUri(localUri)
                        .build()

                    return@map MediaSessionCompat.QueueItem(description, episode.adapterId)
                }
                mediaSession?.setQueue(items)
            } else {
                updateMetadata(null, useEpisodeArtwork)
                mediaSession?.setQueue(emptyList())

                val playbackStateCompat = getPlaybackStateCompat(PlaybackState(state = PlaybackState.State.EMPTY), currentEpisode = null)
                updatePlaybackState(playbackStateCompat)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun observePlaybackState() {
        val ignoreStates = listOf(
            PlaybackManager.LastChangeFrom.OnUpdateBufferPosition.value,
            PlaybackManager.LastChangeFrom.OnUpdateCurrentPosition.value,
            PlaybackManager.LastChangeFrom.OnUserSeeking.value,
        )

        var previousEpisode: BaseEpisode? = null

        playbackManager.playbackStateRelay
            .observeOn(Schedulers.io())
            .switchMap { state ->
                val episodeSource =
                    if (state.isEmpty) {
                        Observable.just(Optional.empty())
                    } else {
                        episodeManager.findEpisodeByUuidRxFlowable(state.episodeUuid)
                            .distinctUntilChanged(BaseEpisode.isMediaSessionEqual)
                            .map { Optional.of(it) }
                            .onErrorReturn { Optional.empty() }
                            .toObservable()
                    }
                Observables.combineLatest(Observable.just(state), episodeSource)
            }
            .filter {
                !ignoreStates.contains(it.first.lastChangeFrom) || !BaseEpisode.isMediaSessionEqual(it.second.get(), previousEpisode)
            }
            .doOnNext {
                previousEpisode = it.second.get()
            }
            .switchMap { (state, episode) -> getPlaybackStateRx(state, episode).toObservable().onErrorResumeNext(Observable.empty()) }
            .switchMap {
                Observable.fromCallable { updatePlaybackState(it) }
                    .doOnError { LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Error updating playback state in media session: ${it.message}") }.retry(3)
            }
            .ignoreElements()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { throwable ->
                    LogBuffer.e(LogBuffer.TAG_PLAYBACK, "MEDIA SESSION ERROR: Error updating playback state: ${throwable.message}")
                },
            ).addTo(disposables)
    }

    private fun updateMetadata(episode: BaseEpisode?, useEpisodeArtwork: Boolean) {
        if (episode == null) {
            Timber.i("MediaSession metadata. Nothing Playing.")
            mediaSession?.setMetadata(NOTHING_PLAYING)
            return
        }

        val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else null
        val podcast = podcastUuid?.let { podcastManager.findPodcastByUuidBlocking(it) }

        val podcastTitle = episode.displaySubtitle(podcast)
        val safeCharacterPodcastTitle = podcastTitle.replace("%", "pct")
        var nowPlayingBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, episode.uuid)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, safeCharacterPodcastTitle)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, episode.durationMs.toLong())
            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Podcast")
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, episode.title)

        if (episode is PodcastEpisode) {
            nowPlayingBuilder.putRating(MediaMetadataCompat.METADATA_KEY_RATING, RatingCompat.newHeartRating(episode.isStarred))
        }

        if (podcast != null && podcast.author.isNotEmpty()) {
            nowPlayingBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, podcast.author)
        }

        Timber.i("MediaSession metadata. Episode: ${episode.uuid} ${episode.title} Duration: ${episode.durationMs.toLong()}")

        if (settings.showArtworkOnLockScreen.value) {
            val bitmapUri = AutoConverter.getPodcastArtworkUri(podcast, episode, context, useEpisodeArtwork)?.toString()
            nowPlayingBuilder = nowPlayingBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, bitmapUri)
            if (Util.isAutomotive(context)) nowPlayingBuilder = nowPlayingBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, bitmapUri)

            if (!Util.isWearOs(context) && !Util.isAutomotive(context)) {
                AutoConverter.getPodcastArtworkBitmap(episode, context, useEpisodeArtwork)?.let { bitmap ->
                    nowPlayingBuilder = nowPlayingBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                }
            }
            Timber.i("MediaSession metadata. With artwork.")
        }

        val nowPlaying = nowPlayingBuilder.build()
        mediaSession?.setMetadata(nowPlaying)
    }

    private fun addCustomActions(stateBuilder: PlaybackStateCompat.Builder, currentEpisode: BaseEpisode, playbackState: PlaybackState) {
        if (useCustomSkipButtons()) {
            addCustomAction(stateBuilder, APP_ACTION_SKIP_BACK, "Skip back", IR.drawable.media_skipback)
            addCustomAction(stateBuilder, APP_ACTION_SKIP_FWD, "Skip forward", IR.drawable.media_skipforward)
        }

        val visibleCount = if (settings.customMediaActionsVisibility.value) MediaNotificationControls.MAX_VISIBLE_OPTIONS else 0
        settings.mediaControlItems.value.take(visibleCount).forEach { mediaControl ->
            when (mediaControl) {
                MediaNotificationControls.Archive -> addCustomAction(stateBuilder, APP_ACTION_ARCHIVE, "Archive", IR.drawable.ic_archive)

                MediaNotificationControls.MarkAsPlayed -> addCustomAction(stateBuilder, APP_ACTION_MARK_AS_PLAYED, "Mark as played", IR.drawable.auto_markasplayed)

                MediaNotificationControls.PlayNext -> addCustomAction(stateBuilder, APP_ACTION_PLAY_NEXT, "Play next", com.google.android.gms.cast.framework.R.drawable.cast_ic_mini_controller_skip_next)

                MediaNotificationControls.PlaybackSpeed -> {
                    if (playbackManager.isAudioEffectsAvailable()) {
                        stateBuilder.addCustomAction(APP_ACTION_CHANGE_SPEED, "Change speed", speedToDrawable(playbackState.playbackSpeed))
                    }
                }

                MediaNotificationControls.Star -> {
                    if (currentEpisode is PodcastEpisode) {
                        if (currentEpisode.isStarred) {
                            addCustomAction(stateBuilder, APP_ACTION_UNSTAR, "Unstar", IR.drawable.auto_starred)
                        } else {
                            addCustomAction(stateBuilder, APP_ACTION_STAR, "Star", IR.drawable.auto_star)
                        }
                    }
                }
            }
        }
    }

    private fun addCustomAction(stateBuilder: PlaybackStateCompat.Builder, action: String, name: CharSequence, @DrawableRes icon: Int) {
        val addToWearExtras = Bundle().apply {
            putBoolean("android.support.wearable.media.extra.CUSTOM_ACTION_SHOW_ON_WEAR", true)
        }

        val skipBackBuilder = PlaybackStateCompat.CustomAction.Builder(action, name, icon).apply {
            setExtras(addToWearExtras)
        }
        stateBuilder.addCustomAction(skipBackBuilder.build())
    }

    inner class MediaSessionCallback(
        val playbackManager: PlaybackManager,
        val episodeManager: EpisodeManager,
        val enqueueCommand: (String, suspend () -> Unit) -> Unit,
    ) : MediaSessionCompat.Callback() {

        private var playFromSearchDisposable: Disposable? = null
        private val mediaEventQueue = MediaEventQueue(scopeProvider = { this@MediaSessionManager.scope })

        override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
            if (Intent.ACTION_MEDIA_BUTTON == mediaButtonEvent.action) {
                val keyEvent = IntentCompat.getParcelableExtra(mediaButtonEvent, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java) ?: return false
                logEvent(keyEvent.toString())
                if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Media button Android event: ${keyEvent.action}")
                    val inputEvent = when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> MediaEvent.SingleTap
                        KeyEvent.KEYCODE_MEDIA_NEXT -> MediaEvent.DoubleTap
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaEvent.TripleTap
                        else -> null
                    }
                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Media button input event: ${keyEvent.action}")

                    if (inputEvent != null) {
                        scope.launch {
                            val outputEvent = mediaEventQueue.consumeEvent(inputEvent)
                            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Media button output event: ${keyEvent.action}")
                            when (outputEvent) {
                                MediaEvent.SingleTap -> handleMediaButtonSingleTap()
                                MediaEvent.DoubleTap -> handleMediaButtonDoubleTap()
                                MediaEvent.TripleTap -> handleMediaButtonTripleTap()
                                null -> Unit
                            }
                        }
                        return true
                    }
                }
            } else {
                logEvent("onMediaButtonEvent(${mediaButtonEvent.action ?: "unknown action"})")
            }

            return super.onMediaButtonEvent(mediaButtonEvent)
        }

        private fun onAddBookmark() {
            logEvent("add bookmark")
            val coroutineContext = CoroutineScope(Dispatchers.Main + Job())
            coroutineContext.launch {
                Util.isAndroidAutoConnectedFlow(context).collect {
                    bookmarkHelper.handleAddBookmarkAction(context, it)
                    coroutineContext.cancel()
                }
            }
        }

        private fun logEvent(action: String) {
            val userInfo = runCatching {
                val info = mediaSession!!.currentControllerInfo
                "Controller: ${info.packageName} pid: ${info.pid} uid: ${info.uid}"
            }.getOrNull()
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Event from Media Session to $action. ${userInfo.orEmpty()}")
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
                HeadphoneAction.ADD_BOOKMARK -> onAddBookmark()

                HeadphoneAction.SKIP_FORWARD -> {
                    onSkipToNext()
                    if (!playbackManager.isPlaying()) {
                        enqueueCommand("play") { playbackManager.playQueueSuspend(source) }
                    }
                }

                HeadphoneAction.SKIP_BACK -> {
                    onSkipToPrevious()
                }

                HeadphoneAction.NEXT_CHAPTER,
                HeadphoneAction.PREVIOUS_CHAPTER,
                -> Timber.e(ACTION_NOT_SUPPORTED)
            }
        }

        override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
            super.onPrepareFromSearch(query, extras)
            onPlayFromSearch(query, extras)
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPrepareFromMediaId(mediaId, extras)
            onPlayFromMediaId(mediaId, extras)
        }

        override fun onPlay() {
            if (Util.isAutomotive(context) && !settings.automotiveConnectedToMediaSession()) {
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Auto start playback ignored just after automotive app restart.")
                return
            }

            logEvent("play")
            enqueueCommand("play") { playbackManager.playQueueSuspend(sourceView = source) }
        }

        override fun onPause() {
            logEvent("pause")
            enqueueCommand("pause") { playbackManager.pauseSuspend(sourceView = source) }
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            logEvent("play from search")
            playFromSearchDisposable?.dispose()
            playFromSearchDisposable = performPlayFromSearchRx(query)
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = { Timber.e(it) })
        }

        override fun onStop() {
            if (playbackManager.player !is CastPlayer) {
                logEvent("stop")
                enqueueCommand("stop") { playbackManager.pauseSuspend(sourceView = source) }
            }
        }

        override fun onSkipToPrevious() {
            onRewind()
        }

        override fun onSkipToNext() {
            onFastForward()
        }

        override fun onRewind() {
            logEvent("skip backwards")
            enqueueCommand("skip backwards") { playbackManager.skipBackwardSuspend(sourceView = source) }
        }

        override fun onFastForward() {
            logEvent("skip forwards")
            enqueueCommand("skip forwards") { playbackManager.skipForwardSuspend(sourceView = source) }
        }

        override fun onSetRating(rating: RatingCompat?) {
            super.onSetRating(rating)

            if (rating?.hasHeart() == true) {
                actions.starEpisode()
            } else {
                actions.unstarEpisode()
            }
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            mediaId ?: return
            scope.launch {
                logEvent("play from media id")

                val autoMediaId = au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoMediaId.fromMediaId(mediaId)
                val episodeId = autoMediaId.episodeId
                episodeManager.findEpisodeByUuid(episodeId)?.let { episode ->
                    enqueueCommand("play from media id") {
                        playbackManager.playNowSuspend(episode = episode, sourceView = source)
                    }
                }
            }
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            action ?: return

            when (action) {
                APP_ACTION_SKIP_BACK -> enqueueCommand("custom action: skip back") {
                    playbackManager.skipBackwardSuspend()
                }

                APP_ACTION_SKIP_FWD -> enqueueCommand("custom action: skip forward") {
                    playbackManager.skipForwardSuspend()
                }

                APP_ACTION_MARK_AS_PLAYED -> actions.markAsPlayed()

                APP_ACTION_STAR -> actions.starEpisode()

                APP_ACTION_UNSTAR -> actions.unstarEpisode()

                APP_ACTION_CHANGE_SPEED -> actions.changePlaybackSpeed()

                APP_ACTION_ARCHIVE -> actions.archive()

                APP_ACTION_PLAY_NEXT -> enqueueCommand("custom action: play next") {
                    playbackManager.playNextInQueue()
                }
            }
        }

        override fun onSkipToQueueItem(id: Long) {
            val state = playbackManager.upNextQueue.changesObservable.blockingFirst()
            if (state is UpNextQueue.State.Loaded) {
                state.queue.find { it.adapterId == id }?.let { episode ->
                    logEvent("play from skip to queue item")
                    enqueueCommand("skip to queue item") {
                        playbackManager.playNowSuspend(episode = episode, sourceView = source)
                    }
                }
            }
        }

        override fun onSeekTo(pos: Long) {
            logEvent("seek to $pos")
            enqueueCommand("seek to $pos") {
                playbackManager.seekToTimeMsSuspend(pos.toInt())
                playbackManager.trackPlaybackSeek(pos.toInt(), SourceView.MEDIA_BUTTON_BROADCAST_ACTION)
            }
        }
    }

    fun playFromSearchExternal(query: String) {
        actions.performPlayFromSearch(query)
    }

    private fun performPlayFromSearchRx(searchTerm: String?): Completable {
        return actions.performPlayFromSearchRx(searchTerm)
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

typealias QueuedCommand = Pair<String, suspend () -> Unit>

internal const val APP_ACTION_STAR = "star"
internal const val APP_ACTION_UNSTAR = "unstar"
internal const val APP_ACTION_SKIP_BACK = "jumpBack"
internal const val APP_ACTION_SKIP_FWD = "jumpFwd"
internal const val APP_ACTION_MARK_AS_PLAYED = "markAsPlayed"
internal const val APP_ACTION_CHANGE_SPEED = "changeSpeed"
internal const val APP_ACTION_ARCHIVE = "archive"
internal const val APP_ACTION_PLAY_NEXT = "playNext"

private val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
    .build()

private data class MediaUpdateData(
    val episode: BaseEpisode,
    val podcast: Podcast?,
    val state: PlaybackState,
    val showArtwork: Boolean,
    val useEpisodeArtwork: Boolean,
    val artworkData: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MediaUpdateData) return false
        return episode == other.episode && podcast == other.podcast && state == other.state &&
            showArtwork == other.showArtwork && useEpisodeArtwork == other.useEpisodeArtwork &&
            artworkData.contentEquals(other.artworkData)
    }

    override fun hashCode(): Int {
        var result = episode.hashCode()
        result = 31 * result + (podcast?.hashCode() ?: 0)
        result = 31 * result + state.hashCode()
        result = 31 * result + showArtwork.hashCode()
        result = 31 * result + useEpisodeArtwork.hashCode()
        result = 31 * result + (artworkData?.contentHashCode() ?: 0)
        return result
    }
}
