package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class LoggingInScreenViewModel @Inject constructor(
    settings: Settings,
    private val syncManager: SyncManager,
) : ViewModel() {

    // The time that the most recent login notification was shown.
    private val logInNotificationShownMs: Long = System.currentTimeMillis()
    private val logInNotificationMinDuration = 5.seconds

    sealed class State(val email: String?) {
        object None : State(null)
        class Refreshing(email: String?) : State(email)
        class CompleteButDelaying(email: String?) : State(email)
        object RefreshComplete : State(null)
    }

    private val _state = MutableStateFlow(
        when (settings.getRefreshState()) {
            is RefreshState.Failed -> State.RefreshComplete
            RefreshState.Never -> State.None
            RefreshState.Refreshing -> State.Refreshing(syncManager.getEmail())
            is RefreshState.Success -> State.RefreshComplete
            null -> State.None
        }
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.refreshStateObservable
                .asFlow()
                .collect(::onRefreshStateChange)
        }
    }

    fun shouldClose(withMinimumDelay: Boolean): Boolean {
        val stateValue = state.value

        if (stateValue is State.None) {
            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Immediately closing LoggingInScreen because refresh is not in progress")
            return true
        }

        val shouldNotDelayOnceComplete = !withMinimumDelay && stateValue is State.CompleteButDelaying
        val delayHasPassed = stateValue is State.RefreshComplete
        return shouldNotDelayOnceComplete || delayHasPassed
    }

    private fun onRefreshStateChange(refreshState: RefreshState) {
        when (refreshState) {

            RefreshState.Refreshing -> {
                val email = state.value.email?.let {
                    syncManager.getEmail()
                }
                _state.value = State.Refreshing(email)
            }

            RefreshState.Never -> { /* Do nothing */ }

            is RefreshState.Failed -> {
                _state.value = State.RefreshComplete
            }

            is RefreshState.Success -> {
                viewModelScope.launch {
                    val email = state.value.email?.let {
                        syncManager.getEmail()
                    }
                    _state.value = State.CompleteButDelaying(email)

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
