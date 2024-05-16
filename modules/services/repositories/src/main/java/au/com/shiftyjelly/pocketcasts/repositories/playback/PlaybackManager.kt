package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.toLiveData
import androidx.media3.datasource.HttpDataSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.repositories.R
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.di.NotificationPermissionChecker
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper.removeEpisodeFromQueue
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.file.CloudFilesManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.LocalPlayer.Companion.VOLUME_DUCK
import au.com.shiftyjelly.pocketcasts.repositories.playback.LocalPlayer.Companion.VOLUME_NORMAL
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.toServerPostFile
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.NotificationBroadcastReceiver
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.widget.WidgetManager
import au.com.shiftyjelly.pocketcasts.servers.sync.EpisodeSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.EpisodeSyncResponse
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.isPositive
import au.com.shiftyjelly.pocketcasts.utils.featureflag.BookmarkFeatureControl
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.Relay
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileInputStream
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitSingleOrNull
import kotlinx.coroutines.rx2.rxCompletable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
open class PlaybackManager @Inject constructor(
    private val settings: Settings,
    private var podcastManager: PodcastManager,
    private var episodeManager: EpisodeManager,
    private var statsManager: StatsManager,
    private val playerManager: PlayerFactory,
    private var castManager: CastManager,
    @ApplicationContext private val application: Context,
    private val widgetManager: WidgetManager,
    private val playlistManager: PlaylistManager,
    private val downloadManager: DownloadManager,
    val upNextQueue: UpNextQueue,
    private val notificationHelper: NotificationHelper,
    private val userEpisodeManager: UserEpisodeManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val episodeAnalytics: EpisodeAnalytics,
    private val syncManager: SyncManager,
    private val cloudFilesManager: CloudFilesManager,
    private val bookmarkManager: BookmarkManager,
    private val showNotesManager: ShowNotesManager,
    private val chapterManager: ChapterManager,
    private val sleepTimer: SleepTimer,
    bookmarkFeature: BookmarkFeatureControl,
    private val playbackManagerNetworkWatcherFactory: PlaybackManagerNetworkWatcher.Factory,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val crashLogging: CrashLogging,
) : FocusManager.FocusChangeListener, AudioNoisyManager.AudioBecomingNoisyListener, CoroutineScope {

    companion object {
        private const val UPDATE_EVERY = 5
        private const val UPDATE_TIMER_POLL_TIME: Long = 1000
        private const val MAX_TIME_WITHOUT_FOCUS_FOR_RESUME_MINUTES = 30
        private const val MAX_TIME_WITHOUT_FOCUS_FOR_RESUME = (MAX_TIME_WITHOUT_FOCUS_FOR_RESUME_MINUTES * 60 * 1000).toLong()
        private const val PAUSE_TIMER_DELAY = ((MAX_TIME_WITHOUT_FOCUS_FOR_RESUME_MINUTES + 1) * 60 * 1000).toLong()
        private const val CONTENT_TYPE_KEY = "content_type"
        private const val SOURCE_KEY = "source"
        private const val SEEK_TO_PERCENT_KEY = "seek_to_percent"
        private const val SEEK_FROM_PERCENT_KEY = "seek_from_percent"
        const val SPEED_KEY = "speed"
        const val AMOUNT_KEY = "amount"
        const val ENABLED_KEY = "enabled"
    }

    private var notificationPermissionChecker: NotificationPermissionChecker? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val focusManager: FocusManager by lazy {
        FocusManager(settings, application).apply {
            focusChangeListener = this@PlaybackManager
        }
    }
    private var audioNoisyManager =
        AudioNoisyManager(application)

    private val bookmarkTonePlayer: MediaPlayer by lazy {
        MediaPlayer().apply {
            setDataSource(application, Uri.parse("android.resource://${application.packageName}/${R.raw.bookmark_creation_sound}"))
            prepare()
        }
    }

    private val sleepTimeTonePlayer: MediaPlayer by lazy {
        MediaPlayer().apply {
            setDataSource(application, Uri.parse("android.resource://${application.packageName}/${R.raw.sleep_time_device_shake_confirmation_sound}"))
            prepare()
        }
    }

    val playbackStateRelay: Relay<PlaybackState> by lazy {
        val relay = BehaviorRelay.create<PlaybackState>().toSerialized()
        relay.accept(PlaybackState(lastChangeFrom = LastChangeFrom.OnInit.value))
        Log.d(Settings.LOG_TAG_AUTO, "Init playback state")
        return@lazy relay
    }
    val playbackStateLive = playbackStateRelay
        .toFlowable(BackpressureStrategy.LATEST)
        .toLiveData()

    private var updateCount = 0
    private var resettingPlayer = false
    private var lastBufferedUpTo: Int = 0
    private var focusWasPlaying: Date? = null
    private var forcePlayerSwitch = false
    private var updateTimerDisposable: Disposable? = null
    private var bufferUpdateTimerDisposable: Disposable? = null
    private var pauseTimerDisposable: Disposable? = null
    private var syncTimerDisposable: Disposable? = null
    private var lastWarnedPlayedEpisodeUuid: String? = null
    private var lastPlayedEpisodeUuid: String? = null

    private val resumptionHelper = ResumptionHelper(settings)

    var episodeSubscription: Disposable? = null

    val mediaSessionManager = MediaSessionManager(
        playbackManager = this,
        podcastManager = podcastManager,
        episodeManager = episodeManager,
        playlistManager = playlistManager,
        settings = settings,
        context = application,
        episodeAnalytics = episodeAnalytics,
        bookmarkManager = bookmarkManager,
        applicationScope = applicationScope,
        bookmarkFeature = bookmarkFeature,
    )

    var episodesUntilSleep: Int = 0

    var chaptersUntilSleep: Int = 0

    private val lastListenedForEndOfChapter = mutableMapOf<String, String?>(
        "chapterUuid" to null,
        "episodeUuid" to null,
    )

    var player: Player? = null

    val mediaSession: MediaSessionCompat
        get() = mediaSessionManager.mediaSession

    @SuppressLint("CheckResult")
    fun setup() {
        if (!Util.isAutomotive(application)) {
            widgetManager.updateWidgetFromPlaybackState(this)
        }

        // load an initial playback state
        upNextQueue.setup()
        mediaSessionManager.startObserving()
        updatePausedPlaybackState()
        val playbackManagerNetworkWatcher = playbackManagerNetworkWatcherFactory.create(::onSwitchedToMeteredConnection)

        launch {
            castManager.startSessionListener(object : CastManager.SessionListener {
                override fun sessionStarted() {
                    castConnected()
                }

                override fun sessionEnded() {
                    castDisconnected()
                }

                override fun sessionReconnected() {
                    castReconnected()
                }
            })
            playbackManagerNetworkWatcher.observeConnection()
        }
    }

    fun getCurrentEpisode(): BaseEpisode? {
        return upNextQueue.currentEpisode
    }

    fun getCurrentPodcast(): Podcast? {
        val currentEpisode = getCurrentEpisode() as? PodcastEpisode ?: return null
        return currentEpisode.podcastUuid.let { podcastManager.findPodcastByUuid(it) }
    }

    private suspend fun autoLoadEpisode(autoPlay: Boolean): BaseEpisode? {
        val nextEpisode = getCurrentEpisode()
        if (nextEpisode != null) {
            return nextEpisode
        }

        if (!settings.autoPlayNextEpisodeOnEmpty.value) {
            return null
        }

        // auto queue next episode on empty
        val autoPlayEpisode = autoSelectNextEpisode() ?: return null

        withContext(Dispatchers.Default) {
            upNextQueue.playNext(autoPlayEpisode, downloadManager) {
                launch {
                    loadCurrentEpisode(play = autoPlay, sourceView = SourceView.AUTO_PLAY)
                }
            }
        }

        analyticsTracker.track(
            AnalyticsEvent.PLAYBACK_EPISODE_AUTOPLAYED,
            mapOf("episode_uuid" to autoPlayEpisode.uuid),
        )
        return autoPlayEpisode
    }

    fun isPlaying(): Boolean {
        return playbackStateRelay.blockingFirst().isPlaying
    }

    fun isStreaming(): Boolean {
        return player?.isStreaming ?: false
    }

    fun updateSleepTimerStatus(sleepTimeRunning: Boolean, sleepAfterEpisodes: Int = 0, sleepAfterChapters: Int = 0) {
        this.episodesUntilSleep = sleepAfterEpisodes
        this.chaptersUntilSleep = sleepAfterChapters
        playbackStateRelay.blockingFirst().let {
            playbackStateRelay.accept(it.copy(isSleepTimerRunning = sleepTimeRunning, lastChangeFrom = LastChangeFrom.OnUpdateSleepTimerStatus.value))
        }
    }

    fun setupProgressSync() {
        syncTimerDisposable?.dispose()
        syncTimerDisposable = playbackStateRelay.sample(settings.getPeriodicSaveTimeMs(), TimeUnit.MILLISECONDS)
            .concatMap {
                if (it.isPlaying && syncManager.isLoggedIn()) {
                    syncEpisodeProgress(it)
                        .toObservable<EpisodeSyncResponse>()
                        .onErrorResumeNext(Observable.empty())
                } else {
                    Observable.empty()
                }
            }
            .doOnError { LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Could not sync episode progress. ${it.javaClass.name} ${it.message ?: ""}") }
            .onErrorReturnItem(EpisodeSyncResponse())
            .subscribeBy(
                onNext = {
                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Synced episode progress")
                },
            )
    }

    private fun syncEpisodeProgress(playbackState: PlaybackState): Completable {
        val episode = getCurrentEpisode()
        if (playbackState.episodeUuid != episode?.uuid) {
            return Completable.complete()
        }

        return if (episode is PodcastEpisode) {
            val request = EpisodeSyncRequest(
                episode.uuid,
                episode.podcastUuid,
                playbackState.positionMs / 1000L,
                playbackState.durationMs / 1000L,
                EpisodeSyncRequest.STATUS_IN_PROGRESS,
            )
            return syncManager.episodeSync(request)
        } else if (episode is UserEpisode && episode.isUploaded) {
            rxCompletable {
                episode.playedUpToMs = playbackState.positionMs
                userEpisodeManager.updateFiles(listOf(episode))
            }
        } else {
            return Completable.complete()
        }
    }

    suspend fun getCurrentTimeMs(episode: BaseEpisode): Int {
        val player = player
        if (player != null) {
            val currentTimeMs = player.getCurrentPositionMs()
            // check the player has been loaded with the latest episode so there isn't side effects like using an old episode's time with a new episode.
            if (currentTimeMs >= 0 && player.episodeUuid == episode.uuid) {
                return currentTimeMs
            }
        }

        return episode.playedUpToMs
    }

    suspend fun getBufferedUpToMs(): Int {
        return player?.bufferedUpToMs() ?: 0
    }

    fun isPlaybackLocal(): Boolean {
        return !isPlaybackRemote()
    }

    fun isPlaybackRemote(): Boolean {
        return player?.isRemote ?: false
    }

    fun isAudioEffectsAvailable(): Boolean {
        val episode = getCurrentEpisode()
        return episode != null
    }

    fun isTrimSilenceSupported(): Boolean {
        return player?.supportsTrimSilence() ?: false
    }

    fun isVolumeBoostSupported(): Boolean {
        return player?.supportsVolumeBoost() ?: false
    }

    private suspend fun onSwitchedToMeteredConnection() {
        val episode = getCurrentEpisode() ?: return

        if (!shouldWarnWhenSwitchingToMeteredConnection(episode.uuid)) return

        sendDataWarningNotification(episode)
        lastWarnedPlayedEpisodeUuid = episode.uuid
        pauseSuspend(sourceView = SourceView.METERED_NETWORK_CHANGE)
    }

    fun shouldWarnAboutPlayback(episodeUUID: String? = upNextQueue.currentEpisode?.uuid): Boolean {
        return settings.warnOnMeteredNetwork.value && !Network.isUnmeteredConnection(application) && lastWarnedPlayedEpisodeUuid != episodeUUID
    }

    private fun shouldWarnWhenSwitchingToMeteredConnection(episodeUUID: String): Boolean =
        settings.warnOnMeteredNetwork.value &&
            lastWarnedPlayedEpisodeUuid != episodeUUID &&
            (player is LocalPlayer) && // don't warn if chromecasting
            isStreaming() &&
            isPlaying()

    fun getPlaybackSpeed(): Double {
        return playbackStateRelay.blockingFirst().playbackSpeed
    }

    fun playPause(sourceView: SourceView = SourceView.UNKNOWN) {
        if (isPlaying()) {
            pause(sourceView = sourceView)
        } else {
            playQueue(sourceView)
        }
    }

    fun playQueue(
        sourceView: SourceView = SourceView.UNKNOWN,
        showedStreamWarning: Boolean = false,
    ) {
        launch { playQueueSuspend(sourceView, showedStreamWarning) }
    }

    suspend fun playQueueSuspend(
        sourceView: SourceView = SourceView.UNKNOWN,
        showedStreamWarning: Boolean = false,
    ) {
        if (upNextQueue.currentEpisode != null) {
            loadEpisodeWhenRequired(sourceView, showedStreamWarning)
        }
    }

    fun playNow(
        episode: BaseEpisode,
        forceStream: Boolean = false,
        showedStreamWarning: Boolean = false,
        sourceView: SourceView = SourceView.UNKNOWN,
    ) {
        launch {
            playNowSuspend(
                episode = episode,
                forceStream = forceStream,
                showedStreamWarning = showedStreamWarning,
                sourceView = sourceView,
            )
        }
    }

    suspend fun playNowSuspend(
        episodeUuid: String,
        forceStream: Boolean = false,
        showedStreamWarning: Boolean = false,
        sourceView: SourceView = SourceView.UNKNOWN,
    ) {
        val episode = episodeManager.findEpisodeByUuid(episodeUuid) ?: return
        playNowSuspend(episode, forceStream, showedStreamWarning, sourceView)
    }

    suspend fun playNowSuspend(
        episode: BaseEpisode,
        forceStream: Boolean = false,
        showedStreamWarning: Boolean = false,
        sourceView: SourceView = SourceView.UNKNOWN,
    ) {
        forcePlayerSwitch = true
        playNowSync(
            episode = episode,
            forceStream = forceStream,
            showedStreamWarning = showedStreamWarning,
            sourceView = sourceView,
        )
    }

    suspend fun playNowSync(
        episode: BaseEpisode,
        forceStream: Boolean = false,
        showedStreamWarning: Boolean = false,
        sourceView: SourceView = SourceView.UNKNOWN,
    ) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Play now: ${episode.uuid} ${episode.title}")

        withContext(Dispatchers.IO) {
            if (episode.isArchived) {
                episodeManager.unarchive(episode)
            }

            if (episode.playingStatus == EpisodePlayingStatus.COMPLETED) {
                episodeManager.markAsNotPlayed(episode)
            }
        }

        val switchEpisode: Boolean = !upNextQueue.isCurrentEpisode(episode)
        if (switchEpisode || isPlayerSwitchRequired()) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Player switch required. Different episode: $switchEpisode")
            pause(transientLoss = true)
            upNextQueue.playNow(
                episode = episode,
                automaticUpNextSource = autoPlaySource(sourceView, episode),
                onAdd = {
                    launch {
                        loadCurrentEpisode(
                            play = true,
                            forceStream = forceStream,
                            showedStreamWarning = showedStreamWarning,
                            sourceView = sourceView,
                        )
                    }
                },
            )

            if (episode is PodcastEpisode) {
                // We only want to track playing of episodes, not files
                FirebaseAnalyticsTracker.playedEpisode()
            }
        } else if (!switchEpisode && playbackStateRelay.blockingFirst().isPaused) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "No player switch required. Playing queue.")
            playQueue(
                showedStreamWarning = showedStreamWarning,
                sourceView = sourceView,
            )
        }
    }

    // Returning null means a source should not affect the auto play behavior. Listening history is not
    // returning null because it should actively disable auto play if a user plays an episode from the
    // listening history screen.
    private fun autoPlaySource(sourceView: SourceView, episode: BaseEpisode): AutoPlaySource? =
        when (sourceView) {
            SourceView.AUTO_PAUSE,
            SourceView.AUTO_PLAY,
            SourceView.CHROMECAST,
            SourceView.DISCOVER,
            SourceView.DISCOVER_PLAIN_LIST,
            SourceView.DISCOVER_PODCAST_LIST,
            SourceView.DISCOVER_RANKED_LIST,
            SourceView.FULL_SCREEN_VIDEO,
            SourceView.MINIPLAYER,
            SourceView.MULTI_SELECT,
            SourceView.ONBOARDING_RECOMMENDATIONS,
            SourceView.ONBOARDING_RECOMMENDATIONS_SEARCH,
            SourceView.PODCAST_LIST,
            SourceView.PODCAST_SETTINGS,
            SourceView.PLAYER,
            SourceView.PLAYER_BROADCAST_ACTION,
            SourceView.PLAYER_PLAYBACK_EFFECTS,
            SourceView.PROFILE,
            SourceView.EPISODE_SWIPE_ACTION,
            SourceView.TASKER,
            SourceView.UNKNOWN,
            SourceView.UP_NEXT,
            SourceView.STATS,
            SourceView.WHATS_NEW,
            SourceView.NOTIFICATION_BOOKMARK,
            SourceView.METERED_NETWORK_CHANGE,
            SourceView.WIDGET_PLAYER_SMALL,
            SourceView.WIDGET_PLAYER_MEDIUM,
            SourceView.WIDGET_PLAYER_LARGE,
            SourceView.WIDGET_PLAYER_OLD,
            SourceView.SHARE_LIST,
            SourceView.BOTTOM_SHELF,
            SourceView.SEARCH,
            SourceView.SEARCH_RESULTS,
            -> null

            SourceView.MEDIA_BUTTON_BROADCAST_SEARCH_ACTION,
            SourceView.MEDIA_BUTTON_BROADCAST_ACTION,
            SourceView.NOTIFICATION,
            -> {
                val source = (episode as? PodcastEpisode)?.let { AutoPlaySource.fromId(it.uuid) }
                if (source != null) {
                    settings.trackingAutoPlaySource.set(source, updateModifiedAt = false)
                }
                source
            }

            // These screens should be setting an appropriate value for [AutomaticUpNextSource.mostRecentList]
            // when the user views them, otherwise [AutomaticUpNextSource.create] will not return the proper
            // value.
            SourceView.LISTENING_HISTORY,
            SourceView.DOWNLOADS,
            SourceView.EPISODE_DETAILS,
            SourceView.FILES,
            SourceView.FILTERS,
            SourceView.PODCAST_SCREEN,
            SourceView.STARRED,
            -> settings.trackingAutoPlaySource.value
        }

    suspend fun play(
        upNextPosition: UpNextPosition,
        episode: BaseEpisode,
        source: SourceView,
        userInitiated: Boolean = true,
    ) {
        when (upNextPosition) {
            UpNextPosition.NEXT -> playNext(episode, source, userInitiated)
            UpNextPosition.LAST -> playLast(episode, source, userInitiated)
        }
    }

    /**
     * Add the episode to the Up Next queue so it will play after the current episode.
     */
    suspend fun playNext(
        episode: BaseEpisode,
        source: SourceView,
        userInitiated: Boolean = true,
    ) = withContext(Dispatchers.Default) {
        val wasEmpty: Boolean = upNextQueue.isEmpty
        upNextQueue.playNext(episode, downloadManager, null)
        if (userInitiated) {
            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_ADDED_TO_UP_NEXT, source, true, episode)
        }
        if (wasEmpty) {
            loadCurrentEpisode(play = false)
        }
    }

    suspend fun playLast(
        episode: BaseEpisode,
        source: SourceView,
        userInitiated: Boolean = true,
    ) {
        val wasEmpty: Boolean = upNextQueue.isEmpty
        upNextQueue.playLast(episode, downloadManager, null)
        if (userInitiated) {
            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_ADDED_TO_UP_NEXT, source, false, episode)
        }
        if (wasEmpty) {
            loadCurrentEpisode(play = false)
        }
    }

    private suspend fun loadEpisodeWhenRequired(
        sourceView: SourceView = SourceView.UNKNOWN,
        showedStreamWarning: Boolean = false,
    ) {
        if (isPlayerSwitchRequired()) {
            loadCurrentEpisode(
                play = true,
                showedStreamWarning = showedStreamWarning,
                sourceView = sourceView,
            )
        } else {
            play(sourceView)
        }
    }

    fun playEpisodes(episodes: List<BaseEpisode>, sourceView: SourceView = SourceView.UNKNOWN) {
        if (episodes.isEmpty()) {
            return
        }

        launch {
            val topEpisode = episodes.first()
            playNowSync(episode = topEpisode, sourceView = sourceView)
            if (episodes.size > 1) {
                upNextQueue.clearAndPlayAll(episodes.slice(1 until min(episodes.size, settings.getMaxUpNextEpisodes())), downloadManager)
            }
        }
    }

    fun playEpisodesLast(episodes: List<BaseEpisode>, source: SourceView) {
        if (episodes.isEmpty()) {
            return
        }

        launch {
            val currentEpisode = upNextQueue.currentEpisode?.uuid
            val wasEmpty: Boolean = upNextQueue.isEmpty

            upNextQueue.playAllLast(episodes.filter { it.uuid != currentEpisode }, downloadManager)
            episodeAnalytics.trackBulkEvent(
                event = AnalyticsEvent.EPISODE_BULK_ADD_TO_UP_NEXT,
                count = episodes.size,
                toTop = false,
                source = source,
            )
            if (wasEmpty) {
                loadCurrentEpisode(play = false)
            }
        }
    }

    fun playEpisodesNext(episodes: List<BaseEpisode>, source: SourceView) {
        if (episodes.isEmpty()) {
            return
        }

        launch {
            val currentEpisode = upNextQueue.currentEpisode?.uuid
            val wasEmpty: Boolean = upNextQueue.isEmpty
            upNextQueue.playAllNext(episodes.filter { it.uuid != currentEpisode }, downloadManager)
            episodeAnalytics.trackBulkEvent(
                event = AnalyticsEvent.EPISODE_BULK_ADD_TO_UP_NEXT,
                count = episodes.size,
                toTop = true,
                source = source,
            )
            if (wasEmpty) {
                loadCurrentEpisode(play = false)
            }
        }
    }

    fun moveEpisode(fromPosition: Int, toPosition: Int) {
        launch {
            upNextQueue.moveEpisode(fromPosition, toPosition)
        }
    }

    fun changeUpNext(episodes: List<BaseEpisode>) {
        launch {
            upNextQueue.changeList(episodes)
        }
    }

    fun pause(transientLoss: Boolean = false, sourceView: SourceView = SourceView.UNKNOWN) {
        launch {
            pauseSuspend(transientLoss, sourceView)
        }
    }

    suspend fun pauseSuspend(transientLoss: Boolean = false, sourceView: SourceView = SourceView.UNKNOWN) {
        if (!transientLoss) {
            focusManager.giveUpAudioFocus()
            playbackStateRelay.blockingFirst().let { playbackState ->
                playbackStateRelay.accept(playbackState.copy(transientLoss = false))
            }
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Paused - Not transient")
            trackPlayback(AnalyticsEvent.PLAYBACK_PAUSE, sourceView)
        } else {
            playbackStateRelay.blockingFirst().let { playbackState ->
                playbackStateRelay.accept(playbackState.copy(transientLoss = true))
            }
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Paused - Transient")
        }

        cancelUpdateTimer()

        player?.pause()
    }

    fun stopAsync(isAudioFocusFailed: Boolean = false, sourceView: SourceView = SourceView.UNKNOWN) {
        launch {
            if (!isAudioFocusFailed) {
                trackPlayback(AnalyticsEvent.PLAYBACK_STOP, sourceView)
            }
            stop()
        }
    }

    suspend fun stop() {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Stopping playback")

        cancelUpdateTimer()
        cancelBufferUpdateTimer()

        withContext(Dispatchers.Main) {
            if (player != null) {
                player?.stop()
                player = null
            }
        }

        playbackStateRelay.blockingFirst().let {
            playbackStateRelay.accept(
                it.copy(
                    state = PlaybackState.State.STOPPED,
                    isPrepared = false,
                    lastChangeFrom = LastChangeFrom.OnStop.value,
                ),
            )
        }
    }

    suspend fun shutdown() {
        stop()

        audioNoisyManager.unregister()
        focusManager.giveUpAudioFocus()

        withContext(Dispatchers.Main) {
            playbackStateRelay.accept(PlaybackState(state = PlaybackState.State.EMPTY, lastChangeFrom = LastChangeFrom.OnShutdown.value))
        }
        castManager.endSession()
        widgetManager.updateWidgetNotPlaying()
    }

    suspend fun hibernatePlayback() {
        if (isPlaybackRemote()) {
            return
        }

        stop()
    }

    fun seekToTimeMs(positionMs: Int, seekComplete: (() -> Unit)? = null) {
        launch {
            seekToTimeMsSuspend(positionMs, seekComplete)
        }
    }

    suspend fun seekToTimeMsSuspend(positionMs: Int, seekComplete: (() -> Unit)? = null) {
        seekToTimeMsInternal(positionMs)
        seekComplete?.invoke()
    }

    fun seekIfPlayingToTimeMs(episodeUuid: String, positionMs: Int, seekComplete: (() -> Unit)? = null) {
        // double checking the episode uuid as this may change during the launch
        if (getCurrentEpisode()?.uuid != episodeUuid) {
            return
        }
        launch {
            if (getCurrentEpisode()?.uuid == episodeUuid) {
                seekToTimeMsInternal(positionMs)
                seekComplete?.invoke()
            }
        }
    }

    private suspend fun seekToTimeMsInternal(duration: Duration) {
        seekToTimeMsInternal(duration.inWholeMilliseconds.toInt())
    }

    private suspend fun seekToTimeMsInternal(positionMs: Int) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "PlaybackService seekToTimeMsInternal %.3f ", positionMs.toDouble() / 1000.0)
        val episode = getCurrentEpisode()
        if (episode != null) {
            episode.playedUpToMs = positionMs
        }

        if (player == null) {
            // if the player is sleeping still update the episode progress so the mini player progress still changes
            updateCurrentPositionInDatabase()
        } else {
            // as soon as the user drops the seek progress bar position make sure it has the new playback position, useful for the media session seeking
            withContext(Dispatchers.Main) {
                playbackStateRelay.blockingFirst().let { playbackState ->
                    playbackStateRelay.accept(playbackState.copy(positionMs = positionMs, lastChangeFrom = LastChangeFrom.OnUserSeeking.value))
                }
            }

            player?.seekToTimeMs(positionMs)
        }

        withContext(Dispatchers.Main) {
            updatePausedPlaybackState()
        }
    }

    /**
     * Remove the currently playing episode from the Up Next and load the next episode in the queue.
     */
    fun playNextInQueue(sourceView: SourceView = SourceView.UNKNOWN) {
        val currentEpisode = upNextQueue.currentEpisode
        if (currentEpisode == null || upNextQueue.queueEpisodes.isEmpty()) {
            return
        }
        removeEpisode(currentEpisode, sourceView)
    }

    fun skipForward(
        sourceView: SourceView = SourceView.UNKNOWN,
        jumpAmountSeconds: Int = settings.skipForwardInSecs.value,
    ) {
        launch {
            skipForwardSuspend(sourceView, jumpAmountSeconds)
        }
    }

    suspend fun skipForwardSuspend(
        sourceView: SourceView = SourceView.UNKNOWN,
        jumpAmountSeconds: Int = settings.skipForwardInSecs.value,
    ) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Skip forward tapped")

        val episode = getCurrentEpisode() ?: return
        val jumpAmountMs = jumpAmountSeconds * 1000

        val currentTimeMs = getCurrentTimeMs(episode = episode)
        if (currentTimeMs < 0 || player?.episodeUuid != episode.uuid) return // Make sure the player hasn't changed episodes before using the current time to seek

        val newPositionMs = currentTimeMs + jumpAmountMs
        val durationMs = player?.durationMs() ?: Int.MAX_VALUE // If we don't have a duration, just let them skip

        statsManager.addTimeSavedSkipping((newPositionMs - currentTimeMs).toLong())
        if (newPositionMs < durationMs) {
            seekToTimeMsInternal(newPositionMs)
        } else {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Seek beyond end of episode so completing. ${episode.uuid}")
            onCompletion(episode.uuid)
        }

        trackPlayback(AnalyticsEvent.PLAYBACK_SKIP_FORWARD, sourceView)
    }

    fun skipBackward(sourceView: SourceView = SourceView.UNKNOWN, jumpAmountSeconds: Int = settings.skipBackInSecs.value) {
        launch {
            skipBackwardSuspend(sourceView, jumpAmountSeconds)
        }
    }

    suspend fun skipBackwardSuspend(sourceView: SourceView = SourceView.UNKNOWN, jumpAmountSeconds: Int = settings.skipBackInSecs.value) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Skip backward tapped")

        val episode = getCurrentEpisode() ?: return

        val jumpAmountMs = jumpAmountSeconds * 1000
        val currentTimeMs = getCurrentTimeMs(episode = episode)
        if (currentTimeMs < 0) return

        val newPositionMs = Math.max(currentTimeMs - jumpAmountMs, 0)
        seekToTimeMsInternal(newPositionMs)

        trackPlayback(AnalyticsEvent.PLAYBACK_SKIP_BACK, sourceView)
    }

    fun skipToNextSelectedOrLastChapter() {
        launch {
            val episode = getCurrentEpisode() ?: return@launch
            val currentTimeMs = getCurrentTimeMs(episode = episode)
            playbackStateRelay.blockingFirst().chapters.getNextSelectedChapter(currentTimeMs.milliseconds)?.let { chapter ->
                seekToTimeMsInternal(chapter.startTime)
                trackPlayback(AnalyticsEvent.PLAYBACK_CHAPTER_SKIPPED, SourceView.PLAYER)
            } ?: skipToEndOfLastChapter()
        }
    }

    fun skipToPreviousSelectedOrLastChapter() {
        launch {
            val episode = getCurrentEpisode() ?: return@launch
            val currentTimeMs = getCurrentTimeMs(episode)
            playbackStateRelay.blockingFirst().chapters.getPreviousSelectedChapter(currentTimeMs.milliseconds)?.let { chapter ->
                seekToTimeMsInternal(chapter.startTime)
                trackPlayback(AnalyticsEvent.PLAYBACK_CHAPTER_SKIPPED, SourceView.PLAYER)
            }
        }
    }

    private fun skipToEndOfLastChapter() {
        launch {
            playbackStateRelay.blockingFirst().chapters.lastChapter?.let { chapter ->
                seekToTimeMsInternal(chapter.endTime)
                trackPlayback(AnalyticsEvent.PLAYBACK_CHAPTER_SKIPPED, SourceView.PLAYER)
            }
        }
    }

    fun skipToChapter(chapter: Chapter) {
        launch {
            if (chapter.selected) {
                seekToTimeMsInternal(chapter.startTime)
            } else {
                skipToNextSelectedOrLastChapter()
            }
        }
    }

    fun skipToChapter(index: Int) {
        launch {
            val chapter = playbackStateRelay.blockingFirst().chapters.getList().firstOrNull { it.index == index } ?: return@launch
            if (chapter.selected) {
                seekToTimeMsInternal(chapter.startTime)
            } else {
                skipToNextSelectedOrLastChapter()
            }
        }
    }

    fun toggleChapter(select: Boolean, chapter: Chapter) {
        launch {
            getCurrentEpisode()?.let { episode ->
                when (episode) {
                    is PodcastEpisode -> {
                        if (select) {
                            episodeManager.selectChapterIndexForEpisode(chapter.index, episode)
                        } else {
                            episodeManager.deselectChapterIndexForEpisode(chapter.index, episode)
                        }
                    }
                    is UserEpisode -> {
                        if (select) {
                            userEpisodeManager.selectChapterIndexForEpisode(chapter.index, episode)
                        } else {
                            userEpisodeManager.deselectChapterIndexForEpisode(chapter.index, episode)
                        }
                    }
                }
            }
            playbackStateRelay.blockingFirst().let { playbackState ->
                val updatedItems = playbackState.chapters.getList().map {
                    if (it.index == chapter.index) {
                        it.copy(selected = select)
                    } else {
                        it
                    }
                }

                playbackStateRelay.accept(
                    playbackState.copy(
                        chapters = playbackState.chapters.copy(items = updatedItems),
                        lastChangeFrom = LastChangeFrom.OnChapterSelectionToggled.value,
                    ),
                )
            }
        }
    }

    fun clearUpNextAsync() {
        launch {
            upNextQueue.clearUpNext()
        }
    }

    fun endPlaybackAndClearUpNextAsync() {
        launch {
            shutdown()
            upNextQueue.removeAll()
        }
    }

    fun updatePlayerEffects(effects: PlaybackEffects) {
        launch {
            val player = player
            player?.setPlaybackEffects(effects)

            withContext(Dispatchers.Main) {
                playbackStateRelay.blockingFirst().let { playbackState ->
                    playbackStateRelay.accept(
                        playbackState.copy(
                            playbackSpeed = effects.playbackSpeed,
                            isVolumeBoosted = effects.isVolumeBoosted,
                            trimMode = effects.trimMode,
                            lastChangeFrom = LastChangeFrom.OnEffectsChanged.value,
                        ),
                    )
                }
            }
        }
    }

    private val removeMutex = Mutex()
    fun removeEpisode(episodeToRemove: BaseEpisode?, source: SourceView, userInitiated: Boolean = true) {
        launch {
            if (episodeToRemove == null) {
                return@launch
            }

            removeMutex.withLock {
                val currentEpisode = getCurrentEpisode()

                val isCurrentEpisode = currentEpisode != null && currentEpisode.uuid == episodeToRemove.uuid && (player == null || player?.episodeUuid == episodeToRemove.uuid)
                val isPlaying = isPlaying()

                if (isCurrentEpisode) {
                    // when there is another episode in the Up Next and we are playing, don't stop so the foreground service isn't stopped
                    val moreEpisodes = upNextQueue.size > 1
                    if (moreEpisodes && isPlaying) {
                        pause(transientLoss = true)
                    } else {
                        stop()
                    }
                }

                upNextQueue.removeEpisode(episodeToRemove)
                if (userInitiated) {
                    episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_REMOVED_FROM_UP_NEXT, source, episodeToRemove.uuid)
                }

                if (isCurrentEpisode) {
                    loadCurrentEpisode(play = isPlaying, sourceView = SourceView.AUTO_PLAY)
                }
            }
        }
    }

    private suspend fun onRemoteMetaDataNotMatched(episodeUuid: String) {
        val episode = episodeManager.findEpisodeByUuid(episodeUuid) ?: return
        val podcast = if (episode is PodcastEpisode) podcastManager.findPodcastByUuid(episode.podcastUuid) else null

        if (player?.isRemote == true && player?.isPlaying() == false) {
            if (castManager.isPlaying()) {
                Timber.d("Playing remote episode %s", episode.title)
                playNowSync(episode)
            } else {
                loadCurrentEpisode(play = false)
            }

            player?.setEpisode(episode)
            player?.setPodcast(podcast)
        }
    }

    fun castReconnected() {
        launch(Dispatchers.Main) {
            if (player != null) {
                player?.stop()
                player = null
            }

            player = playerManager.createCastPlayer(this@PlaybackManager::onPlayerEvent)
            (player as? CastPlayer)?.updateFromRemoteIfRequired()
            Timber.i("Cast reconnected. Creating media player of type CastPlayer")

            setupUpdateTimer()
        }
    }

    fun castConnected() {
        upNextQueue.currentEpisode ?: return
        launch {
            if (isPlayerSwitchRequired()) {
                loadCurrentEpisode(true, sourceView = SourceView.CHROMECAST)
            }
        }
    }

    fun castDisconnected() {
        upNextQueue.currentEpisode ?: return
        launch {
            updateCurrentPositionInDatabase()

            stop()

            if (isPlayerSwitchRequired()) {
                loadCurrentEpisode(false, sourceView = SourceView.CHROMECAST)
            }
        }
    }

    /** LISTENERS  */

    suspend fun onPlayerError(event: PlayerEvent.PlayerError) {
        val episode = getCurrentEpisode()

        try {
            val output = StringBuilder("Playback error: ")
            output.append(event.message)
            output.append(" ")
            if (episode == null) {
                output.append("Episode is null. ")
            } else {
                output.append("Episode: ")
                    .append(episode.uuid).append(" ")
                    .append(if (episode.downloadedFilePath == null) "Download file path is empty!" else episode.downloadedFilePath).append(" ")

                val path = episode.downloadedFilePath
                if (path != null) {
                    val episodeFile = File(path)

                    val episodeStream = FileInputStream(episodeFile)
                    val descriptor = episodeStream.fd

                    output.append(if (episodeFile.exists()) "File exists. " else "File doesn't exist. ")
                        .append(if (episodeFile.canRead()) "File can be read. " else "File can't be read. ")
                        .append(if (descriptor.valid()) "File descriptor is valid. " else "File descriptor is invalid! ")
                }
            }
            Timber.e(output.toString())
        } catch (e: Exception) {
            Timber.e(e, "Problems logging error.")
        }

        LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Player error %s", event.message)

        val currentEpisode = getCurrentEpisode()
        if (currentEpisode is BaseEpisode) {
            episodeManager.markAsPlaybackError(currentEpisode, event, isPlaybackRemote())
        }

        stop()
        onPlayerPaused()

        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let { playbackState ->
                val errorMessage = if (event.error?.cause is HttpDataSource.HttpDataSourceException) {
                    application.getString(LR.string.player_play_failed_check_internet)
                } else {
                    event.message
                }
                crashLogging.sendReport(
                    message = "Illegal playback state encountered",
                    exception = event.error ?: IllegalStateException(event.message),
                    tags = mapOf(
                        "episodeUuid" to episode?.uuid.orEmpty(),
                        "playedUpTo" to episode?.playedUpTo?.roundToInt().toString(),
                    ),
                )
                playbackStateRelay.accept(playbackState.copy(state = PlaybackState.State.ERROR, lastErrorMessage = errorMessage, lastChangeFrom = LastChangeFrom.OnPlayerError.value))
            }
        }
    }

    suspend fun onBufferingStateChanged() {
        player?.let {
            val isBuffering = it.isBuffering()
            val bufferedMs = it.bufferedUpToMs()
            withContext(Dispatchers.Main) {
                playbackStateRelay.blockingFirst().let { playbackState ->
                    if (playbackState.isBuffering == isBuffering) {
                        return@withContext
                    }
                    playbackStateRelay.accept(
                        playbackState.copy(
                            isBuffering = isBuffering,
                            bufferedMs = bufferedMs,
                            lastChangeFrom = LastChangeFrom.OnBufferingStateChanged.value,
                        ),
                    )
                }
            }
        }
    }

    fun onPlayerPlaying() {
        Timber.i("PlaybackService onPlayerPlaying")

        val episode = getCurrentEpisode() ?: return
        val podcast = findPodcastByEpisode(episode)

        playbackStateRelay.blockingFirst().let { playbackState ->
            playbackStateRelay.accept(playbackState.copy(state = PlaybackState.State.PLAYING, transientLoss = false, lastChangeFrom = LastChangeFrom.OnPlayerPlaying.value))
        }

        episodeManager.updatePlayingStatus(episode, EpisodePlayingStatus.IN_PROGRESS)

        setupUpdateTimer()
        setupBufferUpdateTimer(episode)
        cancelPauseTimer()

        widgetManager.updateWidget(podcast, true, episode)
    }

    fun markPodcastNeedsUpdating(podcastUuid: String) {
        launch {
            val playingPodcastUuid = playbackStateRelay.blockingFirst().podcast?.uuid
            if (podcastUuid == playingPodcastUuid) {
                val updatedPodcast = withContext(Dispatchers.Default) { podcastManager.findPodcastByUuid(playingPodcastUuid) }
                playbackStateRelay.blockingFirst().let { state ->
                    if (updatedPodcast != null && updatedPodcast.uuid == state.podcast?.uuid) { // Make sure it hasn't changed while loaded the updated version
                        playbackStateRelay.accept(state.copy(podcast = updatedPodcast, lastChangeFrom = LastChangeFrom.OnMarkPodcastNeedsUpdating.value))
                    }
                }
            }
        }
    }

    suspend fun onPlayerPaused() {
        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let { playbackState ->
                playbackStateRelay.accept(playbackState.copy(state = PlaybackState.State.PAUSED, lastChangeFrom = LastChangeFrom.OnPlayerPaused.value))
            }
        }

        val episode = getCurrentEpisode()

        player?.let {
            val positionMs = it.getCurrentPositionMs()
            if (positionMs > 0) {
                updateCurrentPositionInDatabase()

                episode?.let { episode ->
                    resumptionHelper.paused(episode, positionMs)
                }
            }
        }

        cancelUpdateTimer()

        val podcast = if (episode == null) null else findPodcastByEpisode(episode)
        widgetManager.updateWidget(podcast, false, episode)

        setupPauseTimer()
    }

    private suspend fun onCompletion(episodeUUID: String?) {
        if (resettingPlayer) {
            return
        }

        // keep hold of this as deleting the episode might change the member variable
        val hadSleepAfterEpisode = isSleepAfterEpisodeEnabled()
        val wasPlaying = isPlaying()

        cancelUpdateTimer()
        cancelBufferUpdateTimer()

        val episode = getCurrentEpisode()

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Episode ${episode?.title} finished, should sleep: $hadSleepAfterEpisode")

        if (hadSleepAfterEpisode) {
            sleepEndOfEpisode(episode)
        }

        if (!isSleepAfterEpisodeEnabled()) {
            cancelUpdateTimer()
        }
        cancelBufferUpdateTimer()

        if (episode != null) {
            if (episode.uuid != episodeUUID) {
                // We have already completed this episode, don't do it again or we may skip the next one
                LogBuffer.e(LogBuffer.TAG_PLAYBACK, "OnCompletion uuid does not match playback state current episode, ignoring onComplete event.")
                return
            }

            // remove from Up Next
            upNextQueue.removeEpisode(episode)

            // stop the downloads
            episodeManager.updateAutoDownloadStatus(episode, PodcastEpisode.AUTO_DOWNLOAD_STATUS_IGNORE)
            removeEpisodeFromQueue(episode, "finished", downloadManager)

            // mark as played
            episodeManager.updatePlayingStatus(episode, EpisodePlayingStatus.COMPLETED)

            // auto archive after playing
            if (episode is PodcastEpisode) {
                episodeManager.archivePlayedEpisode(episode, this, podcastManager, sync = true)
            } else if (episode is UserEpisode) {
                userEpisodeManager.deletePlayedEpisodeIfReq(episode, this)
            }

            // Sync played to server straight away
            if (syncManager.isLoggedIn()) {
                if (episode is PodcastEpisode) {
                    val syncRequest =
                        EpisodeSyncRequest(
                            episode.uuid,
                            episode.podcastUuid,
                            episode.durationMs.toLong() / 1000,
                            episode.durationMs.toLong() / 1000,
                            EpisodeSyncRequest.STATUS_COMPLETE,
                        )
                    syncManager.episodeSync(syncRequest)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete { Timber.d("Synced episode completion") }
                        .doOnError { Timber.e("Could not sync episode completion ${it.message}") }
                        .onErrorComplete()
                        .subscribe()
                } else if (episode is UserEpisode) {
                    userEpisodeManager.findEpisodeByUuid(episode.uuid)?.let { userEpisode ->
                        syncManager.postFiles(listOf(userEpisode.toServerPostFile()))
                            .ignoreElement()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete { Timber.d("Synced user episode completion") }
                            .doOnError { Timber.e("Could not sync user episode completion ${it.message}") }
                            .onErrorComplete()
                            .subscribe()
                    }
                }
            }
        }

        // Auto play if it had sleep time enabled for end of episodes and still has episodes set on sleep time
        // or if it did not have sleep time end of episode configured
        // and it was playing episode
        val autoPlay = (!hadSleepAfterEpisode || isSleepAfterEpisodeEnabled()) && wasPlaying

        var nextEpisode = getCurrentEpisode()
        if (nextEpisode == null) {
            nextEpisode = autoLoadEpisode(autoPlay)
            if (nextEpisode == null) {
                stop()
                shutdown()
            }
        } else {
            loadCurrentEpisode(play = autoPlay, sourceView = SourceView.AUTO_PLAY)
        }
    }

    private suspend fun autoSelectNextEpisode(): BaseEpisode? {
        val allEpisodes: List<BaseEpisode> = when (val autoSource = settings.lastAutoPlaySource.value) {
            is AutoPlaySource.Downloads -> episodeManager.observeDownloadEpisodes().asFlow().firstOrNull()
            is AutoPlaySource.Files -> cloudFilesManager.sortedCloudFiles.firstOrNull()
            is AutoPlaySource.Starred -> episodeManager.observeStarredEpisodes().asFlow().firstOrNull()
            // First check if it is a podcast uuid, then check if it is from a filter
            is AutoPlaySource.PodcastOrFilter -> podcastManager.findPodcastByUuid(autoSource.uuid)
                ?.let { podcast -> autoPlayOrderForPodcastEpisodes(podcast) }
                ?: playlistManager.findByUuidSync(autoSource.uuid)
                    ?.let { playlist -> playlistManager.findEpisodes(playlist, episodeManager, this) }
            is AutoPlaySource.None -> null
        } ?: emptyList()

        val allEpisodeUuids = allEpisodes.map { it.uuid }
        val lastEpisodeIndex = allEpisodeUuids.indexOfFirst { it == lastPlayedEpisodeUuid }

        // go down the episode list until the end, and then go up the episode list
        val episodeUuidsSlice = allEpisodeUuids.slice(lastEpisodeIndex + 1 until allEpisodeUuids.size) + allEpisodeUuids.slice(0 until lastEpisodeIndex).asReversed()

        // Return the first episode that is not archived or finished
        for (episodeUuid in episodeUuidsSlice) {
            val episode = episodeManager.findEpisodeByUuid(episodeUuid) ?: continue
            if (!episode.isArchived && !episode.isFinished) {
                return episode
            }
        }

        // If no matches found, play the latest episode on Android Automotive to avoid the player stopping
        return when (Util.getAppPlatform(application)) {
            AppPlatform.Automotive -> episodeManager.findLatestEpisodeToPlay()

            AppPlatform.Phone,
            AppPlatform.WearOs,
            -> null
        }
    }

    private fun autoPlayOrderForPodcastEpisodes(podcast: Podcast): List<PodcastEpisode> {
        val episodes = episodeManager
            .findEpisodesByPodcastOrdered(podcast)

        val modifiedEpisodes = when (podcast.grouping) {
            PodcastGrouping.None,
            PodcastGrouping.Season,
            PodcastGrouping.Starred,
            -> episodes

            // Move just played episode back to downloaded group for purposes of finding the next episode to play
            PodcastGrouping.Downloaded ->
                episodes.map {
                    if (it.uuid == lastPlayedEpisodeUuid) {
                        it.copy(episodeStatus = EpisodeStatusEnum.DOWNLOADED)
                    } else {
                        it
                    }
                }

            // Move just played episode back to unplayed group for purposes of finding the next episode to play
            PodcastGrouping.Unplayed ->
                episodes.map {
                    if (it.uuid == lastPlayedEpisodeUuid) {
                        it.copy(playingStatus = EpisodePlayingStatus.NOT_PLAYED)
                    } else {
                        it
                    }
                }
        }

        return podcast.grouping
            .formGroups(modifiedEpisodes, podcast, application.resources)
            .flatten()
    }

    private suspend fun sleepEndOfEpisode(episode: BaseEpisode?) {
        if (isSleepAfterEpisodeEnabled()) {
            episode?.uuid?.let { sleepTimer.setEndOfEpisodeUuid(it) }
            episodesUntilSleep -= 1
        }

        if (isSleepAfterEpisodeEnabled()) return

        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let {
                playbackStateRelay.accept(it.copy(isSleepTimerRunning = false, lastChangeFrom = LastChangeFrom.OnCompletion.value))
            }
        }
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Sleeping playback for end of episode")

        showToast(application.getString(LR.string.player_sleep_time_fired))

        val podcast = playbackStateRelay.blockingFirst().podcast
        if (podcast != null && podcast.skipLastSecs > 0) {
            pause(sourceView = SourceView.AUTO_PAUSE)
        }
        onPlayerPaused()

        // jump back 5 seconds from the current time so when the player opens it doesn't complete before giving the user a chance to skip back
        player?.let {
            val currentTimeMs = it.getCurrentPositionMs() - 5000
            if (currentTimeMs > 0) {
                val currentTimeSecs = currentTimeMs.toDouble() / 1000.0
                episodeManager.updatePlayedUpTo(episode, currentTimeSecs, false)
            }
        }

        stop()
    }

    private suspend fun sleepEndOfChapter() {
        if (isSleepAfterChapterEnabled()) {
            chaptersUntilSleep -= 1
            sleepTimer.setEndOfChapter()
        }

        if (isSleepAfterChapterEnabled()) return

        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let {
                playbackStateRelay.accept(it.copy(isSleepTimerRunning = false, lastChangeFrom = LastChangeFrom.OnCompletion.value))
            }
        }
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Sleeping playback for end of chapters")

        showToast(application.getString(LR.string.player_sleep_time_fired_end_of_chapter))

        val podcast = playbackStateRelay.blockingFirst().podcast
        if (podcast != null && podcast.skipLastSecs > 0) {
            pause(sourceView = SourceView.AUTO_PAUSE)
        }
        onPlayerPaused()

        stop()
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(application, message, Toast.LENGTH_LONG).show()
        }
    }

    suspend fun onDurationAvailable() {
        val episode = getCurrentEpisode()
        if (episode == null || player == null) {
            return
        }

        val durationMs = player?.durationMs() ?: 0
        if (durationMs <= 0) {
            return
        }

        val durationDiffSeconds = (durationMs - episode.durationMs) / 1000
        if (abs(durationDiffSeconds) > 0) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "The total episode duration has changed by $durationDiffSeconds seconds")
            analyticsTracker.track(
                AnalyticsEvent.PLAYBACK_EPISODE_DURATION_CHANGED,
                mapOf(
                    "duration_change" to durationDiffSeconds,
                    "duration" to durationMs / 1000,
                    "is_user_file" to (episode is UserEpisode),
                    "is_downloaded" to episode.isDownloaded,
                    "episode_uuid" to episode.uuid,
                    "podcast_uuid" to episode.podcastOrSubstituteUuid,
                ),
            )
        }

        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let { playbackState ->
                if (playbackState.durationMs == durationMs) {
                    return@let
                }
                playbackStateRelay.accept(playbackState.copy(durationMs = durationMs, lastChangeFrom = LastChangeFrom.OnDurationAvailable.value))
            }
        }

        val playerDurationSecs = durationMs.toDouble() / 1000.0
        episodeManager.updateDuration(episode, playerDurationSecs, true)
    }

    suspend fun onSeekComplete(positionMs: Int) {
        playbackStateRelay.blockingFirst().let {
            playbackStateRelay.accept(
                it.copy(
                    positionMs = positionMs,
                    lastChangeFrom = LastChangeFrom.OnSeekComplete.value,
                ),
            )
        }
        updateCurrentPositionInDatabase()
    }

    private fun onMetadataAvailable(episodeMetadata: EpisodeFileMetadata) {
        playbackStateRelay.blockingFirst().let { playbackState ->
            launch {
                chapterManager.updateChapters(
                    playbackState.episodeUuid,
                    episodeMetadata.chapters.toDbChapters(playbackState.episodeUuid, isEmbedded = true),
                )
            }
        }
    }

    @Volatile
    private var observeChaptersJob: Job? = null

    private fun onEpisodeChanged(episodeUuid: String) {
        observeChaptersJob?.cancel()
        observeChaptersJob = chapterManager.observerChaptersForEpisode(episodeUuid)
            .onEach { onChaptersAvailable(it) }
            .launchIn(this)
    }

    private fun onChaptersAvailable(chapters: Chapters) {
        playbackStateRelay.blockingFirst().let { playbackState ->
            playbackStateRelay.accept(playbackState.copy(chapters = chapters))
        }
    }

    override fun onFocusGain(shouldResume: Boolean) {
        val lastPlayingTime = focusWasPlaying
        if (shouldResume && lastPlayingTime != null) {
            val timeout = Date(lastPlayingTime.time + MAX_TIME_WITHOUT_FOCUS_FOR_RESUME)
            if (Date().before(timeout)) {
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Focus gained, resuming playback")
                playQueue()
            } else {
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Focus gained but too much time has passed to resume")
            }
        }

        player?.setVolume(VOLUME_NORMAL)

        focusWasPlaying = null
    }

    override fun onFocusLoss(playOverNotification: PlayOverNotificationSetting, transientLoss: Boolean) {
        val player = player
        if (player == null || player.isRemote) {
            return
        }
        // if we are playing but can't just reduce the volume then play when focus gained
        val playing = isPlaying()
        if ((playOverNotification == PlayOverNotificationSetting.NEVER) && playing) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Focus lost while playing")
            focusWasPlaying = Date()

            pause(transientLoss = transientLoss, sourceView = SourceView.AUTO_PAUSE)
        } else {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Focus lost not playing")
            focusWasPlaying = null
        }

        // check if we need to reduce the volume
        if (playOverNotification == PlayOverNotificationSetting.DUCK) {
            player.setVolume(VOLUME_DUCK)
            return
        }

        player.setVolume(VOLUME_NORMAL)
    }

    override fun onFocusRequestFailed() {
        LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Could not get audio focus, stopping")
        stopAsync(isAudioFocusFailed = true)
    }

    override fun onAudioBecomingNoisy() {
        val player = player
        if (player == null || player.isRemote) {
            return
        }
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "System fired 'Audio Becoming Noisy' event, pausing playback.")
        pause(sourceView = SourceView.AUTO_PAUSE)
        focusWasPlaying = null
    }

    /** PRIVATE METHODS  */

    /**
     * Check the player is initialised and if we are using the correct player either the system or cast player.
     * @param chromeCastConnected
     */
    private suspend fun isPlayerSwitchRequired(): Boolean {
        if (player == null) {
            return true
        }
        if (forcePlayerSwitch) {
            forcePlayerSwitch = false
            return true
        }
        // using Chrome Cast make sure the player is connected
        return if (!castManager.isConnected()) {
            player is CastPlayer
        } else {
            player is SimplePlayer
        } // otherwise use the ExoPlayer
    }

    /**
     * Does the media player need to be recreated.
     */
    private fun isPlayerResetNeeded(episode: BaseEpisode, sameEpisode: Boolean, chromeCastConnected: Boolean): Boolean {
        // reset the player if local and changing episode
        val playbackOnDevice = !chromeCastConnected
        return if (!sameEpisode) {
            playbackOnDevice
        } else {
            episode.isDownloaded &&
                playbackOnDevice &&
                episode.downloadedFilePath != null &&
                player != null &&
                episode.downloadedFilePath != player?.filePath
        }

        // if the player has a different media file path then it is changing
    }

    /**
     * Load the episode
     */
    private suspend fun loadCurrentEpisode(
        play: Boolean,
        showedStreamWarning: Boolean = false,
        forceStream: Boolean = false,
        sourceView: SourceView = SourceView.UNKNOWN,
    ) {
        // make sure we have the most recent copy from the database
        val currentUpNextEpisode = upNextQueue.currentEpisode
        val episode: BaseEpisode? = when (currentUpNextEpisode) {
            is PodcastEpisode -> episodeManager.findByUuid(currentUpNextEpisode.uuid)

            is UserEpisode -> {
                userEpisodeManager.findEpisodeByUuidRx(currentUpNextEpisode.uuid)
                    .flatMap {
                        if (it.serverStatus == UserEpisodeServerStatus.MISSING) {
                            userEpisodeManager.downloadMissingUserEpisode(currentUpNextEpisode.uuid, placeholderTitle = currentUpNextEpisode.title, placeholderPublished = null)
                        } else {
                            Maybe.just(it)
                        }
                    }
                    .awaitSingleOrNull()
            }

            else -> null
        }

        if (episode == null) {
            val nextEpisode = autoLoadEpisode(autoPlay = play)
            if (nextEpisode == null) {
                Timber.d("Playback: No episode in upnext, shutting down")
                shutdown()
            }
            return
        }

        val podcast = findPodcastByEpisode(episode)

        cancelPauseTimer()
        cancelUpdateTimer()
        cancelBufferUpdateTimer()

        val currentPlayer = this.player
        val sameEpisode = currentPlayer != null && episode.uuid == currentPlayer.episodeUuid

        // completed episodes should play from the start
        if (episode.isFinished) {
            episodeManager.markAsNotPlayed(episode)
        }

        // make sure we have the latest episode url
        when (episode) {
            is PodcastEpisode -> {
                if (!episode.isDownloaded) {
                    val newDownloadUrl = episodeManager.updateDownloadUrl(episode)
                    if (newDownloadUrl != null && newDownloadUrl != episode.downloadUrl) {
                        Timber.i("Updating url for PodcastEpisode playback")
                        episode.downloadUrl = newDownloadUrl
                    }
                }
            }
            is UserEpisode -> {
                if (episode.serverStatus == UserEpisodeServerStatus.UPLOADED) {
                    try {
                        val newDownloadUrl = userEpisodeManager.getPlaybackUrl(episode).await()
                        episode.downloadUrl = newDownloadUrl
                    } catch (e: Exception) {
                        onPlayerError(PlayerEvent.PlayerError("Could not load cloud file ${e.message}"))
                        removeEpisode(episode, source = sourceView)
                        return
                    }
                }
            }
        }

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Opening episode. %s Downloaded: %b Downloading: %b Audio: %b File: %s Uuid: %s", episode.title, episode.isDownloaded, episode.isDownloading, episode.isAudio, episode.downloadUrl ?: "", episode.uuid)
        if (BuildConfig.DEBUG) {
            Thread.dumpStack()
        }

        episodeSubscription?.dispose()
        if (!episode.isDownloaded) {
            if (!Util.isCarUiMode(application) &&
                !Util.isWearOs(application) && // The watch handles these warnings before this is called
                settings.warnOnMeteredNetwork.value &&
                episode.uuid != lastWarnedPlayedEpisodeUuid &&
                !Network.isUnmeteredConnection(application) &&
                !forceStream &&
                play
            ) {
                sendDataWarningNotification(episode)
                val previousPlaybackState = playbackStateRelay.blockingFirst()
                val playbackState = PlaybackState(
                    state = PlaybackState.State.EMPTY, // Make sure an existing playback notification goes away
                    isBuffering = false,
                    isPrepared = true,
                    isSleepTimerRunning = previousPlaybackState?.isSleepTimerRunning ?: false,
                    title = episode.title,
                    durationMs = episode.durationMs,
                    positionMs = episode.playedUpToMs,
                    episodeUuid = episode.uuid,
                    podcast = podcast,
                    chapters = if (sameEpisode) (previousPlaybackState?.chapters ?: Chapters()) else Chapters(),
                    lastChangeFrom = LastChangeFrom.OnLoadCurrentEpisodeDataWarning.value,
                )
                withContext(Dispatchers.Main) {
                    playbackStateRelay.accept(playbackState)
                }
                return
            } else {
                val episodeObservable: Flowable<BaseEpisode>? = if (episode is PodcastEpisode) {
                    episodeManager.observeByUuid(episode.uuid)
                        .asFlowable()
                        .cast(BaseEpisode::class.java)
                } else if (episode is UserEpisode) {
                    userEpisodeManager.observeEpisodeRx(episode.uuid).cast(BaseEpisode::class.java)
                } else {
                    null
                }
                if (episodeObservable != null) {
                    episodeSubscription = episodeObservable
                        .takeUntil { it.isDownloaded }
                        .subscribeBy(
                            onNext = {
                                if (player?.isStreaming == true && it.isDownloaded && player?.isRemote == false) {
                                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Episode was streaming but was now downloaded, switching to downloaded file")

                                    launch(Dispatchers.Default) {
                                        player?.let { player ->
                                            val currentTimeSecs = player.getCurrentPositionMs().toDouble() / 1000.0
                                            episodeManager.updatePlayedUpTo(it, currentTimeSecs, true)
                                        }
                                        loadCurrentEpisode(isPlaying())
                                    }
                                } else {
                                    Timber.d("Episode is not downloaded $this")
                                }
                            },
                            onError = {
                                Timber.e(it, "Error observing episode")
                                crashLogging.sendReport(it)
                            },
                        )
                }
            }
        }

        // keep track of last played episode id so we don't keep bugging the user about streaming on data
        if (showedStreamWarning) {
            lastWarnedPlayedEpisodeUuid = episode.uuid
        }
        // keep track of the last played episode so we can auto select the next episode for Android Automotive
        lastPlayedEpisodeUuid = episode.uuid

        // podcast start from
        if (episode is PodcastEpisode) {
            // Auto subscribe to played podcasts (used in Automotive)
            if (podcast != null && settings.autoSubscribeToPlayed.value && !podcast.isSubscribed && episode.episodeType !is PodcastEpisode.EpisodeType.Trailer) {
                podcastManager.subscribeToPodcast(podcast.uuid, sync = true)
            }
        }

        var posUpdatedOnPlayerReset = false
        // We want to make sure we get the current position at the last possible moment before changing/resetting the player
        val currentPositionMs = if (
            isPlayerSwitchRequired() ||
            isPlayerResetNeeded(episode, sameEpisode, castManager.isConnected())
        ) {
            // Don't create a player if we aren't playing because it will start to buffer
            if (play) {
                Timber.d("Resetting player")
                val playerPositionMs = player?.getCurrentPositionMs()

                if (sameEpisode && playerPositionMs != null) {
                    val playerPositionSeconds = playerPositionMs / 1000.0
                    // Make sure that the episode is updated with the latest position before resetting the player.
                    // This helps avoid having the audio jump "back" a second or so when the currently playing episode
                    // is downloaded.
                    episodeManager.updatePlayedUpTo(episode, playerPositionSeconds, forceUpdate = true)
                    posUpdatedOnPlayerReset = true
                }

                resetPlayer()
                playerPositionMs
            } else {
                Timber.d("Stopping player")
                val playerPositionMs = player?.getCurrentPositionMs()
                stopPlayer()
                playerPositionMs
            }
        } else {
            Timber.d("Player reset not required")
            player?.getCurrentPositionMs()
        }

        player?.setPodcast(podcast)
        player?.setEpisode(episode)

        val playbackEffects = if (podcast != null && podcast.overrideGlobalEffects) {
            podcast.playbackEffects
        } else {
            settings.globalPlaybackEffects.value
        }

        val previousPlaybackState = playbackStateRelay.blockingFirst()
        val playbackState = PlaybackState(
            state = if (play) PlaybackState.State.PLAYING else PlaybackState.State.PAUSED,
            isBuffering = !episode.isDownloaded && play,
            isPrepared = true,
            isSleepTimerRunning = previousPlaybackState?.isSleepTimerRunning ?: false,
            title = episode.title,
            durationMs = episode.durationMs,
            positionMs = episode.playedUpToMs,
            episodeUuid = episode.uuid,
            podcast = podcast,
            chapters = if (sameEpisode) (previousPlaybackState?.chapters ?: Chapters()) else Chapters(),
            playbackSpeed = playbackEffects.playbackSpeed,
            trimMode = playbackEffects.trimMode,
            isVolumeBoosted = playbackEffects.isVolumeBoosted,
            lastChangeFrom = LastChangeFrom.OnLoadCurrentEpisode.value,
        )
        withContext(Dispatchers.Main) {
            playbackStateRelay.accept(playbackState)
        }

        if (episode is PodcastEpisode) {
            // Update the episode image
            launch {
                showNotesManager.loadShowNotes(
                    podcastUuid = episode.podcastUuid,
                    episodeUuid = episode.uuid,
                )
            }
        }

        // audio effects
        player?.setPlaybackEffects(playbackEffects)

        episodeManager.updatePlaybackInteractionDate(episode)

        widgetManager.updateWidget(podcast, play, episode)

        if (play) {
            if (sameEpisode && currentPositionMs != null) {
                player?.seekToTimeMs(currentPositionMs)
            }
            play(sourceView, posUpdatedOnPlayerReset)
        } else {
            player?.load(episode.playedUpToMs)
            onPlayerPaused()
        }
    }

    private fun findPodcastByEpisode(episode: BaseEpisode): Podcast? {
        return when (episode) {
            is PodcastEpisode -> podcastManager.findPodcastByUuid(episode.podcastUuid)
            is UserEpisode -> podcastManager.buildUserEpisodePodcast(episode)
            else -> null
        }
    }

    @Suppress("DEPRECATION")
    private fun sendDataWarningNotification(episode: BaseEpisode) {
        val manager = NotificationManagerCompat.from(application)

        val intent = application.packageManager.getLaunchIntentForPackage(application.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(application, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))

        val notificationTag = NotificationBroadcastReceiver.NOTIFICATION_TAG_PLAYBACK_ERROR

        val skipAction = if (upNextQueue.queueEpisodes.count { it.isDownloaded } > 0) {
            val skipIntent = buildNotificationIntent(1, NotificationBroadcastReceiver.INTENT_ACTION_PLAY_DOWNLOADED, episode, notificationTag, application)
            NotificationCompat.Action(com.google.android.gms.cast.framework.R.drawable.cast_ic_mini_controller_skip_next, "Play next downloaded", skipIntent)
        } else {
            null
        }

        val streamIntent = buildNotificationIntent(
            intentId = 2,
            intentName = NotificationBroadcastReceiver.INTENT_ACTION_STREAM_EPISODE_FROM_STREAM_WARNING,
            episode = episode,
            notificationTag = notificationTag,
            context = application,
        )
        val streamAction = NotificationCompat.Action(IR.drawable.notification_action_play, "Yes, keep playing", streamIntent)

        val color = ContextCompat.getColor(application, R.color.notification_color)

        var builder = notificationHelper.playbackErrorChannelBuilder()
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentTitle(episode.title)
            .setContentText("This episode is not downloaded, do you want to stream it?")
            .setSmallIcon(IR.drawable.notification)
            .setAutoCancel(true)
            .setColor(color)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(streamAction)

        if (skipAction != null) {
            builder = builder.addAction(skipAction)
        }

        val notification = builder.build()

        // Add sound and vibrations
        val sound = settings.notificationSound.value.uri
        if (sound != null) {
            notification.sound = sound
        }

        try {
            notificationPermissionChecker?.let {
                Timber.i("Checking notification permission")
                it.checkNotificationPermission {
                    manager.notify(notificationTag, NotificationBroadcastReceiver.NOTIFICATION_ID, notification)
                }
            } ?: run {
                val message = "notificationPermissionChecker was null (this should never happen)"
                LogBuffer.e(LogBuffer.TAG_PLAYBACK, message)
                crashLogging.recordEvent(message)
                manager.notify(notificationTag, NotificationBroadcastReceiver.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            val message = "Could not post notification ${e.message}"
            LogBuffer.e(LogBuffer.TAG_PLAYBACK, message)
            crashLogging.sendReport(e, message = message)
        }
    }

    private fun buildNotificationIntent(intentId: Int, intentName: String, episode: BaseEpisode, notificationTag: String, context: Context): PendingIntent {
        val intent = Intent(context, NotificationBroadcastReceiver::class.java)
        intent.action = (System.currentTimeMillis() + intentId).toString()
        intent.putExtra(NotificationBroadcastReceiver.INTENT_EXTRA_ACTION, intentName)
        intent.putExtra(NotificationBroadcastReceiver.INTENT_EXTRA_EPISODE_UUID, episode.uuid)
        intent.putExtra(NotificationBroadcastReceiver.INTENT_EXTRA_NOTIFICATION_TAG, notificationTag)
        return PendingIntent.getBroadcast(context, intentId, intent, PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
    }

    private suspend fun play(
        sourceView: SourceView = SourceView.UNKNOWN,
        posUpdatedOnPlayerReset: Boolean = false,
    ) {
        val episode = getCurrentEpisode()?.let {
            if (posUpdatedOnPlayerReset) {
                // Retrieves latest episode instance to address potential audio skip back issue (PR#1510)
                episodeManager.findEpisodeByUuid(it.uuid) ?: it
            } else {
                it
            }
        }
        if (episode == null || player == null) {
            return
        }

        val hasAudioFocus = focusManager.tryToGetAudioFocus()
        if (!hasAudioFocus) {
            return
        }

        cancelPauseTimer()
        setupBufferUpdateTimer(episode)

        // clear the playback errors
        if (episode.playErrorDetails != null) {
            episodeManager.clearPlaybackError(episode)
        }

        audioNoisyManager.register(this)

        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let { playbackState ->
                playbackStateRelay.accept(playbackState.copy(state = PlaybackState.State.PLAYING, lastChangeFrom = LastChangeFrom.OnPlay.value))

                if (player?.episodeUuid != playbackState.episodeUuid) {
                    LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Player playing episode that is not the same as playback state. Player: ${player?.episodeUuid} State: ${playbackState.episodeUuid}")
                }

                // Handle skip first
                if (episode is PodcastEpisode) {
                    addPodcastStartFromSettings(episode, playbackState.podcast, isPlaying = true)
                }
            }
        }

        val currentTimeMs = resumptionHelper.adjustedStartTimeMsFor(episode)
        LogBuffer.i(
            LogBuffer.TAG_PLAYBACK, "Play %.3f %s Player. %s Downloaded: %b, Downloading: %b, Audio: %b, File: %s, EpisodeUuid: %s, PodcastUuid: %s",
            currentTimeMs / 1000f,
            player?.name ?: "_",
            episode.title,
            episode.isDownloaded,
            episode.isDownloading,
            episode.isAudio,
            episode.downloadUrl ?: "_",
            episode.uuid,
            (episode as? PodcastEpisode)?.podcastUuid ?: "User File",
        )

        player?.play(currentTimeMs)

        sleepTimer.restartSleepTimerIfApplies(
            currentEpisodeUuid = episode.uuid,
            timerState = SleepTimer.SleepTimerState(
                isSleepTimerRunning = playbackStateRelay.blockingFirst().isSleepTimerRunning,
                isSleepEndOfEpisodeRunning = isSleepAfterEpisodeEnabled(),
                isSleepEndOfChapterRunning = isSleepAfterChapterEnabled(),
                numberOfEpisodes = settings.getlastSleepEndOfEpisodes(),
                numberOfChapters = settings.getlastSleepEndOfChapter(),
            ),
            onRestartSleepAfterTime = {
                updateSleepTimerStatus(sleepTimeRunning = true)
            },
            onRestartSleepOnEpisodeEnd = {
                updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterEpisodes = settings.getlastSleepEndOfEpisodes())
            },
            onRestartSleepOnChapterEnd = {
                updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterChapters = settings.getlastSleepEndOfChapter())
            },
        )

        trackPlayback(AnalyticsEvent.PLAYBACK_PLAY, sourceView)
    }

    private suspend fun addPodcastStartFromSettings(episode: PodcastEpisode, podcast: Podcast?, isPlaying: Boolean) {
        if (episode.playedUpTo != 0.toDouble() ||
            episode.playingStatus == EpisodePlayingStatus.IN_PROGRESS ||
            podcast == null ||
            podcast.startFromSecs <= 0
        ) {
            return
        }

        val startFromMs = podcast.startFromSecs * 1000
        episode.playedUpToMs = startFromMs
        statsManager.addTimeSavedAutoSkipping(startFromMs.toLong())
        if (isPlaying) {
            showToast(application.getString(LR.string.player_started_from, podcast.startFromSecs))
        }
    }

    private suspend fun resetPlayer() {
        if (resettingPlayer) return
        resettingPlayer = true

        withContext(Dispatchers.Main) {
            player?.stop()
            if (castManager.isConnected()) {
                player = playerManager.createCastPlayer(this@PlaybackManager::onPlayerEvent)
                Timber.i("Creating media player of type CastPlayer.")
            } else {
                player = playerManager.createSimplePlayer(this@PlaybackManager::onPlayerEvent)
                Timber.i("Creating media player of type SimplePlayer.")
            }
        }

        resettingPlayer = false
    }

    private suspend fun stopPlayer() {
        withContext(Dispatchers.Main) {
            player?.stop()
        }
    }

    private fun onPlayerEvent(player: Player, event: PlayerEvent) {
        if (this.player != player) return

        launch {
            Timber.d("Player %s event %s", player, event)
            when (event) {
                is PlayerEvent.Completion -> onCompletion(event.episodeUUID)
                is PlayerEvent.PlayerPaused -> onPlayerPaused()
                is PlayerEvent.PlayerPlaying -> onPlayerPlaying()
                is PlayerEvent.BufferingStateChanged -> onBufferingStateChanged()
                is PlayerEvent.DurationAvailable -> onDurationAvailable()
                is PlayerEvent.SeekComplete -> onSeekComplete(event.positionMs)
                is PlayerEvent.MetadataAvailable -> onMetadataAvailable(event.metaData)
                is PlayerEvent.PlayerError -> onPlayerError(event)
                is PlayerEvent.RemoteMetadataNotMatched -> onRemoteMetaDataNotMatched(event.remoteEpisodeUuid)
                is PlayerEvent.EpisodeChanged -> onEpisodeChanged(event.episodeUuid)
            }
        }
    }

    private suspend fun updateCurrentPositionInDatabase() {
        updateCount = 0

        val episode = getCurrentEpisode() ?: return
        val currentTimeMs = getCurrentTimeMs(episode = episode)

        if (currentTimeMs < 0) {
            return
        }

        val currentTimeSecs = currentTimeMs.toDouble() / 1000.0
        episodeManager.updatePlayedUpTo(episode, currentTimeSecs, false)
        episodeManager.updatePlaybackInteractionDate(episode)
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Saved time in database %.3f", currentTimeSecs)

        statsManager.persistTimes()
    }

    private fun updateCurrentPositionRx(): Completable {
        return rxCompletable {
            updateCurrentPosition()
        }
    }

    private suspend fun updateCurrentPosition() {
        val episode = getCurrentEpisode() ?: return
        if (episode.uuid != playbackStateRelay.blockingFirst().episodeUuid) {
            Timber.d("Timer fired after onCompletion, ignoring")
            return
        }

        val skipLast = playbackStateRelay.blockingFirst().podcast?.skipLastSecs

        val positionMs = player?.getCurrentPositionMs() ?: -1
        if (positionMs < 0) {
            return
        }

        val durationMs = player?.durationMs()
        if (skipLast.isPositive() && durationMs.isPositive() && durationMs > skipLast) {
            val timeRemaining = (durationMs - positionMs) / 1000
            if (timeRemaining < skipLast) {
                if (isSleepAfterEpisodeEnabled()) {
                    sleepEndOfEpisode(episode)
                } else {
                    statsManager.addTimeSavedAutoSkipping(timeRemaining.toLong() * 1000L)
                    episodeManager.markAsPlayed(episode, this, podcastManager)
                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Skipping remainder of ${episode.title} with skip last $skipLast")
                    showToast(application.getString(LR.string.player_skipped_last, skipLast))
                }
                return
            }
        }

        episode.playedUpToMs = positionMs

        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let { playbackState ->
                if (positionMs != playbackState.positionMs) {
                    Timber.d("Update current position of %s to %d", episode.title, positionMs)
                    playbackStateRelay.accept(playbackState.copy(positionMs = positionMs, lastChangeFrom = LastChangeFrom.OnUpdateCurrentPosition.value))
                }
            }
        }

        player?.let { player ->
            if (!player.isBuffering()) {
                updateCount++
                if (updateCount >= UPDATE_EVERY) {
                    updateCurrentPositionInDatabase()
                }
            }
        }
    }

    private suspend fun updateBufferPosition() {
        val episode = getCurrentEpisode()
        val player = player
        if (episode == null || player == null || !player.isStreaming) {
            return
        }
        val bufferedUpToMs = getBufferedUpToMs()
        if (bufferedUpToMs == lastBufferedUpTo) {
            return
        }
        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let { playbackState ->
                playbackStateRelay.accept(playbackState.copy(bufferedMs = bufferedUpToMs, lastChangeFrom = LastChangeFrom.OnUpdateBufferPosition.value))
            }
        }
        lastBufferedUpTo = bufferedUpToMs
    }

    private fun setupUpdateTimer() {
        setupProgressSync()

        updateTimerDisposable?.dispose()
        updateTimerDisposable = Observable.interval(UPDATE_TIMER_POLL_TIME, UPDATE_TIMER_POLL_TIME, TimeUnit.MILLISECONDS, Schedulers.io())
            .doOnNext {
                if (isPlaying()) {
                    statsManager.addTotalListeningTime(UPDATE_TIMER_POLL_TIME)
                }
                if (playbackStateRelay.blockingFirst().isSleepTimerRunning && !isSleepAfterEpisodeEnabled()) { // Does not apply to end of episode sleep time
                    setupFadeOutWhenFinishingSleepTimer()
                }
                verifySleepTimeForEndOfChapter()
            }
            .switchMapCompletable { updateCurrentPositionRx() }
            .subscribeBy(onError = { Timber.e(it) })
    }

    private fun verifySleepTimeForEndOfChapter() {
        val playbackState = playbackStateRelay.blockingFirst()
        val currentChapterUuid = getCurrentChapterUuidForSleepTime(playbackState)
        val currentEpisodeUui = getCurrentEpisode()?.uuid

        if (!isSleepAfterChapterEnabled()) {
            lastListenedForEndOfChapter.setlastListenedChapterUuid(null)
            lastListenedForEndOfChapter.setlastListenedEpisodeUuid(null)
            return
        }

        if (lastListenedForEndOfChapter.getlastListenedChapterUuid().isNullOrEmpty()) {
            lastListenedForEndOfChapter.setlastListenedChapterUuid(currentChapterUuid)
        }

        if (lastListenedForEndOfChapter.getlastListenedEpisodeUuid().isNullOrEmpty()) {
            lastListenedForEndOfChapter.setlastListenedEpisodeUuid(currentEpisodeUui)
        }

        // When we switch from a episode that contains chapters to another one that does not have chapters
        // the current chapter is null, so for this case we would need to verify if the episode changed to update the sleep timer counter for end of chapter
        if (currentChapterUuid == null && !lastListenedForEndOfChapter.getlastListenedEpisodeUuid().isNullOrEmpty() && lastListenedForEndOfChapter.getlastListenedEpisodeUuid() != currentEpisodeUui) {
            applicationScope.launch {
                lastListenedForEndOfChapter.setlastListenedChapterUuid(null)
                lastListenedForEndOfChapter.setlastListenedEpisodeUuid(currentEpisodeUui)
                sleepEndOfChapter()
            }
        } else if (lastListenedForEndOfChapter.getlastListenedChapterUuid() == currentChapterUuid) { // Same chapter
            return
        } else { // Changed chapter
            applicationScope.launch {
                lastListenedForEndOfChapter.setlastListenedChapterUuid(currentChapterUuid)
                lastListenedForEndOfChapter.setlastListenedEpisodeUuid(getCurrentEpisode()?.uuid)
                sleepEndOfChapter()
            }
        }
    }

    private fun setupFadeOutWhenFinishingSleepTimer() {
        // it needs to run in the main thread because of player getVolume
        applicationScope.launch(Dispatchers.Main) {
            val timeLeft = sleepTimer.timeLeftInSecs()
            timeLeft?.let {
                val fadeDuration = 5
                val startVolume = (player as? SimplePlayer)?.getVolume() ?: 1.0f

                if (timeLeft <= fadeDuration) {
                    val fraction = timeLeft.toFloat() / fadeDuration
                    val newVolume = startVolume * fraction
                    player?.setVolume(newVolume)
                } else {
                    player?.setVolume(startVolume)
                }
            }
        }
    }

    private fun cancelUpdateTimer() {
        updateTimerDisposable?.dispose()
        syncTimerDisposable?.dispose()
        episodeSubscription?.dispose()
    }

    private fun setupBufferUpdateTimer(episode: BaseEpisode) {
        val player = player
        if (player == null || !player.isStreaming || player.isRemote || episode.isDownloading) {
            return
        }
        lastBufferedUpTo = -1
        bufferUpdateTimerDisposable?.dispose()
        bufferUpdateTimerDisposable = Observable.interval(UPDATE_TIMER_POLL_TIME, UPDATE_TIMER_POLL_TIME, TimeUnit.MILLISECONDS, Schedulers.io())
            .switchMapCompletable {
                rxCompletable {
                    launch {
                        updateBufferPosition()
                    }
                }
            }
            .subscribeBy(onError = { Timber.e(it) })
    }

    private fun cancelBufferUpdateTimer() {
        bufferUpdateTimerDisposable?.dispose()
    }

    /**
     * After the episode is paused wait and then stop the player.
     */
    private fun setupPauseTimer() {
        pauseTimerDisposable?.dispose()
        if (player != null && !isPlaybackRemote()) {
            pauseTimerDisposable = Observable.interval(PAUSE_TIMER_DELAY, TimeUnit.MILLISECONDS, Schedulers.io())
                .firstOrError()
                .flatMapCompletable {
                    rxCompletable {
                        if (!playbackStateRelay.blockingFirst().isPlaying) {
                            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Hibernating playback from Pause timer.")
                            hibernatePlayback()
                        }
                    }
                }
                .subscribeBy(onError = { Timber.e(it) })
        }
    }

    private fun cancelPauseTimer() {
        pauseTimerDisposable?.dispose()
    }

    suspend fun preparePlayer() {
        val episode = upNextQueue.currentEpisode ?: return
        val currentPlayer = this.player
        if (currentPlayer == null || episode.uuid != currentPlayer.episodeUuid) {
            loadCurrentEpisode(false)
        }
    }

    fun loadQueue(): Job {
        return launch {
            val episode = upNextQueue.currentEpisode ?: return@launch
            val currentPlayer = this@PlaybackManager.player
            if (currentPlayer == null) {
                withContext(Dispatchers.Main) {
                    updatePausedPlaybackState()
                }
            } else if (episode.uuid != currentPlayer.episodeUuid) {
                loadCurrentEpisode(false)
            }
        }
    }

    fun loadQueueRx(): Completable {
        return rxCompletable { loadQueue() }
    }

    private fun updatePausedPlaybackState() {
        val previousPlaybackState = playbackStateRelay.blockingFirst()
        if (previousPlaybackState != null && previousPlaybackState.isPlaying) {
            return
        }

        Timber.d("updatePausedPlaybackState")
        val upNextState = upNextQueue.changesObservable.blockingFirst()
        if (upNextState is UpNextQueue.State.Loaded) {
            val episode = upNextState.episode
            val podcast = upNextState.podcast
            val sameEpisode = previousPlaybackState != null && episode.uuid == previousPlaybackState.episodeUuid
            val playbackState = PlaybackState(
                state = PlaybackState.State.PAUSED,
                isSleepTimerRunning = previousPlaybackState?.isSleepTimerRunning ?: false,
                title = episode.title,
                durationMs = episode.durationMs,
                positionMs = episode.playedUpToMs,
                episodeUuid = episode.uuid,
                podcast = podcast,
                chapters = if (sameEpisode) (previousPlaybackState?.chapters ?: Chapters()) else Chapters(),
                lastChangeFrom = LastChangeFrom.OnUpdatePausedPlaybackState.value,
            )
            playbackStateRelay.accept(playbackState)
        }
    }

    fun playBookmarkTone() {
        try {
            bookmarkTonePlayer.start()
        } catch (e: Exception) {
            Timber.e("Unable to play tone: $e")
        }
    }

    fun playSleepTimeTone() {
        try {
            sleepTimeTonePlayer.start()
        } catch (e: Exception) {
            Timber.e("Unable to play tone: $e")
        }
    }

    private fun trackPlayback(event: AnalyticsEvent, sourceView: SourceView) {
        if (sourceView == SourceView.UNKNOWN) {
            Timber.w("Found unknown playback source.")
        }
        if (!sourceView.skipTracking()) {
            val contentType = if (getCurrentEpisode()?.isVideo == true) ContentType.VIDEO else ContentType.AUDIO
            analyticsTracker.track(
                event,
                mapOf(
                    SOURCE_KEY to sourceView.analyticsValue,
                    CONTENT_TYPE_KEY to contentType.analyticsValue,
                ),
            )
        }
    }

    fun trackPlaybackSeek(
        positionMs: Int,
        sourceView: SourceView,
    ) {
        val episode = getCurrentEpisode()
        episode?.let {
            val fromPositionMs = episode.playedUpToMs.toDouble()
            val durationMs = episode.duration * 1000
            val seekFromPercent = ((fromPositionMs / durationMs) * 100).toInt()
            val seekToPercent = ((positionMs / durationMs) * 100).toInt()

            analyticsTracker.track(
                AnalyticsEvent.PLAYBACK_SEEK,
                mapOf(
                    SOURCE_KEY to sourceView.analyticsValue,
                    SEEK_FROM_PERCENT_KEY to seekFromPercent,
                    SEEK_TO_PERCENT_KEY to seekToPercent,
                ),
            )
        }
    }

    fun trackPlaybackEffectsEvent(
        event: AnalyticsEvent,
        props: Map<String, Any> = emptyMap(),
        sourceView: SourceView,
    ) {
        val properties = HashMap<String, Any>()
        properties[SOURCE_KEY] = sourceView.analyticsValue
        properties.putAll(props)
        analyticsTracker.track(event, properties)
    }

    fun setNotificationPermissionChecker(notificationPermissionChecker: NotificationPermissionChecker) {
        this.notificationPermissionChecker = notificationPermissionChecker
    }

    fun isSleepAfterEpisodeEnabled(): Boolean = episodesUntilSleep != 0

    fun isSleepAfterChapterEnabled(): Boolean = chaptersUntilSleep != 0

    private fun getCurrentChapterUuidForSleepTime(playbackState: PlaybackState): String? {
        val currentChapter = playbackState.chapters.getChapter(playbackState.positionMs.milliseconds)

        return currentChapter?.let { it.title + it.startTime }
    }

    private fun MutableMap<String, String?>.getlastListenedEpisodeUuid(): String? {
        return this["episodeUuid"]
    }

    private fun MutableMap<String, String?>.getlastListenedChapterUuid(): String? {
        return this["chapterUuid"]
    }

    private fun MutableMap<String, String?>.setlastListenedEpisodeUuid(value: String?) {
        this["episodeUuid"] = value
    }

    private fun MutableMap<String, String?>.setlastListenedChapterUuid(value: String?) {
        this["chapterUuid"] = value
    }

    private enum class ContentType(val analyticsValue: String) {
        AUDIO("audio"),
        VIDEO("video"),
    }

    enum class LastChangeFrom(val value: String) {
        OnInit("Init"),
        OnBufferingStateChanged("onBufferingStateChanged"),
        OnChapterSelectionToggled("onChapterSelectionToggled"),
        OnChapterIndicesUpdated("onChapterIndicesUpdated"),
        OnCompletion("onCompletion"),
        OnDurationAvailable("onDurationAvailable"),
        OnEffectsChanged("effectsChanged"),
        OnPlay("play"),
        OnPlayerError("onPlayerError"),
        OnPlayerPaused("onPlayerPaused"),
        OnPlayerPlaying("onPlayerPlaying"),
        OnMarkPodcastNeedsUpdating("markPodcastNeedsUpdating"),
        OnMetadataAvailable("onMetadataAvailable"),
        OnLoadCurrentEpisode("loadCurrentEpisode"),
        OnLoadCurrentEpisodeDataWarning("loadCurrentEpisode data warning"),
        OnSeekComplete("onSeekComplete"),
        OnShutdown("shutdown"),
        OnStop("stop"),
        OnUpdateBufferPosition("updateBufferPosition"),
        OnUpdateCurrentPosition("updateCurrentPosition"),
        OnUpdatePausedPlaybackState("updatePausedPlaybackState"),
        OnUpdateSleepTimerStatus("updateSleepTimerStatus"),
        OnUserSeeking("onUserSeeking"),
    }
}
