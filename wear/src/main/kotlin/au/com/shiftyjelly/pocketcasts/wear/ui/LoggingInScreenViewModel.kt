package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class LoggingInScreenViewModel @Inject constructor(
    podcastManager: PodcastManager,
    settings: Settings,
    private val syncManager: SyncManager,
) : ViewModel() {

    // The time that the most recent login notification was shown.
    private val logInNotificationShownMs: Long = System.currentTimeMillis()
    private val logInNotificationMinDuration = 5.seconds

    sealed class State(val email: String?) {
        class Refreshing(email: String?) : State(email)
        class CompleteButDelaying(email: String?) : State(email)
        object RefreshComplete : State(null)
    }

    private val _state = MutableStateFlow<State>(State.Refreshing(syncManager.getEmail()))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.refreshStateObservable
                .asFlow()
                .collect(::onRefreshStateChange)
        }
        viewModelScope.launch {
            podcastManager.refreshPodcastsAfterSignIn()
        }
    }

    private fun onRefreshStateChange(refreshState: RefreshState) {
        when (refreshState) {
            RefreshState.Never,
            RefreshState.Refreshing -> {
                val isRefreshingWithoutEmail = state.value.let { it is State.Refreshing && it.email == null }
                if (isRefreshingWithoutEmail) {
                    _state.value = State.Refreshing(syncManager.getEmail())
                }
            }

            is RefreshState.Failed,
            is RefreshState.Success -> {
                viewModelScope.launch {
                    _state.update {
                        val email = it.email ?: syncManager.getEmail()
                        State.CompleteButDelaying(email = email)
                    }

                    delayUntilMinDuration()
                    _state.value = State.RefreshComplete
                }
            }
        }
    }

    private suspend fun delayUntilMinDuration() {
        val notificationDuration = (System.currentTimeMillis() - logInNotificationShownMs).milliseconds

        if (notificationDuration < logInNotificationMinDuration) {
            val delayAmount = logInNotificationMinDuration - notificationDuration
            delay(delayAmount)
        }
    }
}
