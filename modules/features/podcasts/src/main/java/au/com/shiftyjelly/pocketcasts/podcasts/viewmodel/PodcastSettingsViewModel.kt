package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistProperty
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistUpdateSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserPlaylistUpdate
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
    private val analyticsTracker: AnalyticsTrackerWrapper,
    settings: Settings
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    var podcastUuid: String? = null
    lateinit var podcast: LiveData<Podcast>
    lateinit var includedFilters: LiveData<List<Playlist>>
    lateinit var availableFilters: LiveData<List<Playlist>>

    val globalSettings = settings.autoAddUpNextLimit
        .toFlowable(BackpressureStrategy.LATEST)
        .combineLatest(settings.autoAddUpNextLimitBehaviour.toFlowable(BackpressureStrategy.LATEST))
        .toLiveData()

    fun loadPodcast(uuid: String) {
        this.podcastUuid = uuid
        podcast = podcastManager
            .observePodcastByUuid(uuid)
            .subscribeOn(Schedulers.io())
            .toLiveData()

        val filters = playlistManager.observeAll().map {
            it.filter { filter -> filter.podcastUuidList.contains(uuid) }
        }
        includedFilters = filters.toLiveData()

        val availablePodcastFilters = playlistManager.observeAll().map {
            it.filter { filter -> !filter.allPodcasts }
        }
        availableFilters = availablePodcastFilters.toLiveData()
    }

    fun isAutoAddToUpNextOn(): Boolean {
        return !(podcast.value?.isAutoAddToUpNextOff ?: true)
    }

    fun updateAutoAddToUpNext(isOn: Boolean) {
        val podcast = this.podcast.value ?: return
        val value = if (isOn) Podcast.AutoAddUpNext.PLAY_LAST else Podcast.AutoAddUpNext.OFF
        launch {
            podcastManager.updateAutoAddToUpNext(podcast, value)
        }
    }

    fun updateAutoAddToUpNextOrder(value: Podcast.AutoAddUpNext) {
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
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_UNSUBSCRIBED,
                AnalyticsProp.podcastUnsubscribed(AnalyticsSource.PODCAST_SETTINGS, podcast.uuid)
            )
        }
    }

    fun filterSelectionChanged(newSelection: List<String>) {
        launch {
            podcastUuid?.let { podcastUuid ->
                playlistManager.findAll().forEach { playlist ->
                    val currentSelection = playlist.podcastUuidList.toMutableList()
                    val included = newSelection.contains(playlist.uuid)

                    val isUpdated =
                        if (included && !currentSelection.contains(podcastUuid)) {
                            currentSelection.add(podcastUuid)
                            true
                        } else if (!included && currentSelection.contains(podcastUuid)) {
                            currentSelection.remove(podcastUuid)
                            true
                        } else {
                            false
                        }

                    if (isUpdated) {
                        playlist.podcastUuidList = currentSelection
                        playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED
                        val userPlaylistUpdate = UserPlaylistUpdate(
                            listOf(PlaylistProperty.Podcasts),
                            PlaylistUpdateSource.PODCAST_SETTINGS
                        )
                        playlistManager.update(playlist, userPlaylistUpdate)
                    }
                }
            }
        }
    }

    private object AnalyticsProp {
        private const val SOURCE_KEY = "source"
        private const val UUID_KEY = "uuid"
        fun podcastUnsubscribed(source: AnalyticsSource, uuid: String) =
            mapOf(SOURCE_KEY to source.analyticsValue, UUID_KEY to uuid)
    }
}
