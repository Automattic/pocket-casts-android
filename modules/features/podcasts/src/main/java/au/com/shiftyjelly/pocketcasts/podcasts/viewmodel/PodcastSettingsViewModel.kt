package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class PodcastSettingsViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val playlistManager: PlaylistManager,
    settings: Settings
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    var podcastUuid: String? = null
    lateinit var podcast: LiveData<Podcast>
    lateinit var includedFilters: LiveData<List<Playlist>>

    val globalSettings = LiveDataReactiveStreams.fromPublisher(
        settings.autoAddUpNextLimit.toFlowable(BackpressureStrategy.LATEST)
            .combineLatest(settings.autoAddUpNextLimitBehaviour.toFlowable(BackpressureStrategy.LATEST))
    )

    fun loadPodcast(uuid: String) {
        this.podcastUuid = uuid
        podcast = LiveDataReactiveStreams.fromPublisher(podcastManager.observePodcastByUuid(uuid).subscribeOn(Schedulers.io()))

        val filters = playlistManager.observeAll().map {
            it.filter { filter -> filter.podcastUuidList.contains(uuid) }
        }
        includedFilters = LiveDataReactiveStreams.fromPublisher(filters)
    }

    fun isAutoAddToUpNextOn(): Boolean {
        return !(podcast.value?.isAutoAddToUpNextOff ?: true)
    }

    fun updateAutoAddToUpNext(isOn: Boolean) {
        val podcast = this.podcast.value ?: return
        val value = if (isOn) Podcast.AUTO_ADD_TO_UP_NEXT_PLAY_LAST else Podcast.AUTO_ADD_TO_UP_NEXT_OFF
        launch {
            podcastManager.updateAutoAddToUpNext(podcast, value)
        }
    }

    fun updateAutoAddToUpNextOrder(value: Int) {
        val podcast = this.podcast.value ?: return
        launch {
            podcastManager.updateAutoAddToUpNext(podcast, value)
        }
    }

    fun setAutoDownloadEpisodes(download: Boolean) {
        val podcast = this.podcast.value ?: return
        launch {
            val autoDownloadStatus = if (download) Podcast.AUTO_DOWNLOAD_NEW_EPISODES else Podcast.AUTO_DOWNLOAD_OFF
            podcastManager.updateAutoDownloadStatus(podcast, autoDownloadStatus)
        }
    }

    fun showNotifications(show: Boolean) {
        val podcast = this.podcast.value ?: return
        launch {
            podcastManager.updateShowNotifications(podcast, show)
        }
    }

    fun updateStartFrom(secs: Int) {
        val podcast = this.podcast.value ?: return
        launch {
            podcastManager.updateStartFromInSec(podcast, secs)
        }
    }

    fun updateSkipLast(secs: Int) {
        val podcast = this.podcast.value ?: return
        launch {
            podcastManager.updateSkipLastInSec(podcast, secs)
            playbackManager.markPodcastNeedsUpdating(podcast.uuid)
        }
    }

    fun unsubscribe() {
        val podcast = this.podcast.value ?: return
        launch {
            podcastManager.unsubscribe(podcast.uuid, playbackManager)
        }
    }

    fun filterSelectionChanged(newSelection: List<String>) {
        launch {
            podcastUuid?.let { podcastUuid ->
                playlistManager.findAll().forEach { playlist ->
                    val currentSelection = playlist.podcastUuidList.toMutableList()
                    val included = newSelection.contains(playlist.uuid)
                    if (included && !currentSelection.contains(podcastUuid)) {
                        currentSelection.add(podcastUuid)
                    } else if (!included && currentSelection.contains(podcastUuid)) {
                        currentSelection.remove(podcastUuid)
                    }

                    playlist.podcastUuidList = currentSelection
                    playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED
                    playlistManager.update(playlist)
                }
            }
        }
    }
}
