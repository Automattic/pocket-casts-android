package au.com.shiftyjelly.pocketcasts.profile.blogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.webfeeds.WebFeed
import au.com.shiftyjelly.pocketcasts.servers.webfeeds.WebFeedsService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class AddBlogViewModel @Inject constructor(
    private val webFeedsService: WebFeedsService,
    private val syncManager: SyncManager,
    private val podcastManager: PodcastManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Start)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private var findFeedsJob: Job? = null
    private var createFeedJob: Job? = null

    fun onUrlChange(url: String) {
        _url.value = url
    }

    fun onFindFeeds(url: String, onNavigateToPodcast: (String) -> Unit) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) {
            return
        }
        cancelJobs()
        findFeedsJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val feeds = webFeedsService.getFeeds(cleanUrl)
                when {
                    feeds.isEmpty() -> _uiState.value = UiState.Error(ErrorReason.NoFeedsFound)
                    feeds.size == 1 -> createFeed(webFeed = feeds.first(), onNavigateToPodcast = onNavigateToPodcast)
                    else -> _uiState.value = UiState.Pick(feeds)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.e(e, "Failed to find feeds for url: $cleanUrl")
                _uiState.value = UiState.Error(ErrorReason.NoInternet)
            } catch (e: Exception) {
                Timber.e(e, "Failed to find feeds for url: $cleanUrl")
                _uiState.value = UiState.Error(ErrorReason.Generic)
            }
        }
    }

    fun createFeed(webFeed: WebFeed, onNavigateToPodcast: (String) -> Unit) {
        createFeedJob?.cancel()
        createFeedJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = syncManager.createWebFeedPodcastOrThrow(webFeed.href)
                if (response.hasPodcast()) {
                    val podcastUuid = response.podcast.uuid
                    podcastManager.subscribeToPodcast(podcastUuid = podcastUuid, sync = true)
                    onNavigateToPodcast(podcastUuid)
                } else {
                    Timber.e("Timed out waiting for podcast creation for ${webFeed.href}")
                    _uiState.value = UiState.Error(ErrorReason.Generic)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to create feed for ${webFeed.href}")
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

    private fun cancelJobs() {
        findFeedsJob?.cancel()
        createFeedJob?.cancel()
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
        data class Error(val reason: ErrorReason) : UiState
    }

    enum class ErrorReason {
        NoInternet,
        NoFeedsFound,
        Generic,
    }
}
