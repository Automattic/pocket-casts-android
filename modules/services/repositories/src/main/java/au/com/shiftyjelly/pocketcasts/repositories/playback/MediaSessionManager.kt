package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
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
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkHelper
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.PackageValidator
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.getLaunchActivityPendingIntent
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class MediaSessionManager(
    val playbackManager: PlaybackManager,
    val podcastManager: PodcastManager,
    val episodeManager: EpisodeManager,
    val settings: Settings,
    val context: Context,
    val episodeAnalytics: EpisodeAnalytics,
    val bookmarkManager: BookmarkManager,
    val browseTreeProvider: BrowseTreeProvider,
    private val notificationHelper: NotificationHelper,
    applicationScope: CoroutineScope,
) : CoroutineScope {
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

    internal val actions = MediaSessionActions(
        playbackManager = playbackManager,
        podcastManager = podcastManager,
        episodeManager = episodeManager,
        settings = settings,
        episodeAnalytics = episodeAnalytics,
        scope = this,
        source = source,
    )

    private var bookmarkHelper: BookmarkHelper

    // --- Media3 session ---
    private var media3Session: MediaLibraryService.MediaLibrarySession? = null
    private var forwardingPlayer: PocketCastsForwardingPlayer? = null
    private var media3Callback: Media3SessionCallback? = null
    private var media3LibraryCallback: Media3LibrarySessionCallback? = null
    private var media3NotificationBuilder: Media3NotificationBuilder? = null
    private var placeholderPlayer: androidx.media3.common.Player? = null

    internal fun getMedia3Session(): MediaLibraryService.MediaLibrarySession? = media3Session
    internal fun getMedia3NotificationBuilder(): Media3NotificationBuilder? = media3NotificationBuilder

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

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
            onSkipForward = { launch { playbackManager.skipForwardSuspend() } },
            onSkipBack = { launch { playbackManager.skipBackwardSuspend() } },
            onStop = {
                if (playbackManager.player !is CastPlayer) {
                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Media3: stop → pause")
                    launch { playbackManager.pauseSuspend(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION) }
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
            scope = this,
            contextProvider = { context },
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
            scope = this,
            contextProvider = { context },
        )

        media3Session = MediaLibraryService.MediaLibrarySession.Builder(service, forwardingPlayer!!, media3LibraryCallback!!)
            .setId("PocketCastsMedia3Session")
            .apply {
                if (!Util.isAutomotive(context)) {
                    setSessionActivity(context.getLaunchActivityPendingIntent())
                }
            }
            .build()
        media3NotificationBuilder = Media3NotificationBuilder(context, notificationHelper, settings)
        Timber.i("Media3 session created")
    }

    /**
     * Called from [PlaybackManager] when a [SimplePlayer] is created, providing the
     * underlying ExoPlayer reference so the Media3 session can wrap it.
     */
    @OptIn(UnstableApi::class)
    @MainThread
    fun installPlayer(exoPlayer: androidx.media3.common.Player) {
        forwardingPlayer = forwardingPlayer!!.swapPlayer(exoPlayer)
        media3Session?.player = forwardingPlayer!!
        placeholderPlayer?.release()
        placeholderPlayer = null
        Timber.i("Media3 session player swapped")
    }

    @OptIn(UnstableApi::class)
    private fun observeForMedia3Updates() {
        // Observe playback state changes to update the ForwardingPlayer metadata
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
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { (episodeOpt, state) ->
                    val player = forwardingPlayer ?: return@subscribeBy
                    val episode = episodeOpt.get() ?: return@subscribeBy
                    val podcast = when (episode) {
                        is PodcastEpisode -> podcastManager.findPodcastByUuidBlocking(episode.podcastUuid)
                        else -> null
                    }
                    player.updateMetadata(episode, podcast)
                    player.isTransientLoss = state.transientLoss
                },
                onError = { Timber.e(it, "Error observing Media3 updates") },
            )
            .addTo(disposables)

        // Observe custom media actions to update Media3 custom layout
        combine(
            settings.customMediaActionsVisibility.flow,
            settings.mediaControlItems.flow,
        ) { visibility, items -> visibility to items }
            .onEach { updateMedia3CustomLayout() }
            .catch { Timber.e(it) }
            .launchIn(this)
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
                                .setCustomIconResId(IR.drawable.auto_1)
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
    }

    fun release() {
        disposables.clear()
        cancel()
        media3Session?.release()
        media3Session = null
        placeholderPlayer?.release()
        placeholderPlayer = null
    }

    fun playFromSearchExternal(query: String) {
        actions.performPlayFromSearch(query)
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
