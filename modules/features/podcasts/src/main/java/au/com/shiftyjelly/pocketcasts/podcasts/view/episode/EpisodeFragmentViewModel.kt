package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import android.content.Context
import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function4
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlowable
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class EpisodeFragmentViewModel @Inject constructor(
    val episodeManager: EpisodeManager,
    val podcastManager: PodcastManager,
    val theme: Theme,
    val serverShowNotesManager: ServerShowNotesManager,
    val downloadManager: DownloadManager,
    val serverManager: ServerManager,
    val playbackManager: PlaybackManager,
    val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val episodeAnalytics: EpisodeAnalytics
) : ViewModel(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val source = SourceView.EPISODE_DETAILS
    lateinit var state: LiveData<EpisodeFragmentState>
    lateinit var showNotesState: LiveData<ShowNotesState>
    lateinit var inUpNext: LiveData<Boolean>
    val isPlaying: LiveData<Boolean> = playbackManager.playbackStateLive.map {
        it.episodeUuid == episode?.uuid && it.isPlaying
    }

    val disposables = CompositeDisposable()

    var episode: PodcastEpisode? = null
    var isFragmentChangingConfigurations: Boolean = false

    fun setup(episodeUuid: String, podcastUuid: String?, forceDark: Boolean) {
        val isDarkTheme = forceDark || theme.isDarkTheme
        val progressUpdatesObservable = downloadManager.progressUpdateRelay
            .filter { it.episodeUuid == episodeUuid }
            .map { it.downloadProgress }
            .startWith(0f)
            .toFlowable(BackpressureStrategy.LATEST)

        // If we can't find it in the database and we know the podcast uuid we can try load it
        // from the server
        val onEmptyHandler = if (podcastUuid != null) {
            podcastManager.findOrDownloadPodcastRx(podcastUuid).flatMapMaybe {
                val episode = it.episodes.find { episode -> episode.uuid == episodeUuid }
                if (episode != null) {
                    Maybe.just(episode)
                } else {
                    episodeManager.downloadMissingEpisode(episodeUuid, podcastUuid, PodcastEpisode(uuid = episodeUuid, publishedDate = Date()), podcastManager, downloadMetaData = true).flatMap { missingEpisode ->
                        if (missingEpisode is PodcastEpisode) {
                            Maybe.just(missingEpisode)
                        } else {
                            Maybe.empty()
                        }
                    }
                }
            }
        } else {
            Maybe.empty()
        }

        val stateObservable: Flowable<EpisodeFragmentState> = episodeManager.findByUuidRx(episodeUuid)
            .switchIfEmpty(onEmptyHandler)
            .flatMapPublisher { episode ->
                val zipper: Function4<PodcastEpisode, Podcast, ShowNotesState, Float, EpisodeFragmentState> = Function4 { episodeLoaded: PodcastEpisode, podcast: Podcast, showNotesState: ShowNotesState, downloadProgress: Float ->
                    val tintColor = podcast.getTintColor(isDarkTheme)
                    val podcastColor = podcast.getTintColor(isDarkTheme)
                    EpisodeFragmentState.Loaded(episodeLoaded, podcast, showNotesState, tintColor, podcastColor, downloadProgress)
                }
                return@flatMapPublisher Flowable.combineLatest(
                    episodeManager.observeByUuid(episodeUuid).asFlowable(),
                    podcastManager.findPodcastByUuidRx(episode.podcastUuid).toFlowable(),
                    serverShowNotesManager.loadShowNotesFlow(podcastUuid = episode.podcastUuid, episodeUuid = episode.uuid).asFlowable(),
                    progressUpdatesObservable,
                    zipper
                )
            }
            .doOnNext { if (it is EpisodeFragmentState.Loaded) { episode = it.episode } }
            .onErrorReturn { EpisodeFragmentState.Error(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())

        state = stateObservable.toLiveData()

        showNotesState = state
            .map { episodeState ->
                when (episodeState) {
                    is EpisodeFragmentState.Loaded -> episodeState.showNotesState
                    is EpisodeFragmentState.Error -> ShowNotesState.NotFound
                }
            }
            .distinctUntilChanged()

        val inUpNextObservable = playbackManager.upNextQueue.changesObservable.toFlowable(BackpressureStrategy.LATEST)
            .map { upNext -> (upNext is UpNextQueue.State.Loaded) && (upNext.episode == episode || upNext.queue.map { it.uuid }.contains(episodeUuid)) }
        inUpNext = inUpNextObservable.toLiveData()
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun deleteDownloadedEpisode() {
        episode?.let {
            launch {
                episodeManager.deleteEpisodeFile(it, playbackManager, disableAutoDownload = true, removeFromUpNext = true)
                episodeAnalytics.trackEvent(
                    event = AnalyticsEvent.EPISODE_DOWNLOAD_DELETED,
                    source = source,
                    uuid = it.uuid,
                )
            }
        }
    }

    fun shouldDownload(): Boolean {
        return episode?.let {
            it.downloadTaskId == null && !it.isDownloaded
        } ?: false
    }

    fun downloadEpisode() {
        launch {
            episode?.let {
                var analyticsEvent: AnalyticsEvent? = null
                if (it.downloadTaskId != null) {
                    episodeManager.stopDownloadAndCleanUp(it, "episode card")
                    analyticsEvent = AnalyticsEvent.EPISODE_DOWNLOAD_CANCELLED
                } else if (!it.isDownloaded) {
                    it.autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUAL_OVERRIDE_WIFI
                    downloadManager.addEpisodeToQueue(it, "episode card", true)
                    analyticsEvent = AnalyticsEvent.EPISODE_DOWNLOAD_QUEUED
                }
                episodeManager.clearPlaybackError(episode)
                analyticsEvent?.let { event ->
                    episodeAnalytics.trackEvent(event, source = source, uuid = it.uuid)
                }
            }
        }
    }

    fun markAsPlayedClicked(isOn: Boolean) {
        launch {
            val event: AnalyticsEvent
            episode?.let { episode ->
                if (isOn) {
                    event = AnalyticsEvent.EPISODE_MARKED_AS_PLAYED
                    episodeManager.markAsPlayed(episode, playbackManager, podcastManager)
                } else {
                    event = AnalyticsEvent.EPISODE_MARKED_AS_UNPLAYED
                    episodeManager.markAsNotPlayed(episode)
                }
                episodeAnalytics.trackEvent(event, source, episode.uuid)
            }
        }
    }

    fun addToUpNext(isOn: Boolean, addLast: Boolean = false): Boolean {
        episode?.let { episode ->
            return if (!isOn) {
                launch {
                    if (addLast) {
                        playbackManager.playLast(episode = episode, source = source)
                    } else {
                        playbackManager.playNext(episode = episode, source = source)
                    }
                }

                true
            } else {
                playbackManager.removeEpisode(episodeToRemove = episode, source = source)

                false
            }
        }

        return false
    }

    fun shouldShowUpNextDialog(): Boolean {
        return playbackManager.upNextQueue.queueEpisodes.isNotEmpty()
    }

    fun seekToTimeMs(positionMs: Int) {
        playbackManager.seekToTimeMs(positionMs)
    }

    fun isCurrentlyPlayingEpisode(): Boolean {
        return playbackManager.getCurrentEpisode()?.uuid == episode?.uuid
    }

    fun archiveClicked(isOn: Boolean) {
        launch {
            episode?.let { episode ->
                if (isOn) {
                    episodeManager.archive(episode, playbackManager)
                    episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_ARCHIVED, source, episode.uuid)
                } else {
                    episodeManager.unarchive(episode)
                    episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_UNARCHIVED, source, episode.uuid)
                }
            }
        }
    }

    fun shouldShowStreamingWarning(context: Context): Boolean {
        return isPlaying.value == false && episode?.isDownloaded == false && settings.warnOnMeteredNetwork() && !Network.isUnmeteredConnection(context)
    }

    fun playClickedGetShouldClose(
        warningsHelper: WarningsHelper,
        force: Boolean = false,
        fromListUuid: String? = null
    ): Boolean {
        episode?.let { episode ->
            if (isPlaying.value == true) {
                playbackManager.pause(sourceView = source)
                return false
            } else {
                fromListUuid?.let {
                    FirebaseAnalyticsTracker.podcastEpisodePlayedFromList(it, episode.podcastUuid)
                    analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_EPISODE_PLAY, mapOf(LIST_ID_KEY to it, PODCAST_ID_KEY to episode.podcastUuid))
                }
                playbackManager.playNow(episode, forceStream = force, sourceView = source)
                warningsHelper.showBatteryWarningSnackbarIfAppropriate()
                return true
            }
        }

        return false
    }

    fun starClicked() {
        episode?.let { episode ->
            episodeManager.toggleStarEpisodeAsync(episode)
            val event = if (episode.isStarred) AnalyticsEvent.EPISODE_UNSTARRED else AnalyticsEvent.EPISODE_STARRED
            episodeAnalytics.trackEvent(event, source, episode.uuid)
        }
    }

    companion object {
        private const val LIST_ID_KEY = "list_id"
        private const val PODCAST_ID_KEY = "podcast_id"
    }
}

sealed class EpisodeFragmentState {
    data class Loaded(val episode: PodcastEpisode, val podcast: Podcast, val showNotesState: ShowNotesState, @ColorInt val tintColor: Int, @ColorInt val podcastColor: Int, val downloadProgress: Float) : EpisodeFragmentState()
    data class Error(val error: Throwable) : EpisodeFragmentState()
}
