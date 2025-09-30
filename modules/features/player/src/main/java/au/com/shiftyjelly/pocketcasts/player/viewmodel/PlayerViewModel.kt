package au.com.shiftyjelly.pocketcasts.player.viewmodel

import android.content.Context
import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.ChapterSummaryData
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.player.view.UpNextPlaying
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkArguments
import au.com.shiftyjelly.pocketcasts.player.view.dialog.ClearUpNextDialog
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.ads.BlazeAdsManager
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.SleepTimer
import au.com.shiftyjelly.pocketcasts.repositories.playback.SleepTimerState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asObservable
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val bookmarkManager: BookmarkManager,
    private val downloadManager: DownloadManager,
    private val sleepTimer: SleepTimer,
    private val settings: Settings,
    private val theme: Theme,
    private val analyticsTracker: AnalyticsTracker,
    private val episodeAnalytics: EpisodeAnalytics,
    blazeAdsManager: BlazeAdsManager,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel(),
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    data class PodcastEffectsData(val podcast: Podcast, val effects: PlaybackEffects, val showCustomEffectsSettings: Boolean = true)
    data class PlayerHeader(
        val positionMs: Int = 0,
        val durationMs: Int = -1,
        val isPlaying: Boolean = false,
        val isPrepared: Boolean = false,
        val episode: BaseEpisode? = null,
        val podcastTitle: String? = null,
        val isPlaybackRemote: Boolean = false,
        val chapters: Chapters = Chapters(),
        val backgroundColor: Int = 0xFF000000.toInt(),
        val iconTintColor: Int = 0xFFFFFFFF.toInt(),
        val skipForwardInSecs: Int = 15,
        val skipBackwardInSecs: Int = 30,
        val isSleepRunning: Boolean = false,
        val isEffectsOn: Boolean = false,
        val adjustRemainingTimeDuration: Boolean = false,
        val playbackEffects: PlaybackEffects = PlaybackEffects(),
        val isBuffering: Boolean = false,
        val bufferedUpToMs: Int = 0,
        val theme: Theme.ThemeType = Theme.ThemeType.DARK,
        val useEpisodeArtwork: Boolean = false,
    ) {
        val podcastUuid = (episode as? PodcastEpisode)?.podcastUuid
        val episodeUuid = episode?.uuid.orEmpty()
        val episodeTitle = episode?.title.orEmpty()
        val isVideo = episode?.isVideo == true
        val isStarred = (episode as? PodcastEpisode)?.isStarred == true
        val isUserEpisode = episode is UserEpisode

        val isChaptersPresent: Boolean = chapters.isNotEmpty()
        val chapter: Chapter? = chapters.getChapter(positionMs.milliseconds)
        val chapterProgress: Float = chapter?.calculateProgress(positionMs.milliseconds) ?: 0f
        val chapterTimeRemaining: String = chapter?.remainingTime(
            playbackPosition = positionMs.milliseconds,
            playbackSpeed = playbackEffects.playbackSpeed,
            adjustRemainingTimeDuration = adjustRemainingTimeDuration,
        ) ?: ""
        val chapterSummary: ChapterSummaryData = chapters.getChapterSummary(positionMs.milliseconds)
        val isFirstChapter: Boolean = chapters.isFirstChapter(positionMs.milliseconds)
        val isLastChapter: Boolean = chapters.isLastChapter(positionMs.milliseconds)
        val isChapterImagePresent = chapter?.isImagePresent ?: false
        val title = chapter?.title ?: episodeTitle

        fun isPodcastArtworkVisible(): Boolean {
            return (!isVideo || isPlaybackRemote) && !isChapterImagePresent
        }

        fun isChapterArtworkVisible(): Boolean {
            return (!isVideo || isPlaybackRemote) && isChapterImagePresent
        }

        fun isVideoVisible(): Boolean {
            return isVideo && !isPlaybackRemote
        }
    }

    data class UpNextSummary(val episodeCount: Int, val totalTimeSecs: Double, val episodePlaying: Boolean)

    data class ListData(
        var podcastHeader: PlayerHeader,
        var chaptersExpanded: Boolean,
        var chapters: Chapters,
        var currentChapter: Chapter?,
        var upNextExpanded: Boolean,
        var upNextEpisodes: List<BaseEpisode>,
        var upNextSummary: UpNextSummary,
    )
    private val source = SourceView.PLAYER

    var upNextExpanded = settings.getBooleanForKey(Settings.PREFERENCE_UPNEXT_EXPANDED, true)
    var chaptersExpanded = settings.getBooleanForKey(Settings.PREFERENCE_CHAPTERS_EXPANDED, true)

    private val disposables = CompositeDisposable()

    private val playbackStateObservable: Observable<PlaybackState> = playbackManager.playbackStateRelay
        .observeOn(Schedulers.io())
    val upNextStateObservable: Observable<UpNextQueue.State> = playbackManager.upNextQueue.getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager)
        .observeOn(Schedulers.io())

    private val upNextExpandedObservable = BehaviorRelay.create<Boolean>().apply { accept(upNextExpanded) }
    private val chaptersExpandedObservable = BehaviorRelay.create<Boolean>().apply { accept(chaptersExpanded) }

    val listDataRx = Observables.combineLatest(
        upNextStateObservable,
        playbackStateObservable,
        settings.skipBackInSecs.flow.asObservable(coroutineContext),
        settings.skipForwardInSecs.flow.asObservable(coroutineContext),
        upNextExpandedObservable,
        chaptersExpandedObservable,
        settings.useRealTimeForPlaybackRemaingTime.flow.asObservable(coroutineContext),
        settings.artworkConfiguration.flow.asObservable(coroutineContext),
        sleepTimer.stateFlow.asObservable(coroutineContext),
        this::mergeListData,
    )
        .distinctUntilChanged()
        .toFlowable(BackpressureStrategy.LATEST)
    val listDataLive: LiveData<ListData> = listDataRx.toLiveData()
    val playingEpisodeLive: LiveData<Pair<BaseEpisode, Int>> =
        listDataRx.map { Pair(it.podcastHeader.episodeUuid, it.podcastHeader.backgroundColor) }
            .distinctUntilChanged()
            .switchMap { pair -> episodeManager.findEpisodeByUuidRxFlowable(pair.first).map { Pair(it, pair.second) } }
            .toLiveData()

    private var playbackPositionMs: Int = 0

    val upNextPlusData = upNextStateObservable.map { upNextState ->
        var episodeCount = 0
        var totalTime = 0.0
        var upNextEpisodes = emptyList<BaseEpisode>()
        var nowPlaying: BaseEpisode? = null
        if (upNextState is UpNextQueue.State.Loaded) {
            nowPlaying = upNextState.episode
            upNextEpisodes = upNextState.queue
            episodeCount = upNextState.queue.size

            val countEpisodes = listOf(nowPlaying) + upNextEpisodes
            for (countEpisode in countEpisodes) {
                totalTime += countEpisode.duration
                if (countEpisode.isInProgress) {
                    totalTime -= countEpisode.playedUpTo
                }
            }
        }
        val nowPlayingInfo: UpNextPlaying?
        nowPlayingInfo = if (nowPlaying != null) {
            UpNextPlaying(nowPlaying, (nowPlaying.playedUpTo / nowPlaying.duration).toFloat())
        } else {
            null
        }

        val upNextSummary = UpNextSummary(episodeCount = episodeCount, totalTimeSecs = totalTime, episodePlaying = upNextState is UpNextQueue.State.Loaded)

        return@map listOfNotNull(nowPlayingInfo, upNextSummary) + upNextEpisodes
    }

    val upNextLive: LiveData<List<Any>> = upNextPlusData.toFlowable(BackpressureStrategy.LATEST).toLiveData()

    val effectsObservable: Flowable<PodcastEffectsData> = playbackStateObservable
        .toFlowable(BackpressureStrategy.LATEST)
        .map { it.episodeUuid }
        .switchMap { episodeManager.findEpisodeByUuidRxFlowable(it) }
        .switchMap {
            if (it is PodcastEpisode) {
                podcastManager.podcastByUuidRxFlowable(it.podcastUuid)
            } else {
                Flowable.just(Podcast.userPodcast.copy(overrideGlobalEffects = false))
            }
        }
        .map { podcast ->
            val isUserPodcast = podcast.uuid == Podcast.userPodcast.uuid
            PodcastEffectsData(
                podcast = podcast,
                effects = if (podcast.overrideGlobalEffects) podcast.playbackEffects else settings.globalPlaybackEffects.value,
                showCustomEffectsSettings = !isUserPodcast,
            )
        }
        .doOnNext { Timber.i("Effects: Podcast: ${it.podcast.overrideGlobalEffects} ${it.effects}") }
        .observeOn(AndroidSchedulers.mainThread())
    val effectsLive = effectsObservable.toLiveData()

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    private val _snackbarMessages = MutableSharedFlow<SnackbarMessage>()
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _episodeFlow = MutableStateFlow<BaseEpisode?>(null)
    val episodeFlow = _episodeFlow.asStateFlow()
    var episode: BaseEpisode?
        get() = _episodeFlow.value
        set(value) {
            _episodeFlow.value = value
        }

    private val _podcastFlow = MutableStateFlow<Podcast?>(null)
    val podcastFlow = _podcastFlow.asStateFlow()
    var podcast: Podcast?
        get() = _podcastFlow.value
        set(value) {
            _podcastFlow.value = value
        }

    val isSleepRunning = MutableLiveData<Boolean>().apply { postValue(false) }
    val isSleepAtEndOfEpisodeOrChapter = MutableLiveData<Boolean>().apply { postValue(false) }
    val sleepTimeLeftText = MutableLiveData<String>()
    val sleepCustomTimeText = MutableLiveData<String>().apply {
        postValue(calcCustomTimeText())
    }
    val sleepEndOfEpisodesText = MutableLiveData<String>().apply {
        postValue(calcEndOfEpisodeText())
    }
    val sleepEndOfChaptersText = MutableLiveData<String>().apply {
        postValue(calcEndOfChapterText())
    }
    val sleepingInText = MutableLiveData<String>().apply {
        postValue(calcSleepingInEpisodesText())
    }
    var sleepCustomTimeInMinutes: Int = 5
        set(value) {
            field = value.coerceIn(1, 240)
            settings.setSleepTimerCustomMins(field)
            sleepCustomTimeText.postValue(calcCustomTimeText())
            updateSleepTimer()
        }
        get() {
            return settings.getSleepTimerCustomMins()
        }
    val playerFlow = playbackManager.playerFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeAd = blazeAdsManager
        .findPlayerAd()
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    fun setSleepEndOfChapters(chapters: Int = 1, shouldCallUpdateTimer: Boolean = true) {
        val newValue = chapters.coerceIn(1, 240)
        settings.setSleepEndOfChapters(newValue)
        sleepEndOfChaptersText.postValue(calcEndOfChapterText())
        sleepingInText.postValue(calcSleepingInChaptersText())
        if (shouldCallUpdateTimer) {
            updateSleepTimer()
        }
    }

    fun getSleepEndOfChapters(): Int = settings.getSleepEndOfChapters()

    fun setSleepEndOfEpisodes(episodes: Int = 1, shouldCallUpdateTimer: Boolean = true) {
        val newValue = episodes.coerceIn(1, 240)
        settings.setSleepEndOfEpisodes(newValue)
        sleepEndOfEpisodesText.postValue(calcEndOfEpisodeText())
        sleepingInText.postValue(calcSleepingInEpisodesText())
        if (shouldCallUpdateTimer) {
            updateSleepTimer()
        }
    }

    fun getSleepEndOfEpisodes(): Int = settings.getSleepEndOfEpisodes()

    init {
        updateSleepTimer()
        monitorPlaybackPosition()
    }

    private fun monitorPlaybackPosition() {
        playbackStateObservable
            .map { it.positionMs }
            .toFlowable(BackpressureStrategy.LATEST)
            .subscribeBy(
                onNext = { positionMs ->
                    playbackPositionMs = positionMs
                },
            )
            .apply {
                disposables.add(this)
            }
    }

    private fun mergeListData(
        upNextState: UpNextQueue.State,
        playbackState: PlaybackState,
        skipBackwardInSecs: Int,
        skipForwardInSecs: Int,
        upNextExpanded: Boolean,
        chaptersExpanded: Boolean,
        adjustRemainingTimeDuration: Boolean,
        artworkConfiguration: ArtworkConfiguration,
        sleepTimerState: SleepTimerState,
    ): ListData {
        val podcast: Podcast? = (upNextState as? UpNextQueue.State.Loaded)?.podcast
        val episode = (upNextState as? UpNextQueue.State.Loaded)?.episode

        this.episode = episode
        this.podcast = podcast

        val effects = PlaybackEffects().apply {
            playbackSpeed = playbackState.playbackSpeed
            trimMode = playbackState.trimMode
            isVolumeBoosted = playbackState.isVolumeBoosted
        }

        val podcastHeader: PlayerHeader
        if (episode == null) {
            podcastHeader = PlayerHeader()
        } else {
            isSleepRunning.postValue(sleepTimerState.isSleepTimerRunning)
            val playerBackground = theme.playerBackgroundColor(podcast)
            val iconTintColor = theme.playerHighlightColor(podcast)

            podcastHeader = PlayerHeader(
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                isPlaying = playbackState.isPlaying,
                isPrepared = playbackState.isPrepared,
                episode = episode,
                isPlaybackRemote = playbackManager.isPlaybackRemote(),
                chapters = playbackState.chapters,
                backgroundColor = playerBackground,
                iconTintColor = iconTintColor,
                podcastTitle = if (playbackState.chapters.isEmpty()) podcast?.title else null,
                skipBackwardInSecs = skipBackwardInSecs,
                skipForwardInSecs = skipForwardInSecs,
                isSleepRunning = sleepTimerState.isSleepTimerRunning,
                isEffectsOn = !effects.usingDefaultValues,
                playbackEffects = effects,
                adjustRemainingTimeDuration = adjustRemainingTimeDuration,
                isBuffering = playbackState.isBuffering,
                bufferedUpToMs = playbackState.bufferedMs,
                theme = theme.activeTheme,
                useEpisodeArtwork = artworkConfiguration.useEpisodeArtwork,
            )
        }
        val chapters = playbackState.chapters
        val currentChapter = playbackState.chapters.getChapter(playbackState.positionMs.milliseconds)

        var episodeCount = 0
        var totalTime = 0.0
        var upNextEpisodes = emptyList<BaseEpisode>()
        if (upNextState is UpNextQueue.State.Loaded) {
            upNextEpisodes = upNextState.queue
            episodeCount = upNextState.queue.size
            for (upNextEpisode in upNextState.queue) {
                totalTime += upNextEpisode.duration
                if (upNextEpisode.isInProgress) {
                    totalTime -= upNextEpisode.playedUpTo
                }
            }
        }
        val upNextFooter = UpNextSummary(episodeCount = episodeCount, totalTimeSecs = totalTime, episodePlaying = upNextState is UpNextQueue.State.Loaded)

        return ListData(
            podcastHeader = podcastHeader,
            chaptersExpanded = chaptersExpanded,
            chapters = chapters,
            currentChapter = currentChapter,
            upNextExpanded = upNextExpanded,
            upNextEpisodes = upNextEpisodes,
            upNextSummary = upNextFooter,
        )
    }

    fun onPlayPauseClicked() {
        if (playbackManager.isPlaying()) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Pause clicked in player")
            playbackManager.pause(sourceView = source)
        } else {
            if (playbackManager.shouldWarnAboutPlayback(playbackManager.upNextQueue.currentEpisode?.uuid)) {
                viewModelScope.launch(ioDispatcher) {
                    // show the stream warning if the episode isn't downloaded
                    playbackManager.getCurrentEpisode()?.let { episode ->
                        withContext(Dispatchers.Main) {
                            if (episode.isDownloaded) {
                                play()
                                _snackbarMessages.emit(SnackbarMessage.ShowBatteryWarningIfAppropriate)
                            } else {
                                _navigationState.emit(NavigationState.ShowStreamingWarningDialog(episode))
                            }
                        }
                    }
                }
            } else {
                play()
                viewModelScope.launch {
                    _snackbarMessages.emit(SnackbarMessage.ShowBatteryWarningIfAppropriate)
                }
            }
        }
    }

    fun play() {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Play clicked in player")
        playbackManager.playQueue(sourceView = source)
    }

    fun playEpisode(uuid: String, sourceView: SourceView = SourceView.UNKNOWN) {
        launch {
            val episode = episodeManager.findEpisodeByUuid(uuid) ?: return@launch
            playbackManager.playNow(episode = episode, sourceView = sourceView)
        }
    }

    fun onSkipBackwardClick() {
        playbackManager.skipBackward(sourceView = source, jumpAmountSeconds = settings.skipBackInSecs.value)
    }

    fun onSkipForwardClick() {
        playbackManager.skipForward(sourceView = source, jumpAmountSeconds = settings.skipForwardInSecs.value)
    }

    fun onSkipForwardLongClick() {
        viewModelScope.launch {
            _navigationState.emit(NavigationState.ShowSkipForwardLongPressOptionsDialog)
        }
    }

    fun onMarkAsPlayedClick() {
        playbackManager.upNextQueue.currentEpisode?.let {
            markAsPlayedConfirmed(it)
        }
    }

    fun hasNextEpisode(): Boolean {
        return playbackManager.upNextQueue.queueEpisodes.isNotEmpty()
    }

    fun onNextEpisodeClick() {
        playbackManager.playNextInQueue(sourceView = source)
    }

    fun markAsPlayedConfirmed(episode: BaseEpisode, shouldShuffleUpNext: Boolean = false) {
        launch {
            episodeManager.markAsPlayedBlocking(episode, playbackManager, podcastManager, shouldShuffleUpNext)
            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_MARKED_AS_PLAYED, source, episode.uuid)
        }
    }

    fun archiveConfirmed(episode: PodcastEpisode) {
        launch {
            episodeManager.archiveBlocking(episode, playbackManager, sync = true, shouldShuffleUpNext = settings.upNextShuffle.value)
            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_ARCHIVED, source, episode.uuid)
        }
    }

    suspend fun createBookmarkArguments(): BookmarkArguments? {
        val episode = episode ?: return null
        val timeSecs = playbackPositionMs / 1000
        val bookmark = bookmarkManager.findByEpisodeTime(episode, timeSecs)
        val podcast = podcast
        return BookmarkArguments(
            bookmarkUuid = bookmark?.uuid,
            episodeUuid = episode.uuid,
            timeSecs = timeSecs,
            podcastColors = podcast?.let(::PodcastColors) ?: PodcastColors.ForUserEpisode,
        )
    }

    fun handleDownloadClickFromPlaybackActions(onDeleteStart: () -> Unit, onDownloadStart: () -> Unit) {
        val episode = playbackManager.upNextQueue.currentEpisode ?: return

        if (episode.episodeStatus != EpisodeStatusEnum.NOT_DOWNLOADED) {
            onDeleteStart.invoke()
            launch {
                episodeManager.deleteEpisodeFile(episode, playbackManager, disableAutoDownload = false)
            }
        } else {
            onDownloadStart.invoke()
            launch {
                DownloadHelper.manuallyDownloadEpisodeNow(episode, "Player shelf", downloadManager, episodeManager, source = source)
            }
        }
    }

    fun seekToMs(seekTimeMs: Int, seekComplete: () -> Unit) {
        playbackManager.seekToTimeMs(seekTimeMs, seekComplete)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    private fun calcCustomTimeText(): String {
        val hours = sleepCustomTimeInMinutes / 60
        val minutes = sleepCustomTimeInMinutes % 60

        return if (hours == 1 && minutes == 0) {
            context.resources.getString(LR.string.hours_singular)
        } else if (hours == 1 && minutes > 0) {
            context.resources.getString(LR.string.hour_and_minutes, minutes)
        } else if (hours > 1 && minutes == 0) {
            context.resources.getString(LR.string.hours_plural, hours)
        } else if (hours > 0) {
            context.resources.getString(LR.string.hours_and_minutes, hours, minutes)
        } else if (hours == 0 && minutes == 1) {
            context.resources.getString(LR.string.minutes_singular)
        } else {
            context.resources.getString(LR.string.minutes_plural, sleepCustomTimeInMinutes)
        }
    }

    private fun calcEndOfEpisodeText(): String {
        return if (getSleepEndOfEpisodes() == 1) {
            context.resources.getString(LR.string.player_sleep_timer_in_episode)
        } else {
            context.resources.getString(LR.string.player_sleep_timer_in_episode_plural, getSleepEndOfEpisodes())
        }
    }

    private fun calcEndOfChapterText(): String {
        return if (getSleepEndOfChapters() == 1) {
            context.resources.getString(LR.string.player_sleep_timer_in_chapter)
        } else {
            context.resources.getString(LR.string.player_sleep_timer_in_chapter_plural, getSleepEndOfChapters())
        }
    }

    private fun calcSleepingInEpisodesText(): String {
        return if (getSleepEndOfEpisodes() == 1) {
            context.resources.getString(LR.string.player_sleep_in_one_episode)
        } else {
            context.resources.getString(LR.string.player_sleep_in_episodes, getSleepEndOfEpisodes())
        }
    }

    private fun calcSleepingInChaptersText(): String {
        return if (getSleepEndOfChapters() == 1) {
            context.resources.getString(LR.string.player_sleep_in_one_chapter)
        } else {
            context.resources.getString(LR.string.player_sleep_in_chapters, getSleepEndOfChapters())
        }
    }

    fun updateSleepTimer() {
        val timeLeft = timeLeftInSeconds()
        if ((sleepTimer.state.isSleepTimerRunning && timeLeft > 0) || playbackManager.isSleepAfterEpisodeEnabled()) {
            isSleepAtEndOfEpisodeOrChapter.postValue(playbackManager.isSleepAfterEpisodeEnabled())
            sleepTimeLeftText.postValue(if (timeLeft > 0) Util.formattedSeconds(timeLeft.toDouble()) else "")
            setSleepEndOfEpisodes(sleepTimer.state.numberOfEpisodesLeft, shouldCallUpdateTimer = false)
            sleepingInText.postValue(calcSleepingInEpisodesText())
        } else if (playbackManager.isSleepAfterChapterEnabled()) {
            isSleepAtEndOfEpisodeOrChapter.postValue(playbackManager.isSleepAfterChapterEnabled())
            setSleepEndOfChapters(sleepTimer.state.numberOfChaptersLeft, shouldCallUpdateTimer = false)
            sleepingInText.postValue(calcSleepingInChaptersText())
        } else {
            isSleepAtEndOfEpisodeOrChapter.postValue(false)
            sleepTimer.updateSleepTimerStatus(false)
        }
    }

    fun timeLeftInSeconds(): Int {
        return (sleepTimer.state.timeLeft.inWholeMilliseconds / DateUtils.SECOND_IN_MILLIS).toInt()
    }

    fun sleepTimerAfter(mins: Int) {
        sleepTimer.sleepAfter(mins.toDuration(DurationUnit.MINUTES))
        LogBuffer.i(SleepTimer.TAG, "Sleep after $mins minutes configured")
    }

    fun sleepTimerAfterEpisode(episodes: Int = 1) {
        LogBuffer.i(SleepTimer.TAG, "Sleep after $episodes episodes configured")
        settings.setlastSleepEndOfEpisodes(episodes)
        sleepTimer.cancelTimer()
        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterEpisodes = episodes)
    }

    fun sleepTimerAfterChapter(chapters: Int = 1) {
        LogBuffer.i(SleepTimer.TAG, "Sleep after $chapters chapters configured")
        settings.setlastSleepEndOfChapters(chapters)
        sleepTimer.cancelTimer()
        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = true, sleepAfterChapters = chapters)
    }

    fun cancelSleepTimer() {
        LogBuffer.i(SleepTimer.TAG, "Cancelled sleep timer")
        sleepTimer.updateSleepTimerStatus(sleepTimeRunning = false)
        sleepTimer.cancelTimer()
    }

    fun sleepTimerAddExtraMins(mins: Int) {
        sleepTimer.addExtraTime(mins.toDuration(DurationUnit.MINUTES))
        updateSleepTimer()
    }

    fun changeUpNextEpisodes(episodes: List<BaseEpisode>) {
        playbackManager.changeUpNext(episodes)
    }

    fun saveEffects(effects: PlaybackEffects, podcast: Podcast) {
        launch {
            if (podcast.overrideGlobalEffects) {
                podcastManager.updateEffectsBlocking(podcast, effects)
            } else {
                settings.globalPlaybackEffects.set(effects, updateModifiedAt = true)
            }
            playbackManager.updatePlayerEffects(effects)
        }
    }

    fun onEffectsSettingsSegmentedTabSelected(podcast: Podcast, selectedTab: PlaybackEffectsSettingsTab) {
        val currentEpisode = playbackManager.getCurrentEpisode()
        val isCurrentPodcast = currentEpisode?.podcastOrSubstituteUuid == podcast.uuid
        if (!isCurrentPodcast) return
        viewModelScope.launch(ioDispatcher) {
            val override = selectedTab == PlaybackEffectsSettingsTab.ThisPodcast
            podcastManager.updateOverrideGlobalEffectsBlocking(podcast, override)

            val effects = if (override) podcast.playbackEffects else settings.globalPlaybackEffects.value
            podcast.overrideGlobalEffects = override
            saveEffects(effects, podcast)
        }
        trackPlaybackEffectsEvent(AnalyticsEvent.PLAYBACK_EFFECT_SETTINGS_CHANGED)
    }

    fun clearUpNext(context: Context, upNextSource: UpNextSource): ClearUpNextDialog {
        val dialog = ClearUpNextDialog(
            source = upNextSource,
            removeNowPlaying = false,
            playbackManager = playbackManager,
            analyticsTracker = analyticsTracker,
            context = context,
        )
        val forceDarkTheme = settings.useDarkUpNextTheme.value && upNextSource != UpNextSource.UP_NEXT_TAB
        dialog.setForceDarkTheme(forceDarkTheme)
        return dialog
    }

    fun onChapterUrlClick(chapterUrl: HttpUrl) {
        viewModelScope.launch {
            _navigationState.emit(NavigationState.OpenChapterUrl(chapterUrl.toString()))
        }
    }

    fun onNextChapterClick() {
        analyticsTracker.track(AnalyticsEvent.PLAYER_NEXT_CHAPTER_TAPPED)
        playbackManager.skipToNextSelectedOrLastChapter()
    }

    fun onPreviousChapterClick() {
        analyticsTracker.track(AnalyticsEvent.PLAYER_PREVIOUS_CHAPTER_TAPPED)
        playbackManager.skipToPreviousSelectedOrLastChapter()
    }

    fun onChapterTitleClick(chapter: Chapter) {
        viewModelScope.launch {
            _navigationState.emit(NavigationState.OpenChapterAt(chapter))
        }
    }

    fun onPodcastTitleClick(episodeUuid: String, podcastUuid: String?) {
        if (podcastUuid == null) return
        analyticsTracker.track(
            AnalyticsEvent.EPISODE_DETAIL_PODCAST_NAME_TAPPED,
            mapOf(
                ShelfViewModel.Companion.AnalyticsProp.Key.EPISODE_UUID to episodeUuid,
                ShelfViewModel.Companion.AnalyticsProp.Key.SOURCE to EpisodeViewSource.NOW_PLAYING.value,
            ),
        )
        viewModelScope.launch {
            _navigationState.emit(NavigationState.OpenPodcastPage(podcastUuid, source))
        }
    }

    fun trackPlaybackEffectsEvent(
        event: AnalyticsEvent,
        properties: Map<String, Any> = emptyMap(),
    ) {
        playbackManager.trackPlaybackEffectsEvent(
            event = event,
            props = buildMap {
                putAll(properties)
                val settings = if (effectsLive.value?.podcast?.overrideGlobalEffects == true) {
                    PlaybackEffectsSettingsTab.ThisPodcast.analyticsValue
                } else {
                    PlaybackEffectsSettingsTab.AllPodcasts.analyticsValue
                }
                put(AnalyticsProp.SETTINGS, settings)
            },
            sourceView = SourceView.PLAYER_PLAYBACK_EFFECTS,
        )
    }

    fun trackAdImpression(ad: BlazeAd) {
        analyticsTracker.trackBannerAdImpression(id = ad.id, location = ad.location.value)
    }

    fun trackAdTapped(ad: BlazeAd) {
        analyticsTracker.trackBannerAdTapped(id = ad.id, location = ad.location.value)
    }

    sealed interface NavigationState {
        data class ShowStreamingWarningDialog(val episode: BaseEpisode) : NavigationState
        data object ShowSkipForwardLongPressOptionsDialog : NavigationState
        data class OpenChapterAt(val chapter: Chapter) : NavigationState
        data class OpenPodcastPage(val podcastUuid: String, val source: SourceView) : NavigationState
        data class OpenChapterUrl(val chapterUrl: String) : NavigationState
    }

    sealed interface SnackbarMessage {
        data object ShowBatteryWarningIfAppropriate : SnackbarMessage
    }

    private object AnalyticsProp {
        const val SETTINGS = "settings"
    }

    enum class PlaybackEffectsSettingsTab(@StringRes val labelResId: Int, val analyticsValue: String) {
        AllPodcasts(LR.string.podcasts_all, "global"),
        ThisPodcast(LR.string.podcast_this, "local"),
    }
}
