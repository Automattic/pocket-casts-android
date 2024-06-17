package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ChangePwdViewModel
@Inject constructor(
    private val syncManager: SyncManager,
) : AccountViewModel() {

    val passwordCurrent = MutableLiveData<String>().apply { postValue("") }
    val passwordNew = MutableLiveData<String>().apply { postValue("") }
    val passwordConfirm = MutableLiveData<String>().apply { postValue("") }

    private val changePasswordState = MutableStateFlow<ChangePasswordState>(ChangePasswordState.Empty)
    val state: StateFlow<ChangePasswordState> = changePasswordState

    private fun pwdCurrentValid(): Boolean {
        return isPasswordValid(passwordCurrent.value)
    }

    private fun pwdNewValid(): Boolean {
        return isPasswordValid(passwordNew.value)
    }

    private fun pwdConfirmValid(): Boolean {
        val pwd0 = passwordNew.value ?: ""
        val pwd1 = passwordConfirm.value ?: ""
        val isNewAndConfirmMatch = (pwd0.isNotEmpty() && pwd0 == pwd1)
        return isPasswordValid(passwordConfirm.value) && (isNewAndConfirmMatch)
    }

    private fun errorUpdate(error: ChangePasswordError, add: Boolean, message: String?) {
        val errors = mutableSetOf<ChangePasswordError>()
        when (val existingState = changePasswordState.value) {
            is ChangePasswordState.Failure -> {
                errors.addAll(existingState.errors)
            }
            else -> {}
        }
        if (add) errors.add(error) else errors.remove(error)
        if (errors.isNotEmpty()) {
            changePasswordState.value = ChangePasswordState.Failure(errors, message)
        } else {
            changePasswordState.value = ChangePasswordState.Empty
        }
    }

    fun updatePwdCurrent(value: String) {
        passwordCurrent.value = value
        val invalid = !pwdCurrentValid()
        errorUpdate(ChangePasswordError.INVALID_PASSWORD_CURRENT, invalid, null)
    }

    fun updatePwdNew(value: String) {
        passwordNew.value = value
        val invalid = !pwdNewValid()
        errorUpdate(ChangePasswordError.INVALID_PASSWORD_NEW, invalid, null)
        val password = passwordConfirm.value ?: ""
        updatePwdConfirm(password)
    }

    fun updatePwdConfirm(value: String) {
        passwordConfirm.value = value
        val invalid = !pwdConfirmValid()
        errorUpdate(ChangePasswordError.INVALID_PASSWORD_CONFIRM, invalid, null)
    }

    fun clearValues() {
        confirmationMessages.value = Pair("", "")
        updatePwdCurrent("")
        updatePwdNew("")
        updatePwdConfirm("")
    }

    fun clearServerError() {
        errorUpdate(ChangePasswordError.SERVER, false, null)
    }

    fun changePassword() {
        val pwdCurrentString = passwordCurrent.value ?: ""
        val pwdNewString = passwordNew.value ?: ""
        val pwdConfirmString = passwordConfirm.value ?: ""
        if (pwdCurrentString.isEmpty() || pwdNewString.isEmpty() || pwdConfirmString.isEmpty()) {
            return
        }

        changePasswordState.value = ChangePasswordState.Loading

        viewModelScope.launch {
            try {
                syncManager.updatePassword(
                    newPassword = pwdNewString,
                    oldPassword = pwdCurrentString,
                )
                changePasswordState.value = ChangePasswordState.Success("OK")
            } catch (ex: Exception) {
                Timber.e(ex, "Failed update password")
                changePasswordState.value = ChangePasswordState.Failure(errors = setOf(ChangePasswordError.SERVER), message = null)
            }
        }
    }
}

enum class ChangePasswordError {
    INVALID_PASSWORD_CURRENT,
    INVALID_PASSWORD_NEW,
    INVALID_PASSWORD_CONFIRM,
    SERVER,
}

sealed class ChangePasswordState {
    data object Empty : ChangePasswordState()
    data object Loading : ChangePasswordState()
    data class Success(val result: String) : ChangePasswordState()
    data class Failure(val errors: Set<ChangePasswordError>, val message: String?) : ChangePasswordState()
}
