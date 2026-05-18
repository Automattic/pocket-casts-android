package au.com.shiftyjelly.pocketcasts.profile.blogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.webfeeds.WebFeed
import au.com.shiftyjelly.pocketcasts.servers.webfeeds.WebFeedsService
import com.automattic.eventhorizon.BlogsCreateFailedEvent
import com.automattic.eventhorizon.BlogsFeedSelectedEvent
import com.automattic.eventhorizon.BlogsFeedsFoundEvent
import com.automattic.eventhorizon.BlogsFindFeedsFailedEvent
import com.automattic.eventhorizon.BlogsFindFeedsFailureReason
import com.automattic.eventhorizon.BlogsFindFeedsTappedEvent
import com.automattic.eventhorizon.BlogsRetryTappedEvent
import com.automattic.eventhorizon.BlogsSubscribedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class AddBlogViewModel @Inject constructor(
    private val webFeedsService: WebFeedsService,
    private val syncManager: SyncManager,
    private val podcastManager: PodcastManager,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Start)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _podcastNavigationEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val podcastNavigationEvents: SharedFlow<String> = _podcastNavigationEvents.asSharedFlow()

    private var findFeedsJob: Job? = null
    private var createFeedJob: Job? = null
    private var pollEpisodesJob: Job? = null

    fun onUrlChange(url: String) {
        _url.value = url
    }

    fun onFindFeedsTapped(url: String) {
        eventHorizon.track(BlogsFindFeedsTappedEvent)
        findFeeds(url = url)
    }

    fun onRetryTapped(url: String) {
        eventHorizon.track(BlogsRetryTappedEvent)
        findFeeds(url = url)
    }

    private fun findFeeds(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) {
            return
        }
        cancelJobs()
        findFeedsJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val feeds = webFeedsService.getFeeds(cleanUrl)
                eventHorizon.track(BlogsFeedsFoundEvent(feedCount = feeds.size.toLong()))
                when {
                    feeds.isEmpty() -> _uiState.value = UiState.Error(ErrorReason.NoFeedsFound)
                    feeds.size == 1 -> createFeed(webFeed = feeds.first())
                    else -> _uiState.value = UiState.Pick(feeds)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.e(e, "Failed to find feeds for url: $cleanUrl")
                eventHorizon.track(BlogsFindFeedsFailedEvent(reason = BlogsFindFeedsFailureReason.NoInternet))
                _uiState.value = UiState.Error(ErrorReason.NoInternet)
            } catch (e: Exception) {
                Timber.e(e, "Failed to find feeds for url: $cleanUrl")
                eventHorizon.track(BlogsFindFeedsFailedEvent(reason = BlogsFindFeedsFailureReason.Generic))
                _uiState.value = UiState.Error(ErrorReason.Generic)
            }
        }
    }

    fun onFeedSelected(webFeed: WebFeed) {
        eventHorizon.track(BlogsFeedSelectedEvent)
        createFeed(webFeed = webFeed)
    }

    fun createFeed(webFeed: WebFeed) {
        createFeedJob?.cancel()
        createFeedJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = syncManager.createWebFeedPodcast(webFeed.href)
                if (response.hasPodcast()) {
                    val podcastUuid = response.podcast.uuid
                    podcastManager.subscribeToPodcast(podcastUuid = podcastUuid, sync = true)
                    eventHorizon.track(BlogsSubscribedEvent(uuid = podcastUuid))
                    val podcast = awaitSubscribedPodcast(podcastUuid)
                    // jump straight to the podcast page if the subscribe is slow or didn't work
                    if (podcast == null) {
                        _podcastNavigationEvents.tryEmit(podcastUuid)
                    } else {
                        val episodeCount = podcastManager.countEpisodesByPodcast(podcastUuid)
                        _uiState.value = UiState.Found(
                            webFeed = webFeed,
                            podcastUuid = podcastUuid,
                            episodeCount = episodeCount,
                        )
                        if (episodeCount == 0) {
                            pollForFirstEpisode(podcast = podcast, webFeed = webFeed)
                        }
                    }
                } else {
                    Timber.e("Timed out waiting for podcast creation for ${webFeed.href}")
                    eventHorizon.track(BlogsCreateFailedEvent)
                    _uiState.value = UiState.Error(ErrorReason.Generic)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to create feed for ${webFeed.href}")
                eventHorizon.track(BlogsCreateFailedEvent)
                _uiState.value = UiState.Error(ErrorReason.Generic)
            }
        }
    }

    fun resetToStart() {
        cancelJobs()
        _uiState.value = UiState.Start
        _url.value = ""
    }

    fun editUrl() {
        cancelJobs()
        _uiState.value = UiState.Start
    }

    private suspend fun awaitSubscribedPodcast(podcastUuid: String): Podcast? {
        repeat(10) {
            val podcast = podcastManager.findPodcastByUuid(podcastUuid)
            if (podcast != null) return podcast
            delay(1000L)
        }
        return null
    }

    private fun pollForFirstEpisode(podcast: Podcast, webFeed: WebFeed) {
        pollEpisodesJob?.cancel()
        pollEpisodesJob = viewModelScope.launch {
            repeat(POLL_FIRST_EPISODE_MAX_ATTEMPTS) {
                if (!isActive) return@launch
                delay(POLL_FIRST_EPISODE_INTERVAL_MS)
                try {
                    podcastManager.refreshPodcastFeed(podcast)
                    val count = podcastManager.countEpisodesByPodcast(podcast.uuid)
                    if (count > 0) {
                        _uiState.value = UiState.Found(
                            webFeed = webFeed,
                            podcastUuid = podcast.uuid,
                            episodeCount = count,
                        )
                        _podcastNavigationEvents.tryEmit(podcast.uuid)
                        return@launch
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Polling for first episode failed for ${podcast.uuid}")
                }
            }
            // when waiting for too long, take the user to the podcast page
            _podcastNavigationEvents.tryEmit(podcast.uuid)
        }
    }

    private fun cancelJobs() {
        findFeedsJob?.cancel()
        createFeedJob?.cancel()
        pollEpisodesJob?.cancel()
    }

    fun onBackPressed(): Boolean {
        return if (_uiState.value != UiState.Start) {
            resetToStart()
            true
        } else {
            false
        }
    }

    sealed interface UiState {
        data object Start : UiState
        data object Loading : UiState
        data class Pick(val feeds: List<WebFeed>) : UiState
        data class Found(
            val webFeed: WebFeed,
            val podcastUuid: String,
            val episodeCount: Int,
        ) : UiState
        data class Error(val reason: ErrorReason) : UiState
    }

    enum class ErrorReason {
        NoInternet,
        NoFeedsFound,
        Generic,
    }

    companion object {
        private const val POLL_FIRST_EPISODE_INTERVAL_MS = 20_000L
        private const val POLL_FIRST_EPISODE_MAX_ATTEMPTS = 15
    }
}
