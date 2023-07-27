package au.com.shiftyjelly.pocketcasts.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistProperty
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistUpdateSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserPlaylistUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asObservable
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class FilterEpisodeListViewModel @Inject constructor(
    private val playlistManager: PlaylistManager,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val downloadManager: DownloadManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel(), CoroutineScope {

    companion object {
        const val MAX_DOWNLOAD_ALL = Settings.MAX_DOWNLOAD
    }

    var isFragmentChangingConfigurations: Boolean = false
        private set

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    lateinit var episodesList: LiveData<List<PodcastEpisode>>
    val playlist: MutableLiveData<Playlist> = MutableLiveData()
    val playlistDeleted: MutableLiveData<Boolean> = MutableLiveData(false)

    lateinit var playlistUUID: String

    fun setup(playlistUUID: String) {
        this.playlistUUID = playlistUUID

        val episodes = Observables.combineLatest(
            settings.upNextSwipeActionObservable,
            settings.streamingMode.flow.asObservable(coroutineContext)
        )
            .toFlowable(BackpressureStrategy.LATEST)
            .switchMap { playlistManager.observeByUuidAsList(playlistUUID) }
            .switchMap { playlists ->
                Timber.d("Loading playlist $playlist")
                val playlist = playlists.firstOrNull() // We observe as a list to get notified on delete
                if (playlist != null) {
                    this.playlist.postValue(playlist)
                    playlistManager.observeEpisodes(playlist, episodeManager, playbackManager)
                } else {
                    this.playlistDeleted.postValue(true)
                    Flowable.just(emptyList())
                }
            }
            .distinctUntilChanged()
            .onErrorReturn {
                Timber.e("Could not load episode filter: ${it.message}")
                emptyList()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
        episodesList = episodes.toLiveData()
    }

    fun deletePlaylist() {
        launch {
            withContext(Dispatchers.Default) { playlistManager.findByUuid(playlistUUID) }?.let { playlist ->
                playlistManager.delete(playlist)
                analyticsTracker.track(AnalyticsEvent.FILTER_DELETED)
            }
        }
    }

    fun onPlayAllFromHere(episode: PodcastEpisode) {
        launch {
            val episodes = episodesList.value ?: emptyList()
            val startIndex = episodes.indexOf(episode)
            if (startIndex > -1) {
                playbackManager.upNextQueue.removeAll()
                val count = min(episodes.size - startIndex, settings.getMaxUpNextEpisodes())
                playbackManager.playEpisodes(episodes = episodes.subList(startIndex, startIndex + count), sourceView = SourceView.FILTERS)
            }
        }
    }

    fun changeSort(sortOrder: Playlist.SortOrder) {
        launch {
            playlist.value?.let { playlist ->
                playlist.sortId = sortOrder.value

                val userPlaylistUpdate = UserPlaylistUpdate(
                    listOf(PlaylistProperty.Sort(sortOrder)),
                    PlaylistUpdateSource.FILTER_EPISODE_LIST
                )
                playlistManager.update(playlist, userPlaylistUpdate)
            }
        }
    }

    fun starredChipTapped() {
        launch {
            playlist.value?.let { playlist ->
                playlist.starred = !playlist.starred

                val userPlaylistUpdate = UserPlaylistUpdate(
                    listOf(PlaylistProperty.Starred),
                    PlaylistUpdateSource.FILTER_EPISODE_LIST
                )
                playlistManager.update(playlist, userPlaylistUpdate)
            }
        }
    }

    fun downloadAll() {
        val episodes = (episodesList.value ?: emptyList())
        val trimmedList = episodes.subList(0, min(MAX_DOWNLOAD_ALL, episodes.count()))
        launch {
            trimmedList.forEach {
                downloadManager.addEpisodeToQueue(it, "filter download all", false)
            }
        }
    }

    fun onFromHereCount(episode: PodcastEpisode): Int {
        val episodes = episodesList.value ?: return 0
        val index = max(episodes.indexOf(episode), 0) // -1 on not found
        return min(episodes.count() - index, settings.getMaxUpNextEpisodes())
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun trackFilterShown() {
        analyticsTracker.track(AnalyticsEvent.FILTER_SHOWN)
        FirebaseAnalyticsTracker.openedFilter()
    }
}
