package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.toLiveData
import androidx.media3.datasource.HttpDataSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.R
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper.removeEpisodeFromQueue
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.LocalPlayer.Companion.VOLUME_DUCK
import au.com.shiftyjelly.pocketcasts.repositories.playback.LocalPlayer.Companion.VOLUME_NORMAL
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.toServerPostFile
import au.com.shiftyjelly.pocketcasts.repositories.sync.NotificationBroadcastReceiver
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.widget.WidgetManager
import au.com.shiftyjelly.pocketcasts.servers.sync.EpisodeSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.EpisodeSyncResponse
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.isPositive
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitSingleOrNull
import kotlinx.coroutines.rx2.rxCompletable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
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
) : FocusManager.FocusChangeListener, AudioNoisyManager.AudioBecomingNoisyListener, CoroutineScope {

    companion object {
        private const val UPDATE_EVERY = 10
        private const val UPDATE_TIMER_POLL_TIME: Long = 1000
        private const val MAX_TIME_WITHOUT_FOCUS_FOR_RESUME_MINUTES = 30
        private const val MAX_TIME_WITHOUT_FOCUS_FOR_RESUME = (MAX_TIME_WITHOUT_FOCUS_FOR_RESUME_MINUTES * 60 * 1000).toLong()
        private const val PAUSE_TIMER_DELAY = ((MAX_TIME_WITHOUT_FOCUS_FOR_RESUME_MINUTES + 1) * 60 * 1000).toLong()
        private const val SOURCE_KEY = "source"
        private const val SEEK_TO_PERCENT_KEY = "seek_to_percent"
        private const val SEEK_FROM_PERCENT_KEY = "seek_from_percent"
        const val SPEED_KEY = "speed"
        const val AMOUNT_KEY = "amount"
        const val ENABLED_KEY = "enabled"
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val focusManager: FocusManager by lazy {
        FocusManager(settings, application).apply {
            focusChangeListener = this@PlaybackManager
        }
    }
    private var audioNoisyManager =
        AudioNoisyManager(application)

    val playbackStateRelay: Relay<PlaybackState> by lazy {
        val relay = BehaviorRelay.create<PlaybackState>().toSerialized()
        relay.accept(PlaybackState(lastChangeFrom = "Init"))
        Log.d(Settings.LOG_TAG_AUTO, "Init playback state")
        return@lazy relay
    }
    val playbackStateLive = playbackStateRelay.toFlowable(BackpressureStrategy.LATEST).toLiveData()

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
    var lastLoadedFromPodcastOrPlaylistUuid: String? = null

    private val resumptionHelper = ResumptionHelper(settings)

    var episodeSubscription: Disposable? = null

    val mediaSessionManager = MediaSessionManager(
        playbackManager = this,
        podcastManager = podcastManager,
        episodeManager = episodeManager,
        playlistManager = playlistManager,
        settings = settings,
        context = application,
        episodeAnalytics = episodeAnalytics
    )
    var sleepAfterEpisode: Boolean = false

    var pocketCastsPlayer: PocketCastsPlayer? = null

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
        }
    }

    fun getCurrentEpisode(): Playable? {
        return upNextQueue.currentEpisode
    }

    fun getCurrentPodcast(): Podcast? {
        val currentEpisode = getCurrentEpisode() as? Episode ?: return null
        return currentEpisode.podcastUuid.let { podcastManager.findPodcastByUuid(it) }
    }

    private suspend fun autoLoadEpisode(autoPlay: Boolean): Playable? {
        val nextEpisode = getCurrentEpisode()
        if (nextEpisode != null) {
            return nextEpisode
        }

        if (!settings.getAutoPlayNextEpisodeOnEmpty()) {
            return null
        }

        // auto queue next episode on empty
        val autoPlayEpisode = autoSelectNextEpisode() ?: return null

        withContext(Dispatchers.Default) {
            upNextQueue.playNext(autoPlayEpisode, downloadManager) {
                launch {
                    loadCurrentEpisode(play = autoPlay, playbackSource = AnalyticsSource.AUTO_PLAY)
                }
            }
        }

        return autoPlayEpisode
    }

    fun isPlaying(): Boolean {
        return playbackStateRelay.blockingFirst().isPlaying
    }

    fun isStreaming(): Boolean {
        return pocketCastsPlayer?.isStreaming ?: false
    }

    fun updateSleepTimerStatus(running: Boolean, sleepAfterEpisode: Boolean = false) {
        this.sleepAfterEpisode = sleepAfterEpisode
        playbackStateRelay.blockingFirst().let {
            playbackStateRelay.accept(it.copy(isSleepTimerRunning = running, lastChangeFrom = "updateSleepTimerStatus"))
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
                    Observable.empty<EpisodeSyncResponse>()
                }
            }
            .doOnError { LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Could not sync episode progress. ${it.javaClass.name} ${it.message ?: ""}") }
            .onErrorReturnItem(EpisodeSyncResponse())
            .subscribeBy(
                onNext = {
                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Synced episode progress")
                }
            )
    }

    private fun syncEpisodeProgress(playbackState: PlaybackState): Completable {
        val episode = getCurrentEpisode()
        if (playbackState.episodeUuid != episode?.uuid) {
            return Completable.complete()
        }

        return if (episode is Episode) {
            val request = EpisodeSyncRequest(
                episode.uuid,
                episode.podcastUuid,
                playbackState.positionMs / 1000L,
                playbackState.durationMs / 1000L,
                EpisodeSyncRequest.STATUS_IN_PROGRESS
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

    private suspend fun getCurrentTimeMs(episode: Playable): Int {
        val player = pocketCastsPlayer
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
        return pocketCastsPlayer?.bufferedUpToMs() ?: 0
    }

    fun isPlaybackLocal(): Boolean {
        return !isPlaybackRemote()
    }

    fun isPlaybackRemote(): Boolean {
        return pocketCastsPlayer?.isRemote ?: false
    }

    fun isAudioEffectsAvailable(): Boolean {
        val episode = getCurrentEpisode()
        return episode != null
    }

    fun isTrimSilenceSupported(): Boolean {
        return pocketCastsPlayer?.supportsTrimSilence() ?: false
    }

    fun isVolumeBoostSupported(): Boolean {
        return pocketCastsPlayer?.supportsVolumeBoost() ?: false
    }

    fun shouldWarnAboutPlayback(episodeUUID: String? = upNextQueue.currentEpisode?.uuid): Boolean {
        return settings.warnOnMeteredNetwork() && !Network.isUnmeteredConnection(application) && lastWarnedPlayedEpisodeUuid != episodeUUID
    }

    fun getPlaybackSpeed(): Double {
        return playbackStateRelay.blockingFirst().playbackSpeed
    }

    fun playPause(playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN) {
        if (isPlaying()) {
            pause(playbackSource = playbackSource)
        } else {
            playQueue(playbackSource)
        }
    }

    fun playQueue(playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN) {
        launch {
            if (upNextQueue.currentEpisode != null) {
                loadEpisodeWhenRequired(playbackSource)
            }
        }
    }

    fun playNow(episode: Playable, forceStream: Boolean = false, playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN) {
        launch {
            forcePlayerSwitch = true
            playNowSync(episode = episode, forceStream = forceStream, playbackSource = playbackSource)
        }
    }

    suspend fun playNowSync(episode: Playable, forceStream: Boolean = false, playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN) {
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
            upNextQueue.playNow(episode) {
                launch {
                    loadCurrentEpisode(play = true, forceStream = forceStream, playbackSource = playbackSource)
                }
            }

            if (episode is Episode) {
                // We only want to track playing of episodes, not files
                FirebaseAnalyticsTracker.playedEpisode()
            }
        } else if (!switchEpisode && playbackStateRelay.blockingFirst().isPaused) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "No player switch required. Playing queue.")
            playQueue(playbackSource)
        }
    }

    suspend fun play(
        upNextPosition: UpNextPosition,
        episode: Playable,
        source: AnalyticsSource,
        userInitiated: Boolean = true
    ) {
        when (upNextPosition) {
            UpNextPosition.NEXT -> playNext(episode, source, userInitiated)
            UpNextPosition.LAST -> playLast(episode, source, userInitiated)
        }
    }

    suspend fun playNext(
        episode: Playable,
        source: AnalyticsSource,
        userInitiated: Boolean = true
    ) = withContext(Dispatchers.Default) {
        val wasEmpty: Boolean = upNextQueue.isEmpty
        upNextQueue.playNext(episode, downloadManager, null)
        if (userInitiated) {
            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_ADDED_TO_UP_NEXT, source, true)
        }
        if (wasEmpty) {
            loadCurrentEpisode(play = false)
        }
    }

    suspend fun playLast(
        episode: Playable,
        source: AnalyticsSource,
        userInitiated: Boolean = true
    ) {
        val wasEmpty: Boolean = upNextQueue.isEmpty
        upNextQueue.playLast(episode, downloadManager, null)
        if (userInitiated) {
            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_ADDED_TO_UP_NEXT, source, false)
        }
        if (wasEmpty) {
            loadCurrentEpisode(play = false)
        }
    }

    private suspend fun loadEpisodeWhenRequired(playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN) {
        if (isPlayerSwitchRequired()) {
            loadCurrentEpisode(play = true, playbackSource = playbackSource)
        } else {
            play(playbackSource)
        }
    }

    fun playEpisodes(episodes: List<Playable>, playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN) {
        if (episodes.isEmpty()) {
            return
        }

        launch {
            val topEpisode = episodes.first()
            playNowSync(episode = topEpisode, playbackSource = playbackSource)
            if (episodes.size > 1) {
                upNextQueue.clearAndPlayAll(episodes.slice(1 until min(episodes.size, settings.getMaxUpNextEpisodes())), downloadManager)
            }
        }
    }

    fun playEpisodesLast(episodes: List<Playable>, source: AnalyticsSource) {
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
                source = source
            )
            if (wasEmpty) {
                loadCurrentEpisode(play = false)
            }
        }
    }

    fun playEpisodesNext(episodes: List<Playable>, source: AnalyticsSource) {
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
                source = source
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

    fun changeUpNext(episodes: List<Playable>) {
        launch {
            upNextQueue.changeList(episodes)
        }
    }

    fun pause(transientLoss: Boolean = false, playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN) {
        if (!transientLoss) {
            focusManager.giveUpAudioFocus()
            playbackStateRelay.blockingFirst().let { playbackState ->
                playbackStateRelay.accept(playbackState.copy(transientLoss = false))
            }
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Paused - Not transient")
            trackPlayback(AnalyticsEvent.PLAYBACK_PAUSE, playbackSource)
        } else {
            playbackStateRelay.blockingFirst().let { playbackState ->
                playbackStateRelay.accept(playbackState.copy(transientLoss = true))
            }
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Paused - Transient")
        }

        cancelUpdateTimer()

        launch {
            pocketCastsPlayer?.pause()
        }
    }

    fun stopAsync(isAudioFocusFailed: Boolean = false, playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN) {
        launch {
            if (!isAudioFocusFailed) {
                trackPlayback(AnalyticsEvent.PLAYBACK_STOP, playbackSource)
            }
            stop()
        }
    }

    suspend fun stop() {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Stopping playback")

        cancelUpdateTimer()
        cancelBufferUpdateTimer()

        withContext(Dispatchers.Main) {
            if (pocketCastsPlayer != null) {
                pocketCastsPlayer?.stop()
                pocketCastsPlayer = null
            }
        }

        playbackStateRelay.blockingFirst().let {
            playbackStateRelay.accept(
                it.copy(
                    state = PlaybackState.State.STOPPED,
                    isPrepared = false,
                    lastChangeFrom = "stop"
                )
            )
        }
    }

    suspend fun shutdown() {
        stop()

        audioNoisyManager.unregister()
        focusManager.giveUpAudioFocus()

        withContext(Dispatchers.Main) {
            playbackStateRelay.accept(PlaybackState(state = PlaybackState.State.EMPTY, lastChangeFrom = "shutdown"))
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
            seekToTimeMsInternal(positionMs)
            seekComplete?.invoke()
        }
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

    private suspend fun seekToTimeMsInternal(positionMs: Int) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "PlaybackService seekToTimeMsInternal %.3f ", positionMs.toDouble() / 1000.0)
        val episode = getCurrentEpisode()
        if (episode != null) {
            episode.playedUpToMs = positionMs
        }

        if (pocketCastsPlayer == null) {
            // if the player is sleeping still update the episode progress so the mini player progress still changes
            updateCurrentPositionInDatabase()
        } else {
            // as soon as the user drops the seek progress bar position make sure it has the new playback position, useful for the media session seeking
            withContext(Dispatchers.Main) {
                playbackStateRelay.blockingFirst().let { playbackState ->
                    playbackStateRelay.accept(playbackState.copy(positionMs = positionMs, lastChangeFrom = "onUserSeeking"))
                }
            }

            pocketCastsPlayer?.seekToTimeMs(positionMs)
        }

        withContext(Dispatchers.Main) {
            updatePausedPlaybackState()
        }
    }

    fun playNextInQueue(playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN) {
        launch {
            upNextQueue.queueEpisodes.getOrNull(0)?.let {
                playNowSync(episode = it, playbackSource = playbackSource)
            }
        }
    }

    fun skipForward(playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN, jumpAmountSeconds: Int = settings.getSkipForwardInSecs()) {
        launch {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Skip forward tapped")

            val episode = getCurrentEpisode() ?: return@launch
            val jumpAmountMs = jumpAmountSeconds * 1000

            val currentTimeMs = getCurrentTimeMs(episode = episode)
            if (currentTimeMs < 0 || pocketCastsPlayer?.episodeUuid != episode.uuid) return@launch // Make sure the player hasn't changed episodes before using the current time to seek

            val newPositionMs = currentTimeMs + jumpAmountMs
            val durationMs = pocketCastsPlayer?.durationMs() ?: Int.MAX_VALUE // If we don't have a duration, just let them skip

            statsManager.addTimeSavedSkipping((newPositionMs - currentTimeMs).toLong())
            if (newPositionMs < durationMs) {
                seekToTimeMsInternal(newPositionMs)
            } else {
                LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Seek beyond end of episode so completing. ${episode.uuid}")
                onCompletion(episode.uuid)
            }
        }
        trackPlayback(AnalyticsEvent.PLAYBACK_SKIP_FORWARD, playbackSource)
    }

    fun skipBackward(playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN, jumpAmountSeconds: Int = settings.getSkipBackwardInSecs()) {
        launch {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Skip backward tapped")

            val episode = getCurrentEpisode() ?: return@launch

            val jumpAmountMs = jumpAmountSeconds * 1000
            val currentTimeMs = getCurrentTimeMs(episode = episode)
            if (currentTimeMs < 0) return@launch

            val newPositionMs = Math.max(currentTimeMs - jumpAmountMs, 0)
            seekToTimeMsInternal(newPositionMs)
        }
        trackPlayback(AnalyticsEvent.PLAYBACK_SKIP_BACK, playbackSource)
    }

    fun skipToNextChapter() {
        launch {
            val episode = getCurrentEpisode() ?: return@launch
            val currentTimeMs = getCurrentTimeMs(episode = episode)
            playbackStateRelay.blockingFirst().chapters.getNextChapter(currentTimeMs)?.let { chapter ->
                seekToTimeMsInternal(chapter.startTime)
            }
        }
    }

    fun skipToPreviousChapter() {
        launch {
            val episode = getCurrentEpisode() ?: return@launch
            val currentTimeMs = getCurrentTimeMs(episode)
            playbackStateRelay.blockingFirst().chapters.getPreviousChapter(currentTimeMs)?.let { chapter ->
                seekToTimeMsInternal(chapter.startTime)
            }
        }
    }

    fun skipToChapter(chapter: Chapter) {
        launch {
            seekToTimeMsInternal(chapter.startTime)
        }
    }

    fun skipToChapter(index: Int) {
        launch {
            val chapter = playbackStateRelay.blockingFirst().chapters.getList().firstOrNull { it.index == index } ?: return@launch
            seekToTimeMsInternal(chapter.startTime)
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
            val player = pocketCastsPlayer
            player?.setPlaybackEffects(effects)

            withContext(Dispatchers.Main) {
                playbackStateRelay.blockingFirst().let { playbackState ->
                    playbackStateRelay.accept(
                        playbackState.copy(
                            playbackSpeed = effects.playbackSpeed,
                            isVolumeBoosted = effects.isVolumeBoosted,
                            trimMode = effects.trimMode,
                            lastChangeFrom = "effectsChanged"
                        )
                    )
                }
            }
        }
    }

    private val removeMutex = Mutex()
    fun removeEpisode(episodeToRemove: Playable?, source: AnalyticsSource, userInitiated: Boolean = true) {
        launch {
            if (episodeToRemove == null) {
                return@launch
            }

            removeMutex.withLock {
                val currentEpisode = getCurrentEpisode()

                val isCurrentEpisode = currentEpisode != null && currentEpisode.uuid == episodeToRemove.uuid && (pocketCastsPlayer == null || pocketCastsPlayer?.episodeUuid == episodeToRemove.uuid)
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
                    loadCurrentEpisode(play = isPlaying, playbackSource = AnalyticsSource.AUTO_PLAY)
                }
            }
        }
    }

    private suspend fun onRemoteMetaDataNotMatched(episodeUuid: String) {
        val episode = episodeManager.findPlayableByUuid(episodeUuid) ?: return
        val podcast = if (episode is Episode) podcastManager.findPodcastByUuid(episode.podcastUuid) else null

        if (pocketCastsPlayer?.isRemote == true && pocketCastsPlayer?.isPlaying() == false) {
            if (castManager.isPlaying()) {
                Timber.d("Playing remote episode %s", episode.title)
                playNowSync(episode)
            } else {
                loadCurrentEpisode(play = false)
            }

            pocketCastsPlayer?.setEpisode(episode)
            pocketCastsPlayer?.setPodcast(podcast)
        }
    }

    fun castReconnected() {
        launch(Dispatchers.Main) {
            if (pocketCastsPlayer != null) {
                pocketCastsPlayer?.stop()
                pocketCastsPlayer = null
            }

            pocketCastsPlayer = playerManager.createCastPlayer(this@PlaybackManager::onPlayerEvent)
            (pocketCastsPlayer as? CastPlayer)?.updateFromRemoteIfRequired()
            Timber.i("Cast reconnected. Creating media player of type CastPlayer")

            setupUpdateTimer()
        }
    }

    fun castConnected() {
        upNextQueue.currentEpisode ?: return
        launch {
            if (isPlayerSwitchRequired()) {
                loadCurrentEpisode(true, playbackSource = AnalyticsSource.CHROMECAST)
            }
        }
    }

    fun castDisconnected() {
        upNextQueue.currentEpisode ?: return
        launch {
            updateCurrentPositionInDatabase()

            stop()

            if (isPlayerSwitchRequired()) {
                loadCurrentEpisode(false, playbackSource = AnalyticsSource.CHROMECAST)
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
        if (currentEpisode is Playable) {
            episodeManager.markAsPlaybackError(currentEpisode, event, isPlaybackRemote())
        }

        stop()
        onPlayerPaused()

        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let { playbackState ->
                val errorMessage = if (event.error?.cause is HttpDataSource.HttpDataSourceException) {
                    "Could not load podcast. Please check your internet connection and try again."
                } else {
                    event.message
                }
                val isAutomotive = Util.isAutomotive(application)
                SentryHelper.recordException(
                    message = "Illegal playback state encountered for episode uuid ${episode?.uuid}, isAutomotive $isAutomotive}: ",
                    throwable = event.error ?: IllegalStateException(event.message)
                )
                playbackStateRelay.accept(playbackState.copy(state = PlaybackState.State.ERROR, lastErrorMessage = errorMessage, lastChangeFrom = "onPlayerError"))
            }
        }
    }

    suspend fun onBufferingStateChanged() {
        pocketCastsPlayer?.let {
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
                            lastChangeFrom = "onBufferingStateChanged"
                        )
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
            playbackStateRelay.accept(playbackState.copy(state = PlaybackState.State.PLAYING, transientLoss = false, lastChangeFrom = "onPlayerPlaying"))
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
                        playbackStateRelay.accept(state.copy(podcast = updatedPodcast, lastChangeFrom = "markPodcastNeedsUpdating"))
                    }
                }
            }
        }
    }

    suspend fun onPlayerPaused() {
        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let { playbackState ->
                playbackStateRelay.accept(playbackState.copy(state = PlaybackState.State.PAUSED, lastChangeFrom = "onPlayerPaused"))
            }
        }

        val episode = getCurrentEpisode()

        pocketCastsPlayer?.let {
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
        val shouldSleepAfterEpisode = sleepAfterEpisode
        val wasPlaying = isPlaying()

        cancelUpdateTimer()
        cancelBufferUpdateTimer()

        val episode = getCurrentEpisode()

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Episode ${episode?.title} finished, should sleep: $shouldSleepAfterEpisode")

        if (shouldSleepAfterEpisode) {
            sleep(episode)
            return
        }

        cancelUpdateTimer()
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
            episodeManager.updateAutoDownloadStatus(episode, Episode.AUTO_DOWNLOAD_STATUS_IGNORE)
            removeEpisodeFromQueue(episode, "finished", downloadManager)

            // mark as played
            episodeManager.updatePlayingStatus(episode, EpisodePlayingStatus.COMPLETED)

            // auto archive after playing
            if (episode is Episode) {
                episodeManager.archivePlayedEpisode(episode, this, podcastManager, sync = true)
            } else if (episode is UserEpisode) {
                userEpisodeManager.deletePlayedEpisodeIfReq(episode, this)
            }

            // Sync played to server straight away
            if (syncManager.isLoggedIn()) {
                if (episode is Episode) {
                    val syncRequest =
                        EpisodeSyncRequest(
                            episode.uuid,
                            episode.podcastUuid,
                            episode.durationMs.toLong() / 1000,
                            episode.durationMs.toLong() / 1000,
                            EpisodeSyncRequest.STATUS_COMPLETE
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

        val autoPlay = !shouldSleepAfterEpisode && wasPlaying

        var nextEpisode = getCurrentEpisode()
        if (nextEpisode == null) {
            nextEpisode = autoLoadEpisode(autoPlay)
            if (nextEpisode == null) {
                stop()
                shutdown()
            }
        } else {
            loadCurrentEpisode(play = autoPlay, playbackSource = AnalyticsSource.AUTO_PLAY)
        }
    }

    private suspend fun autoSelectNextEpisode(): Playable? {
        val lastPodcastOrPlaylistUuid = lastLoadedFromPodcastOrPlaylistUuid
        val lastEpisodeUuid = lastPlayedEpisodeUuid
        if (lastEpisodeUuid == null || lastPodcastOrPlaylistUuid == null) {
            return null
        }

        var episodeUuids = emptyList<String>()

        val podcast = podcastManager.findPodcastByUuid(lastPodcastOrPlaylistUuid)
        if (podcast != null) {
            episodeUuids = episodeManager.findEpisodesByPodcastOrdered(podcast).map { it.uuid }
        } else {
            val playlist = playlistManager.findByUuid(lastPodcastOrPlaylistUuid)
            if (playlist != null) {
                episodeUuids = playlistManager.findEpisodes(playlist, episodeManager, this).map { it.uuid }
            }
        }

        val lastEpisodeIndex = episodeUuids.indexOfFirst { it == lastEpisodeUuid }
        // go down the episode list until the end, and then go up the episode list
        episodeUuids = episodeUuids.slice(lastEpisodeIndex + 1 until episodeUuids.size) + episodeUuids.slice(0 until lastEpisodeIndex).asReversed()

        for (episodeUuid in episodeUuids) {
            val episode = episodeManager.findPlayableByUuid(episodeUuid) ?: continue
            if (episode.isFinished || episode.isArchived) {
                continue
            }
            return episode
        }

        // find the latest episode from the podcast in your collection
        return episodeManager.findLatestEpisodeToPlay()
    }

    private suspend fun sleep(episode: Playable?) {
        sleepAfterEpisode = false

        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let {
                playbackStateRelay.accept(it.copy(isSleepTimerRunning = false, lastChangeFrom = "onCompletion"))
            }
        }

        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Sleeping playback")

        showToast(application.getString(LR.string.player_sleep_time_fired))

        val podcast = playbackStateRelay.blockingFirst().podcast
        if (podcast != null && podcast.skipLastSecs > 0) {
            pause(playbackSource = AnalyticsSource.AUTO_PAUSE)
        }
        onPlayerPaused()

        // jump back 5 seconds from the current time so when the player opens it doesn't complete before giving the user a chance to skip back
        pocketCastsPlayer?.let {
            val currentTimeMs = it.getCurrentPositionMs() - 5000
            if (currentTimeMs > 0) {
                val currentTimeSecs = currentTimeMs.toDouble() / 1000.0
                episodeManager.updatePlayedUpTo(episode, currentTimeSecs, false)
            }
        }

        stop()
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(application, message, Toast.LENGTH_LONG).show()
        }
    }

    suspend fun onDurationAvailable() {
        val episode = getCurrentEpisode()
        if (episode == null || pocketCastsPlayer == null) {
            return
        }

        val durationMs = pocketCastsPlayer?.durationMs() ?: 0
        if (durationMs <= 0) {
            return
        }

        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let { playbackState ->
                if (playbackState.durationMs == durationMs) {
                    return@let
                }
                playbackStateRelay.accept(playbackState.copy(durationMs = durationMs, lastChangeFrom = "onDurationAvailable"))
            }
        }

        val playerDurationSecs = durationMs.toDouble() / 1000.0

        val currentDurationMs = episode.durationMs
        if (currentDurationMs < 10000) {
            episodeManager.updateDuration(episode, playerDurationSecs, true)
        } else {
            episodeManager.updateDuration(episode, playerDurationSecs, true)
        }
    }

    suspend fun onSeekComplete(positionMs: Int) {
        playbackStateRelay.blockingFirst().let {
            playbackStateRelay.accept(
                it.copy(
                    positionMs = positionMs,
                    lastChangeFrom = "onSeekComplete"
                )
            )
        }
        updateCurrentPositionInDatabase()
    }

    fun onMetadataAvailable(episodeMetadata: EpisodeFileMetadata) {
        playbackStateRelay.blockingFirst().let { playbackState ->
            val chapters = episodeMetadata.chapters
            if (!chapters.isEmpty) {
                chapters.getList().first().startTime = 0
                chapters.getList().last().endTime = playbackState.durationMs
            }

            playbackStateRelay.accept(
                playbackState.copy(
                    chapters = chapters,
                    embeddedArtworkPath = episodeMetadata.embeddedArtworkPath,
                    lastChangeFrom = "onMetadataAvailable"
                )
            )
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

        pocketCastsPlayer?.setVolume(VOLUME_NORMAL)

        focusWasPlaying = null
    }

    override fun onFocusLoss(mayDuck: Boolean, transientLoss: Boolean) {
        val player = pocketCastsPlayer
        if (player == null || player.isRemote) {
            return
        }
        // if we are playing but can't just reduce the volume then play when focus gained
        val playing = isPlaying()
        if (!mayDuck && playing) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Focus lost while playing")
            focusWasPlaying = Date()

            pause(transientLoss = transientLoss, playbackSource = AnalyticsSource.AUTO_PAUSE)
        } else {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Focus lost not playing")
            focusWasPlaying = null
        }

        // check if we need to reduce the volume
        if (focusManager.canDuck()) {
            player.setVolume(VOLUME_DUCK)
        }
    }

    override fun onFocusRequestFailed() {
        LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Could not get audio focus, stopping")
        stopAsync(isAudioFocusFailed = true)
    }

    override fun onAudioBecomingNoisy() {
        val player = pocketCastsPlayer
        if (player == null || player.isRemote) {
            return
        }
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "System fired 'Audio Becoming Noisy' event, pausing playback.")
        pause(playbackSource = AnalyticsSource.AUTO_PAUSE)
        focusWasPlaying = null
    }

    /** PRIVATE METHODS  */

    /**
     * Check the player is initialised and if we are using the correct player either the system or cast player.
     * @param chromeCastConnected
     */
    private suspend fun isPlayerSwitchRequired(): Boolean {
        if (pocketCastsPlayer == null) {
            return true
        }
        if (forcePlayerSwitch) {
            forcePlayerSwitch = false
            return true
        }
        // using Chrome Cast make sure the player is connected
        return if (!castManager.isConnected()) {
            pocketCastsPlayer is CastPlayer
        } else {
            pocketCastsPlayer is SimplePlayer
        } // otherwise use the ExoPlayer
    }

    /**
     * Does the media player need to be recreated.
     */
    private fun isPlayerResetNeeded(episode: Playable, sameEpisode: Boolean, chromeCastConnected: Boolean): Boolean {
        // reset the player if local and changing episode
        val playbackOnDevice = !chromeCastConnected
        return if (!sameEpisode) {
            playbackOnDevice
        } else episode.isDownloaded &&
            playbackOnDevice &&
            episode.downloadedFilePath != null &&
            pocketCastsPlayer != null &&
            episode.downloadedFilePath != pocketCastsPlayer?.filePath

        // if the player has a different media file path then it is changing
    }

    /**
     * Load the episode
     */
    private suspend fun loadCurrentEpisode(play: Boolean, forceStream: Boolean = false, playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN) {
        // make sure we have the most recent copy from the database
        val currentUpNextEpisode = upNextQueue.currentEpisode
        val episode: Playable? = if (currentUpNextEpisode is Episode) {
            episodeManager.findByUuid(currentUpNextEpisode.uuid)
        } else if (currentUpNextEpisode is UserEpisode) {
            userEpisodeManager.findEpisodeByUuidRx(currentUpNextEpisode.uuid)
                .flatMap {
                    if (it.serverStatus == UserEpisodeServerStatus.MISSING) {
                        userEpisodeManager.downloadMissingUserEpisode(currentUpNextEpisode.uuid, placeholderTitle = null, placeholderPublished = null)
                    } else {
                        Maybe.just(it)
                    }
                }
                .awaitSingleOrNull()
        } else {
            null
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

        val currentPlayer = this.pocketCastsPlayer
        val sameEpisode = currentPlayer != null && episode.uuid == currentPlayer.episodeUuid

        sleepAfterEpisode = sleepAfterEpisode && playbackStateRelay.blockingFirst().episodeUuid == episode.uuid

        // completed episodes should play from the start
        if (episode.isFinished) {
            episodeManager.markAsNotPlayed(episode)
        }
        // check we have the latest episode url in the background
        if (episode is Episode) {
            updateEpisodeUrl(episode)
        }

        if (episode is UserEpisode && episode.serverStatus == UserEpisodeServerStatus.UPLOADED) {
            try {
                val playbackUrl = userEpisodeManager.getPlaybackUrl(episode).await()
                episode.downloadUrl = playbackUrl
            } catch (e: Exception) {
                onPlayerError(PlayerEvent.PlayerError("Could not load cloud file ${e.message}"))
                removeEpisode(episode, source = playbackSource)
                return
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
                settings.warnOnMeteredNetwork() &&
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
                    embeddedArtworkPath = if (sameEpisode) previousPlaybackState?.embeddedArtworkPath else null,
                    lastChangeFrom = "loadCurrentEpisode data warning"
                )
                withContext(Dispatchers.Main) {
                    playbackStateRelay.accept(playbackState)
                }
                return
            } else {
                val episodeObservable: Flowable<Playable>? = if (episode is Episode) {
                    episodeManager.observeByUuid(episode.uuid)
                        .asFlowable()
                        .cast(Playable::class.java)
                } else if (episode is UserEpisode) {
                    userEpisodeManager.observeEpisode(episode.uuid).cast(Playable::class.java)
                } else {
                    null
                }
                if (episodeObservable != null) {
                    episodeSubscription = episodeObservable
                        .takeUntil { it.isDownloaded }
                        .subscribeBy(
                            onNext = {
                                if (pocketCastsPlayer?.isStreaming == true && it.isDownloaded && pocketCastsPlayer?.isRemote == false) {
                                    LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Episode was streaming but was now downloaded, switching to downloaded file")

                                    launch(Dispatchers.Default) {
                                        pocketCastsPlayer?.let { player ->
                                            val currentTimeSecs = player.getCurrentPositionMs().toDouble() / 1000.0
                                            episodeManager.updatePlayedUpTo(it, currentTimeSecs, true)
                                        }
                                        loadCurrentEpisode(isPlaying())
                                    }
                                } else {
                                    Timber.d("Episode is not downloaded $this")
                                }
                            }
                        )
                }
            }
        }

        // keep track of last played episode id so we don't keep bugging the user about streaming on data
        if (play) {
            lastWarnedPlayedEpisodeUuid = episode.uuid
        }
        // keep track of the last played episode so we can auto select the next episode for Android Automotive
        lastPlayedEpisodeUuid = episode.uuid

        // podcast start from
        if (episode is Episode) {
            // Auto subscribe to played podcasts (used in Automotive)
            if (podcast != null && settings.getAutoSubscribeToPlayed() && !podcast.isSubscribed && episode.episodeType !is Episode.EpisodeType.Trailer) {
                podcastManager.subscribeToPodcast(podcast.uuid, sync = true)
            }
        }

        val chromeCastConnected = castManager.isConnected()
        val currentPosition = pocketCastsPlayer?.getCurrentPositionMs()
        if (isPlayerSwitchRequired() || isPlayerResetNeeded(episode, sameEpisode, chromeCastConnected)) { // Don't create a player if we aren't playing because it will start to buffer
            if (play) {
                Timber.d("Resetting player")
                resetPlayer()
            } else {
                Timber.d("Stopping player")
                stopPlayer()
            }
        } else {
            Timber.d("Player reset not required")
        }

        pocketCastsPlayer?.setPodcast(podcast)
        pocketCastsPlayer?.setEpisode(episode)

        val playbackEffects = if (podcast != null && podcast.overrideGlobalEffects) podcast.playbackEffects else settings.getGlobalPlaybackEffects()

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
            embeddedArtworkPath = if (sameEpisode) previousPlaybackState?.embeddedArtworkPath else null,
            playbackSpeed = playbackEffects.playbackSpeed,
            trimMode = playbackEffects.trimMode,
            isVolumeBoosted = playbackEffects.isVolumeBoosted,
            lastChangeFrom = "loadCurrentEpisode"
        )
        withContext(Dispatchers.Main) {
            playbackStateRelay.accept(playbackState)
        }

        // audio effects
        pocketCastsPlayer?.setPlaybackEffects(playbackEffects)

        episodeManager.updatePlaybackInteractionDate(episode)

        widgetManager.updateWidget(podcast, play, episode)

        if (play) {
            if (sameEpisode && currentPosition != null) {
                pocketCastsPlayer?.seekToTimeMs(currentPosition)
            }
            play(playbackSource)
        } else {
            pocketCastsPlayer?.load(episode.playedUpToMs)
            onPlayerPaused()
        }
    }

    private fun findPodcastByEpisode(episode: Playable): Podcast? {
        return when (episode) {
            is Episode -> podcastManager.findPodcastByUuid(episode.podcastUuid)
            is UserEpisode -> podcastManager.buildUserEpisodePodcast(episode)
            else -> null
        }
    }

    /**
     * Check we have the latest episode url in the background.
     */
    private fun updateEpisodeUrl(episode: Episode) {
        if (episode.isDownloaded) {
            return
        }

        // TODO
//        val updateEpisodeTask = UpdateEpisodeTask(podcastCacheServerManager, episodeManager)
//        val episodeSingle = updateEpisodeTask.download(episode.podcastUuid, episode.uuid)
//        val disposable = episodeSingle
//                .subscribeOn(Schedulers.io())
//                .subscribe(
//                        { episodeUpdated ->
//                            val currentEpisode = getCurrentEpisode()
//                            if (currentEpisode != null &&
//                                    currentEpisode.uuid == episodeUpdated.uuid &&
//                                    currentEpisode.downloadUrl != episodeUpdated.downloadUrl) {
//                                upNextQueue.reloadCurrentEpisode()
//                                forcePlayerSwitch = true
//                            }
//                        },
//                        { throwable -> Timber.e(throwable, "Failed to update episode url") }
//                )
//        disposables.add(disposable)
    }

    @Suppress("DEPRECATION")
    private fun sendDataWarningNotification(episode: Playable) {
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

        val streamIntent = buildNotificationIntent(2, NotificationBroadcastReceiver.INTENT_ACTION_STREAM_EPISODE, episode, notificationTag, application)
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
        val sound = settings.getNotificationSound()
        if (sound != null) {
            notification.sound = sound
        }

        manager.notify(notificationTag, NotificationBroadcastReceiver.NOTIFICATION_ID, notification)
    }

    private fun buildNotificationIntent(intentId: Int, intentName: String, episode: Playable, notificationTag: String, context: Context): PendingIntent {
        val intent = Intent(context, NotificationBroadcastReceiver::class.java)
        intent.action = (System.currentTimeMillis() + intentId).toString()
        intent.putExtra(NotificationBroadcastReceiver.INTENT_EXTRA_ACTION, intentName)
        intent.putExtra(NotificationBroadcastReceiver.INTENT_EXTRA_EPISODE_UUID, episode.uuid)
        intent.putExtra(NotificationBroadcastReceiver.INTENT_EXTRA_NOTIFICATION_TAG, notificationTag)
        return PendingIntent.getBroadcast(context, intentId, intent, PendingIntent.FLAG_UPDATE_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
    }

    private suspend fun play(playbackSource: AnalyticsSource = AnalyticsSource.UNKNOWN) {
        val episode = getCurrentEpisode()
        if (episode == null || pocketCastsPlayer == null) {
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
                playbackStateRelay.accept(playbackState.copy(state = PlaybackState.State.PLAYING, lastChangeFrom = "play"))

                if (pocketCastsPlayer?.episodeUuid != playbackState.episodeUuid) {
                    LogBuffer.e(LogBuffer.TAG_PLAYBACK, "Player playing episode that is not the same as playback state. Player: ${pocketCastsPlayer?.episodeUuid} State: ${playbackState.episodeUuid}")
                }

                // Handle skip first
                if (episode is Episode) {
                    addPodcastStartFromSettings(episode, playbackState.podcast, isPlaying = true)
                }
            }
        }

        val currentTimeMs = resumptionHelper.adjustedStartTimeMsFor(episode)
        LogBuffer.i(
            LogBuffer.TAG_PLAYBACK, "Play %.3f %s Player. %s Downloaded: %b Downloading: %b Audio: %b File: %s Uuid: %s", currentTimeMs / 1000f,
            pocketCastsPlayer?.name
                ?: "",
            episode.title, episode.isDownloaded, episode.isDownloading, episode.isAudio, episode.downloadUrl ?: "", episode.uuid
        )

        pocketCastsPlayer?.play(currentTimeMs)

        trackPlayback(AnalyticsEvent.PLAYBACK_PLAY, playbackSource)
    }

    private suspend fun addPodcastStartFromSettings(episode: Episode, podcast: Podcast?, isPlaying: Boolean) {
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
            pocketCastsPlayer?.stop()
            if (castManager.isConnected()) {
                pocketCastsPlayer = playerManager.createCastPlayer(this@PlaybackManager::onPlayerEvent)
                Timber.i("Creating media player of type CastPlayer.")
            } else {
                pocketCastsPlayer = playerManager.createSimplePlayer(this@PlaybackManager::onPlayerEvent)
                Timber.i("Creating media player of type SimplePlayer.")
            }
        }

        resettingPlayer = false
    }

    private suspend fun stopPlayer() {
        withContext(Dispatchers.Main) {
            pocketCastsPlayer?.stop()
        }
    }

    private fun onPlayerEvent(pocketCastsPlayer: PocketCastsPlayer, event: PlayerEvent) {
        if (this.pocketCastsPlayer != pocketCastsPlayer) return

        launch {
            Timber.d("Player %s event %s", pocketCastsPlayer, event)
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
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Saved time in database %.3f", currentTimeSecs)
        episodeManager.updatePlayedUpTo(episode, currentTimeSecs, false)
        episodeManager.updatePlaybackInteractionDate(episode)

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

        val positionMs = pocketCastsPlayer?.getCurrentPositionMs() ?: -1
        if (positionMs < 0) {
            return
        }

        val durationMs = pocketCastsPlayer?.durationMs()
        if (skipLast.isPositive() && durationMs.isPositive() && durationMs > skipLast) {
            val timeRemaining = (durationMs - positionMs) / 1000
            if (timeRemaining < skipLast) {
                if (sleepAfterEpisode) {
                    sleep(episode)
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
                    playbackStateRelay.accept(playbackState.copy(positionMs = positionMs, lastChangeFrom = "updateCurrentPosition"))
                }
            }
        }

        pocketCastsPlayer?.let { player ->
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
        val player = pocketCastsPlayer
        if (episode == null || player == null || !player.isStreaming) {
            return
        }
        val bufferedUpToMs = getBufferedUpToMs()
        if (bufferedUpToMs == lastBufferedUpTo) {
            return
        }
        withContext(Dispatchers.Main) {
            playbackStateRelay.blockingFirst().let { playbackState ->
                playbackStateRelay.accept(playbackState.copy(bufferedMs = bufferedUpToMs, lastChangeFrom = "updateBufferPosition"))
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
            }
            .switchMapCompletable { updateCurrentPositionRx() }
            .subscribeBy(onError = { Timber.e(it) })
    }

    private fun cancelUpdateTimer() {
        updateTimerDisposable?.dispose()
        syncTimerDisposable?.dispose()
        episodeSubscription?.dispose()
    }

    private fun setupBufferUpdateTimer(episode: Playable) {
        val player = pocketCastsPlayer
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
        if (pocketCastsPlayer != null && !isPlaybackRemote()) {
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
        val currentPlayer = this.pocketCastsPlayer
        if (currentPlayer == null || episode.uuid != currentPlayer.episodeUuid) {
            loadCurrentEpisode(false)
        }
    }

    fun loadQueue(): Job {
        return launch {
            val episode = upNextQueue.currentEpisode ?: return@launch
            val currentPlayer = this@PlaybackManager.pocketCastsPlayer
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
                embeddedArtworkPath = if (sameEpisode) previousPlaybackState?.embeddedArtworkPath else null,
                chapters = if (sameEpisode) (previousPlaybackState?.chapters ?: Chapters()) else Chapters(),
                lastChangeFrom = "updatePausedPlaybackState"
            )
            playbackStateRelay.accept(playbackState)
        }
    }

    private fun trackPlayback(event: AnalyticsEvent, playbackSource: AnalyticsSource) {
        if (playbackSource == AnalyticsSource.UNKNOWN) {
            Timber.w("Found unknown playback source.")
        }
        if (!playbackSource.skipTracking()) {
            analyticsTracker.track(event, mapOf(SOURCE_KEY to playbackSource.analyticsValue))
        }
    }

    fun trackPlaybackSeek(
        positionMs: Int,
        playbackSource: AnalyticsSource
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
                    SOURCE_KEY to playbackSource.analyticsValue,
                    SEEK_FROM_PERCENT_KEY to seekFromPercent,
                    SEEK_TO_PERCENT_KEY to seekToPercent
                )
            )
        }
    }

    fun trackPlaybackEffectsEvent(
        event: AnalyticsEvent,
        props: Map<String, Any> = emptyMap(),
        playbackSource: AnalyticsSource
    ) {
        val properties = HashMap<String, Any>()
        properties[SOURCE_KEY] = playbackSource.analyticsValue
        properties.putAll(props)
        analyticsTracker.track(event, properties)
    }
}
