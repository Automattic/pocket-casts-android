package au.com.shiftyjelly.pocketcasts.account.deviceapprove

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.automattic.eventhorizon.DeviceApproveConnectTappedEvent
import com.automattic.eventhorizon.DeviceApproveDismissedEvent
import com.automattic.eventhorizon.DeviceApproveFailedEvent
import com.automattic.eventhorizon.DeviceApproveShownEvent
import com.automattic.eventhorizon.DeviceApproveSuccessfulEvent
import com.automattic.eventhorizon.DeviceSetupAccountTappedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber

@HiltViewModel
class DeviceApproveViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val settings: Settings,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceApproveUiState())
    val uiState: StateFlow<DeviceApproveUiState> = _uiState.asStateFlow()

    private val wasSignedOutInitially = !syncManager.isLoggedIn()

    val shouldPromptUpsellAfterApproval get() = wasSignedOutInitially && settings.cachedSubscription.value == null

    init {
        refreshAccountState()
    }

    fun setUserCode(userCode: String) {
        _uiState.update { it.copy(userCode = userCode) }
    }

    fun refreshAccountState() {
        _uiState.update { it.copy(isLoggedIn = syncManager.isLoggedIn(), email = syncManager.getEmail()) }
    }

    fun onShown() {
        eventHorizon.track(DeviceApproveShownEvent())
    }

    fun onSetupAccountTapped() {
        eventHorizon.track(DeviceSetupAccountTappedEvent)
    }

    fun onDismissed() {
        if (_uiState.value.status != DeviceApproveStatus.Approved) {
            eventHorizon.track(DeviceApproveDismissedEvent)
        }
    }

    fun connect() {
        val userCode = _uiState.value.userCode
        if (userCode.isBlank() || _uiState.value.status == DeviceApproveStatus.Submitting) {
            return
        }
        eventHorizon.track(DeviceApproveConnectTappedEvent)
        _uiState.update { it.copy(status = DeviceApproveStatus.Submitting) }
        viewModelScope.launch {
            var errorCode: String? = null
            val status = try {
                syncManager.deviceApprove(userCode = userCode, approve = true)
                DeviceApproveStatus.Approved
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: HttpException) {
                Timber.e(ex, "Failed to approve TV device")
                if (ex.code() in EXPIRED_CODES) {
                    errorCode = INVALID_GRANT
                    DeviceApproveStatus.ExpiredError
                } else {
                    errorCode = ex.code().toString()
                    DeviceApproveStatus.GenericError
                }
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to approve TV device")
                errorCode = UNKNOWN_ERROR
                DeviceApproveStatus.GenericError
            }
            if (status == DeviceApproveStatus.Approved) {
                eventHorizon.track(DeviceApproveSuccessfulEvent)
            } else {
                eventHorizon.track(DeviceApproveFailedEvent(errorCode = errorCode ?: UNKNOWN_ERROR))
            }
            _uiState.update { it.copy(status = status) }
        }
    }

    private companion object {
        val EXPIRED_CODES = setOf(400, 410)
        const val INVALID_GRANT = "invalid_grant"
        const val UNKNOWN_ERROR = "unknown"
    }
}

data class DeviceApproveUiState(
    val userCode: String = "",
    val isLoggedIn: Boolean = false,
    val email: String? = null,
    val status: DeviceApproveStatus = DeviceApproveStatus.Idle,
)

enum class DeviceApproveStatus {
    Idle,
    Submitting,
    Approved,
    ExpiredError,
    GenericError,
}
