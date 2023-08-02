package au.com.shiftyjelly.pocketcasts.player.viewmodel

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.helper.UserEpisodePodcastSubstitute
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.view.ShelfItem
import au.com.shiftyjelly.pocketcasts.player.view.ShelfItems
import au.com.shiftyjelly.pocketcasts.player.view.UpNextPlaying
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkArguments
import au.com.shiftyjelly.pocketcasts.player.view.dialog.ClearUpNextDialog
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getUrlForArtwork
import au.com.shiftyjelly.pocketcasts.repositories.extensions.saveToGlobalSettings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.SleepTimer
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.DeleteState
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val userEpisodeManager: UserEpisodeManager,
    private val userManager: UserManager,
    private val downloadManager: DownloadManager,
    private val podcastManager: PodcastManager,
    private val bookmarkManager: BookmarkManager,
    private val sleepTimer: SleepTimer,
    private val settings: Settings,
    private val theme: Theme,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val episodeAnalytics: EpisodeAnalytics,
    @ApplicationContext private val context: Context,
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    sealed class Artwork {
        data class Url(val url: String) : Artwork()
        data class Path(val path: String) : Artwork()
        object None : Artwork()
    }

    data class PodcastEffectsPair(val podcast: Podcast, val effects: PlaybackEffects)
    data class PlayerHeader(
        val positionMs: Int = 0,
        val durationMs: Int = -1,
        val isPlaying: Boolean = false,
        val isPrepared: Boolean = false,
        val podcastUuid: String? = "",
        val episodeUuid: String = "",
        val episodeTitle: String = "",
        val podcastTitle: String? = null,
        val isVideo: Boolean = false,
        val isStarred: Boolean = false,
        val isPlaybackRemote: Boolean = false,
        val chapters: Chapters = Chapters(),
        val backgroundColor: Int = 0xFF000000.toInt(),
        val iconTintColor: Int = 0xFFFFFFFF.toInt(),
        val skipForwardInSecs: Int = 15,
        val skipBackwardInSecs: Int = 30,
        val isSleepRunning: Boolean = false,
        val isEffectsOn: Boolean = false,
        val isBuffering: Boolean = false,
        val downloadStatus: EpisodeStatusEnum = EpisodeStatusEnum.NOT_DOWNLOADED,
        val bufferedUpToMs: Int = 0,
        val embeddedArtwork: Artwork = Artwork.None,
        val isUserEpisode: Boolean = false,
        val theme: Theme.ThemeType = Theme.ThemeType.DARK
    ) {

        val isChaptersPresent: Boolean = !chapters.isEmpty
        val chapter: Chapter? = chapters.getChapter(positionMs)
        val chapterSummary: String = chapters.getChapterSummary(positionMs)
        val isFirstChapter: Boolean = chapters.isFirstChapter(positionMs)
        val isLastChapter: Boolean = chapters.isLastChapter(positionMs)
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

    data class ChaptersHeader(val expanded: Boolean)
    data class UpNextSummary(val episodeCount: Int, val totalTimeSecs: Double)

    data class ListData(
        var podcastHeader: PlayerHeader,
        var chaptersExpanded: Boolean,
        var chapters: List<Chapter>,
        var currentChapter: Chapter?,
        var upNextExpanded: Boolean,
        var upNextEpisodes: List<BaseEpisode>,
        var upNextSummary: UpNextSummary,
    ) {
        fun isSameChapter(chapter: Chapter) = currentChapter?.let { it.index == chapter.index } ?: false
    }
    private val source = SourceView.PLAYER
    private val _showPlayerFlow = MutableSharedFlow<Unit>()
    val showPlayerFlow: SharedFlow<Unit> = _showPlayerFlow

    var upNextExpanded = settings.getBooleanForKey(Settings.PREFERENCE_UPNEXT_EXPANDED, true)
    var chaptersExpanded = settings.getBooleanForKey(Settings.PREFERENCE_CHAPTERS_EXPANDED, true)

    private val disposables = CompositeDisposable()

    private val playbackStateObservable: Observable<PlaybackState> = playbackManager.playbackStateRelay
        .observeOn(Schedulers.io())
    private val upNextStateObservable: Observable<UpNextQueue.State> = playbackManager.upNextQueue.getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager)
        .observeOn(Schedulers.io())

    private val upNextExpandedObservable = BehaviorRelay.create<Boolean>().apply { accept(upNextExpanded) }
    private val chaptersExpandedObservable = BehaviorRelay.create<Boolean>().apply { accept(chaptersExpanded) }

    val listDataRx = Observables.combineLatest(
        upNextStateObservable,
        playbackStateObservable,
        settings.skipBackwardInSecsObservable,
        settings.skipForwardInSecsObservable,
        upNextExpandedObservable,
        chaptersExpandedObservable,
        settings.playbackEffectsObservable,
        this::mergeListData
    )
        .distinctUntilChanged()
        .toFlowable(BackpressureStrategy.LATEST)
    val listDataLive: LiveData<ListData> = listDataRx.toLiveData()
    val playingEpisodeLive: LiveData<Pair<BaseEpisode, Int>> =
        listDataRx.map { Pair(it.podcastHeader.episodeUuid, it.podcastHeader.backgroundColor) }
            .distinctUntilChanged()
            .switchMap { pair -> episodeManager.observeEpisodeByUuidRx(pair.first).map { Pair(it, pair.second) } }
            .toLiveData()

    private var playbackPositionMs: Int = 0

    private val shelfObservable = settings.shelfItemsObservable.map { list ->
        if (list.isEmpty()) {
            ShelfItems.itemsList
        } else {
            list.mapNotNull { id ->
                ShelfItems.itemForId(id)
            }
        }
    }
        .toFlowable(BackpressureStrategy.LATEST)
        .combineLatest(userManager.getSignInState()).map { (shelfItems, signInState) ->
            shelfItems.filter { item -> !item.isPlus || signInState.isSignedInAsPlusOrPatron }
        }

    private val shelfUpNext = upNextStateObservable.distinctUntilChanged { t1, t2 ->
        val entry1 = t1 as? UpNextQueue.State.Loaded ?: return@distinctUntilChanged false
        val entry2 = t2 as? UpNextQueue.State.Loaded ?: return@distinctUntilChanged false

        return@distinctUntilChanged (entry1.episode as? PodcastEpisode)?.isStarred == (entry2.episode as? PodcastEpisode)?.isStarred && entry1.episode.episodeStatus == entry2.episode.episodeStatus && entry1.podcast?.isUsingEffects == entry2.podcast?.isUsingEffects
    }

    private val trimmedShelfObservable = Flowables.combineLatest(shelfUpNext.toFlowable(BackpressureStrategy.LATEST), shelfObservable).map { (upNextState, shelf) ->
        val episode = (upNextState as? UpNextQueue.State.Loaded)?.episode
        val isUserEpisode = episode is UserEpisode
        val trimmedShelf = if (isUserEpisode) shelf.filter { it.shownWhen == ShelfItem.Shown.UserEpisodeOnly || it.shownWhen == ShelfItem.Shown.Always } else shelf.filter { it.shownWhen == ShelfItem.Shown.EpisodeOnly || it.shownWhen == ShelfItem.Shown.Always }
        return@map Pair(trimmedShelf, episode)
    }

    val shelfLive: LiveData<List<ShelfItem>> = shelfObservable.toLiveData()
    val trimmedShelfLive: LiveData<Pair<List<ShelfItem>, BaseEpisode?>> = trimmedShelfObservable.toLiveData()

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

        val upNextSummary = UpNextSummary(episodeCount = episodeCount, totalTimeSecs = totalTime)

        return@map listOfNotNull(nowPlayingInfo, upNextSummary) + upNextEpisodes
    }

    val upNextLive: LiveData<List<Any>> = upNextPlusData.toFlowable(BackpressureStrategy.LATEST).toLiveData()

    val effectsObservable: Flowable<PodcastEffectsPair> = playbackStateObservable
        .toFlowable(BackpressureStrategy.LATEST)
        .map { it.episodeUuid }
        .switchMap { episodeManager.observeEpisodeByUuidRx(it) }
        .switchMap {
            if (it is PodcastEpisode) {
                podcastManager.observePodcastByUuid(it.podcastUuid)
            } else {
                Flowable.just(Podcast(uuid = UserEpisodePodcastSubstitute.substituteUuid, title = UserEpisodePodcastSubstitute.substituteTitle, overrideGlobalEffects = false))
            }
        }
        .map { PodcastEffectsPair(it, if (it.overrideGlobalEffects) it.playbackEffects else settings.getGlobalPlaybackEffects()) }
        .doOnNext { Timber.i("Effects: Podcast: ${it.podcast.overrideGlobalEffects} ${it.effects}") }
        .observeOn(AndroidSchedulers.mainThread())
    val effectsLive = effectsObservable.toLiveData()

    var episode: BaseEpisode? = null
    var podcast: Podcast? = null

    val isSleepRunning = MutableLiveData<Boolean>().apply { postValue(false) }
    val isSleepAtEndOfEpisode = MutableLiveData<Boolean>().apply { postValue(false) }
    val sleepTimeLeftText = MutableLiveData<String>()
    val sleepCustomTimeText = MutableLiveData<String>().apply {
        postValue(calcCustomTimeText())
    }
    var sleepCustomTimeMins: Int = 5
        set(value) {
            field = value.coerceIn(1, 240)
            settings.setSleepTimerCustomMins(field)
            sleepCustomTimeText.postValue(calcCustomTimeText())
            updateSleepTimer()
        }
        get() {
            return settings.getSleepTimerCustomMins()
        }

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
                }
            )
            .apply {
                disposables.add(this)
            }
    }

    private fun mergeListData(upNextState: UpNextQueue.State, playbackState: PlaybackState, skipBackwardInSecs: Int, skipForwardInSecs: Int, upNextExpanded: Boolean, chaptersExpanded: Boolean, globalPlaybackEffects: PlaybackEffects): ListData {
        val podcast: Podcast? = (upNextState as? UpNextQueue.State.Loaded)?.podcast
        val episode = (upNextState as? UpNextQueue.State.Loaded)?.episode

        this.episode = episode
        this.podcast = podcast

        val effects = if (podcast?.overrideGlobalEffects == true) podcast.playbackEffects else globalPlaybackEffects

        val embeddedPath = playbackState.embeddedArtworkPath
        val embeddedArtwork: Artwork = if (embeddedPath != null) {
            Artwork.Path(embeddedPath)
        } else if (episode is UserEpisode) {
            val artworkUrl = episode.getUrlForArtwork(themeIsDark = true)
            if (artworkUrl.startsWith("/")) Artwork.Path(artworkUrl) else Artwork.Url(artworkUrl)
        } else Artwork.None

        val podcastHeader: PlayerHeader
        if (episode == null) {
            podcastHeader = PlayerHeader()
        } else {
            isSleepRunning.postValue(playbackState.isSleepTimerRunning)
            val playerBackground = theme.playerBackgroundColor(podcast)
            val iconTintColor = theme.playerHighlightColor(podcast)

            podcastHeader = PlayerHeader(
                positionMs = playbackState.positionMs,
                durationMs = playbackState.durationMs,
                isPlaying = playbackState.isPlaying,
                isPrepared = playbackState.isPrepared,
                isVideo = episode.isVideo,
                isStarred = (episode is PodcastEpisode && episode.isStarred),
                isPlaybackRemote = playbackManager.isPlaybackRemote(),
                chapters = playbackState.chapters,
                backgroundColor = playerBackground,
                iconTintColor = iconTintColor,
                podcastUuid = podcast?.uuid,
                episodeUuid = episode.uuid,
                episodeTitle = episode.title,
                podcastTitle = if (playbackState.chapters.isEmpty) podcast?.title else null,
                skipBackwardInSecs = skipBackwardInSecs,
                skipForwardInSecs = skipForwardInSecs,
                isSleepRunning = playbackState.isSleepTimerRunning,
                isEffectsOn = !effects.usingDefaultValues,
                isBuffering = playbackState.isBuffering,
                downloadStatus = episode.episodeStatus,
                bufferedUpToMs = playbackState.bufferedMs,
                embeddedArtwork = embeddedArtwork,
                isUserEpisode = episode is UserEpisode,
                theme = theme.activeTheme
            )
        }
        // copy the chapter so the diff can see the differences
        val chapters = playbackState.chapters.getListWithState(playbackState.positionMs).map { it.copy() }
        val currentChapter = playbackState.chapters.getChapter(playbackState.positionMs)

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
        val upNextFooter = UpNextSummary(episodeCount = episodeCount, totalTimeSecs = totalTime)

        return ListData(
            podcastHeader = podcastHeader,
            chaptersExpanded = chaptersExpanded,
            chapters = chapters,
            currentChapter = currentChapter,
            upNextExpanded = upNextExpanded,
            upNextEpisodes = upNextEpisodes,
            upNextSummary = upNextFooter
        )
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

    fun skipBackward() {
        playbackManager.skipBackward(sourceView = source)
    }

    fun skipForward() {
        playbackManager.skipForward(sourceView = source)
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

    private fun markAsPlayedConfirmed(episode: BaseEpisode) {
        launch {
            episodeManager.markAsPlayed(episode, playbackManager, podcastManager)
            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_MARKED_AS_PLAYED, source, episode.uuid)
        }
    }

    fun markCurrentlyPlayingAsPlayed(context: Context): ConfirmationDialog? {
        val episode = playbackManager.upNextQueue.currentEpisode ?: return null
        return ConfirmationDialog()
            .setForceDarkTheme(true)
            .setSummary(context.getString(LR.string.player_mark_as_played))
            .setIconId(R.drawable.ic_markasplayed)
            .setButtonType(ConfirmationDialog.ButtonType.Danger(context.getString(LR.string.player_mark_as_played_button)))
            .setOnConfirm { markAsPlayedConfirmed(episode) }
    }

    private fun archiveConfirmed(episode: PodcastEpisode) {
        launch {
            episodeManager.archive(episode, playbackManager, true)
            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_ARCHIVED, source, episode.uuid)
        }
    }

    fun archiveCurrentlyPlaying(resources: Resources): ConfirmationDialog? {
        val episode = playbackManager.upNextQueue.currentEpisode ?: return null
        if (episode is PodcastEpisode) {
            return ConfirmationDialog()
                .setForceDarkTheme(true)
                .setSummary(resources.getString(LR.string.player_archive_summary))
                .setIconId(IR.drawable.ic_archive)
                .setButtonType(ConfirmationDialog.ButtonType.Danger(resources.getString(LR.string.player_archive_title)))
                .setOnConfirm { archiveConfirmed(episode) }
        } else if (episode is UserEpisode) {
            val deleteState = CloudDeleteHelper.getDeleteState(episode)
            val deleteFunction: (UserEpisode, DeleteState) -> Unit = { ep, delState -> CloudDeleteHelper.deleteEpisode(ep, delState, playbackManager, episodeManager, userEpisodeManager) }
            return CloudDeleteHelper.getDeleteDialog(episode, deleteState, deleteFunction, resources)
        }

        return null
    }

    fun buildBookmarkArguments(onSuccess: (BookmarkArguments) -> Unit) {
        val episode = episode ?: return
        val timeSecs = playbackPositionMs / 1000
        launch {
            val bookmark = bookmarkManager.findByEpisodeTime(episode, timeSecs)
            val podcast = podcast
            val backgroundColor = if (podcast == null) 0xFF000000.toInt() else theme.playerBackgroundColor(podcast)
            val tintColor = if (podcast == null) 0xFFFFFFFF.toInt() else theme.playerHighlightColor(podcast)
            val arguments = BookmarkArguments(
                bookmarkUuid = bookmark?.uuid,
                episodeUuid = episode.uuid,
                timeSecs = timeSecs,
                backgroundColor = backgroundColor,
                tintColor = tintColor
            )
            onSuccess(arguments)
        }
    }

    fun downloadCurrentlyPlaying() {
        val episode = playbackManager.upNextQueue.currentEpisode ?: return
        if (episode.episodeStatus != EpisodeStatusEnum.NOT_DOWNLOADED) {
            launch {
                episodeManager.deleteEpisodeFile(episode, playbackManager, disableAutoDownload = false, removeFromUpNext = episode.episodeStatus == EpisodeStatusEnum.DOWNLOADED)
                episodeAnalytics.trackEvent(
                    event = AnalyticsEvent.EPISODE_DOWNLOAD_DELETED,
                    source = source,
                    uuid = episode.uuid,
                )
            }
        } else {
            launch {
                DownloadHelper.manuallyDownloadEpisodeNow(episode, "Player shelf", downloadManager, episodeManager)
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
        return context.resources.getString(LR.string.minutes_plural, sleepCustomTimeMins)
    }

    fun updateSleepTimer() {
        val timeLeft = sleepTimer.timeLeftInSecs()
        if ((sleepTimer.isRunning && timeLeft != null && timeLeft.toInt() > 0) || playbackManager.sleepAfterEpisode) {
            isSleepAtEndOfEpisode.postValue(playbackManager.sleepAfterEpisode)
            sleepTimeLeftText.postValue(if (timeLeft != null && timeLeft > 0) Util.formattedSeconds(timeLeft.toDouble()) else "")
        } else {
            isSleepAtEndOfEpisode.postValue(false)
            playbackManager.updateSleepTimerStatus(false)
        }
    }

    fun timeLeftInSeconds(): Int? {
        return sleepTimer.timeLeftInSecs()
    }

    fun sleepTimerAfter(mins: Int) {
        sleepTimer.sleepAfter(mins = mins) {
            playbackManager.updateSleepTimerStatus(running = true, sleepAfterEpisode = false)
        }
    }

    fun sleepTimerAfterEpisode() {
        playbackManager.updateSleepTimerStatus(running = true, sleepAfterEpisode = true)
        sleepTimer.cancelTimer()
    }

    fun cancelSleepTimer() {
        playbackManager.updateSleepTimerStatus(running = false)
        sleepTimer.cancelTimer()
    }

    fun sleepTimerAddExtraMins(mins: Int) {
        sleepTimer.addExtraTime(mins)
        updateSleepTimer()
    }

    fun starToggle() {
        playbackManager.upNextQueue.currentEpisode?.let {
            if (it is PodcastEpisode) {
                episodeManager.toggleStarEpisodeAsync(episode = it)
                val event = if (it.isStarred) AnalyticsEvent.EPISODE_UNSTARRED else AnalyticsEvent.EPISODE_STARRED
                episodeAnalytics.trackEvent(event, source, it.uuid)
            }
        }
    }

    fun changeUpNextEpisodes(episodes: List<BaseEpisode>) {
        playbackManager.changeUpNext(episodes)
    }

    fun saveEffects(effects: PlaybackEffects, podcast: Podcast) {
        launch {
            if (podcast.overrideGlobalEffects) {
                podcastManager.updateEffects(podcast, effects)
            } else {
                effects.saveToGlobalSettings(settings)
            }
            playbackManager.updatePlayerEffects(effects)
        }
    }

    fun clearPodcastEffects(podcast: Podcast) {
        launch {
            podcastManager.updateOverrideGlobalEffects(podcast, false)
            playbackManager.updatePlayerEffects(settings.getGlobalPlaybackEffects())
        }
    }

    fun clearUpNext(context: Context, upNextSource: UpNextSource): ClearUpNextDialog {
        val dialog = ClearUpNextDialog(
            source = upNextSource,
            removeNowPlaying = false,
            playbackManager = playbackManager,
            analyticsTracker = analyticsTracker,
            context = context
        )
        dialog.setForceDarkTheme(true)
        return dialog
    }

    fun nextChapter() {
        playbackManager.skipToNextChapter()
    }

    fun previousChapter() {
        playbackManager.skipToPreviousChapter()
    }

    fun onChapterClick(chapter: Chapter) {
        launch {
            val listData = listDataLive.value
            if (listData?.isSameChapter(chapter) == true) {
                _showPlayerFlow.emit(Unit)
            } else {
                playbackManager.skipToChapter(chapter)
            }
        }
    }
}
