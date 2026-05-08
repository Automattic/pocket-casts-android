package au.com.shiftyjelly.pocketcasts.profile.blogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Start)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private var findFeedsJob: Job? = null

    fun onUrlChange(url: String) {
        _url.value = url
    }

    fun onFindFeeds(url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) {
            return
        }
        findFeedsJob?.cancel()
        findFeedsJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val feeds = webFeedsService.getFeeds(cleanUrl)
                _uiState.value = when {
                    feeds.isEmpty() -> UiState.Error(ErrorReason.NoFeedsFound)
                    feeds.size == 1 -> UiState.Found(feeds.first())
                    else -> UiState.Pick(feeds)
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

    fun resetToStart() {
        findFeedsJob?.cancel()
        _uiState.value = UiState.Start
        _url.value = ""
    }

    fun editUrl() {
        findFeedsJob?.cancel()
        _uiState.value = UiState.Start
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
        data class Found(val feed: WebFeed) : UiState
        data class Pick(val feeds: List<WebFeed>) : UiState
        data class Error(val reason: ErrorReason) : UiState
    }

    enum class ErrorReason {
        NoInternet,
        NoFeedsFound,
        Generic,
    }
}
