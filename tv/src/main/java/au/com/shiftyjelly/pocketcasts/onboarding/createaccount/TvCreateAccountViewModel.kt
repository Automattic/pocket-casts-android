package au.com.shiftyjelly.pocketcasts.onboarding.createaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInUiState
import au.com.shiftyjelly.pocketcasts.onboarding.signin.deviceAuthFlow
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.automattic.eventhorizon.CreateAccountShownEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.OnboardingFlowType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TvCreateAccountViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TvSignInUiState>(TvSignInUiState.Loading)
    val uiState: StateFlow<TvSignInUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        requestDeviceCode()
    }

    fun trackShown() {
        eventHorizon.track(CreateAccountShownEvent(flow = OnboardingFlowType.InitialOnboarding))
    }

    private fun requestDeviceCode() {
        pollingJob?.cancel()
        _uiState.value = TvSignInUiState.Loading
        pollingJob = viewModelScope.launch {
            deviceAuthFlow(syncManager, isNewAccount = true).collect { _uiState.value = it }
        }
    }

    fun retry() {
        requestDeviceCode()
    }
}
