package au.com.shiftyjelly.pocketcasts.onboarding.signin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

fun interface SyncCompletionWaiter {
    suspend fun awaitCompletion()
}

@Module
@InstallIn(SingletonComponent::class)
object SyncCompletionModule {
    @Provides
    fun provideSyncCompletionWaiter(
        @ApplicationContext context: Context,
        podcastManager: PodcastManager,
    ): SyncCompletionWaiter = SyncCompletionWaiter {
        RefreshPodcastsTask.runNowSync(context, CoroutineScope(EmptyCoroutineContext))
        awaitSubscriptionQueueDrain(podcastManager)
    }

    private suspend fun awaitSubscriptionQueueDrain(podcastManager: PodcastManager) {
        var consecutiveEmptyChecks = 0
        var totalChecks = 0
        while (consecutiveEmptyChecks < REQUIRED_EMPTY_CHECKS && totalChecks < MAX_TOTAL_CHECKS) {
            delay(SUBSCRIPTION_POLL_INTERVAL_MS)
            if (podcastManager.isSubscribingToPodcasts()) {
                consecutiveEmptyChecks = 0
            } else {
                consecutiveEmptyChecks++
            }
            totalChecks++
        }
    }
}

private const val SUBSCRIPTION_POLL_INTERVAL_MS = 300L
private const val REQUIRED_EMPTY_CHECKS = 5
private const val MAX_TOTAL_CHECKS = 100 // 30 seconds max wait

@HiltViewModel
class TvSyncingViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val syncCompletionWaiter: SyncCompletionWaiter,
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
                syncCompletionWaiter.awaitCompletion()
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
