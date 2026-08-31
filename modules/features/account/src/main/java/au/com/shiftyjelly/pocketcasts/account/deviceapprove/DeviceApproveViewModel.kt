package au.com.shiftyjelly.pocketcasts.account.deviceapprove

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceApproveUiState())
    val uiState: StateFlow<DeviceApproveUiState> = _uiState.asStateFlow()

    private val wasSignedOutInitially = !syncManager.isLoggedIn()

    val shouldPromptUpsellAfterApproval get() = wasSignedOutInitially

    init {
        refreshAccountState()
    }

    fun setUserCode(userCode: String) {
        _uiState.update { it.copy(userCode = userCode) }
    }

    fun refreshAccountState() {
        _uiState.update { it.copy(isLoggedIn = syncManager.isLoggedIn(), email = syncManager.getEmail()) }
    }

    fun connect() {
        val userCode = _uiState.value.userCode
        if (userCode.isBlank() || _uiState.value.status == DeviceApproveStatus.Submitting) {
            return
        }
        _uiState.update { it.copy(status = DeviceApproveStatus.Submitting) }
        viewModelScope.launch {
            val status = try {
                syncManager.deviceApprove(userCode = userCode, approve = true)
                DeviceApproveStatus.Approved
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: HttpException) {
                if (ex.code() in EXPIRED_CODES) DeviceApproveStatus.ExpiredError else DeviceApproveStatus.GenericError
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to approve TV device")
                DeviceApproveStatus.GenericError
            }
            _uiState.update { it.copy(status = status) }
        }
    }

    private companion object {
        val EXPIRED_CODES = setOf(400, 410)
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
