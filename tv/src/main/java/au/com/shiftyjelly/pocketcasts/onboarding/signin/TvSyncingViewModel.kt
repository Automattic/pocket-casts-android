package au.com.shiftyjelly.pocketcasts.onboarding.signin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class TvSyncingViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TvSyncingUiState())
    val uiState: StateFlow<TvSyncingUiState> = _uiState.asStateFlow()

    init {
        observePodcasts()
        startSync()
    }

    private fun observePodcasts() {
        viewModelScope.launch {
            podcastManager.findSubscribedFlow()
                .collect { podcasts ->
                    _uiState.update { state ->
                        state.copy(podcastUuids = podcasts.map { it.uuid })
                    }
                }
        }
    }

    private fun startSync() {
        viewModelScope.launch {
            val minimumDisplayTime = launch { delay(MIN_DISPLAY_TIME_MS) }
            try {
                podcastManager.refreshPodcastsAfterSignIn()
                RefreshPodcastsTask.runNowSync(context, viewModelScope)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Sync failed during onboarding")
            }
            minimumDisplayTime.join()
            _uiState.update { it.copy(syncComplete = true) }
        }
    }

    companion object {
        internal const val MIN_DISPLAY_TIME_MS = 3_000L
    }
}

data class TvSyncingUiState(
    val podcastUuids: List<String> = emptyList(),
    val syncComplete: Boolean = false,
)
