package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import android.content.Context
import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.CachedServerCallback
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.views.helper.WarningsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel(), CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val playbackSource = AnalyticsSource.EPISODE_DETAILS
    lateinit var state: LiveData<EpisodeFragmentState>
    val showNotes: MutableLiveData<String> = MutableLiveData()
    lateinit var inUpNext: LiveData<Boolean>
    val isPlaying: LiveData<Boolean> = Transformations.map(playbackManager.playbackStateLive) {
        it.episodeUuid == episode?.uuid && it.isPlaying
    }

    val disposables = CompositeDisposable()

    var episode: Episode? = null
    var isFragmentChangingConfigurations: Boolean = false

    fun setup(episodeUUID: String, podcastUUID: String?, forceDark: Boolean) {
        val isDarkTheme = forceDark || theme.isDarkTheme
        val progressUpdatesObservable = downloadManager.progressUpdateRelay
            .filter { it.episodeUuid == episodeUUID }
            .map { it.downloadProgress }
            .startWith(0f)
            .toFlowable(BackpressureStrategy.LATEST)

        // If we can't find it in the database and we know the podcast uuid we can try load it
        // from the server
        val onEmptyHandler = if (podcastUUID != null) {
            podcastManager.findOrDownloadPodcastRx(podcastUUID).flatMapMaybe {
                val episode = it.episodes.find { episode -> episode.uuid == episodeUUID }
                if (episode != null) {
                    Maybe.just(episode)
                } else {
                    episodeManager.downloadMissingEpisode(episodeUUID, podcastUUID, Episode(uuid = episodeUUID, publishedDate = Date()), podcastManager, downloadMetaData = true).flatMap { playable ->
                        if (playable is Episode) {
                            Maybe.just(playable)
                        } else {
                            Maybe.empty()
                        }
                    }
                }
            }
        } else {
            Maybe.empty<Episode>()
        }

        val stateObservable: Flowable<EpisodeFragmentState> = episodeManager.findByUuidRx(episodeUUID)
            .switchIfEmpty(onEmptyHandler)
            .flatMapPublisher { episode ->
                val zipper: Function3<Episode, Podcast, Float, EpisodeFragmentState> = Function3 { episodeLoaded: Episode, podcast: Podcast, downloadProgress: Float ->
                    val tintColor = podcast.getTintColor(isDarkTheme)
                    val podcastColor = podcast.getTintColor(isDarkTheme)
                    EpisodeFragmentState.Loaded(episodeLoaded, podcast, tintColor, podcastColor, downloadProgress)
                }
                loadShowNotes(episode)
                return@flatMapPublisher Flowable.combineLatest(
                    episodeManager.observeByUuid(episodeUUID),
                    podcastManager.findPodcastByUuidRx(episode.podcastUuid).toFlowable(),
                    progressUpdatesObservable,
                    zipper
                )
            }
            .doOnNext { if (it is EpisodeFragmentState.Loaded) { episode = it.episode } }
            .onErrorReturn { EpisodeFragmentState.Error(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())

        state = LiveDataReactiveStreams.fromPublisher(stateObservable)

        val inUpNextObservable = playbackManager.upNextQueue.changesObservable.toFlowable(BackpressureStrategy.LATEST)
            .map { upNext -> (upNext is UpNextQueue.State.Loaded) && (upNext.episode == episode || upNext.queue.map { it.uuid }.contains(episodeUUID)) }
        inUpNext = LiveDataReactiveStreams.fromPublisher(inUpNextObservable)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun loadShowNotes(episode: Episode) {
        serverShowNotesManager.loadShowNotes(
            episode.uuid,
            object : CachedServerCallback<String> {
                override fun cachedDataFound(data: String) {
                    showNotes.value = data
                }

                override fun networkDataFound(data: String) {
                    showNotes.value = data
                }

                override fun notFound() {
                    showNotes.value = ""
                }
            }
        )
    }

    fun deleteDownloadedEpisode() {
        episode?.let {
            launch {
                episodeManager.deleteEpisodeFile(it, playbackManager, disableAutoDownload = true, removeFromUpNext = true)
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
                if (it.downloadTaskId != null) {
                    episodeManager.stopDownloadAndCleanUp(it, "episode card")
                } else if (!it.isDownloaded) {
                    it.autoDownloadStatus = Episode.AUTO_DOWNLOAD_STATUS_MANUAL_OVERRIDE_WIFI
                    downloadManager.addEpisodeToQueue(it, "episode card", true)
                }
                episodeManager.clearPlaybackError(episode)
            }
        }
    }

    fun markAsPlayedClicked(isOn: Boolean) {
        launch {
            episode?.let { episode ->
                if (isOn) {
                    episodeManager.markAsPlayed(episode, playbackManager, podcastManager)
                } else {
                    episodeManager.markAsNotPlayed(episode)
                }
            }
        }
    }

    fun addToUpNext(isOn: Boolean, addLast: Boolean = false): Boolean {
        episode?.let { episode ->
            return if (!isOn) {
                launch {
                    if (addLast) {
                        playbackManager.playLast(episode)
                    } else {
                        playbackManager.playNext(episode)
                    }
                }

                true
            } else {
                playbackManager.removeEpisode(episode)

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
                } else {
                    episodeManager.unarchive(episode)
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
                playbackManager.pause(playbackSource = playbackSource)
                return false
            } else {
                fromListUuid?.let {
                    FirebaseAnalyticsTracker.podcastEpisodePlayedFromList(it, episode.podcastUuid)
                    analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_EPISODE_PLAY, mapOf(LIST_ID_KEY to it, PODCAST_ID_KEY to episode.podcastUuid))
                }
                playbackManager.playNow(episode, forceStream = force, playbackSource = playbackSource)
                warningsHelper.showBatteryWarningSnackbarIfAppropriate()
                return true
            }
        }

        return false
    }

    fun starClicked() {
        episode?.let { episode ->
            episodeManager.toggleStarEpisodeAsync(episode)
        }
    }

    companion object {
        private const val LIST_ID_KEY = "list_id"
        private const val PODCAST_ID_KEY = "podcast_id"
    }
}

sealed class EpisodeFragmentState {
    data class Loaded(val episode: Episode, val podcast: Podcast, @ColorInt val tintColor: Int, @ColorInt val podcastColor: Int, val downloadProgress: Float) : EpisodeFragmentState()
    data class Error(val error: Throwable) : EpisodeFragmentState()
}
