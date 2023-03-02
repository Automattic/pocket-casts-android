package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.servers.account.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val syncAccountManager: SyncAccountManager,
    private val syncServerManager: SyncServerManager,
) : AccountViewModel() {

    val resetPasswordState = MutableLiveData<ResetPasswordState>().apply { value = ResetPasswordState.Empty }

    private fun errorUpdate(error: ResetPasswordError, add: Boolean, message: String?) {
        val errors = mutableSetOf<ResetPasswordError>()
        when (val existingState = resetPasswordState.value) {
            is ResetPasswordState.Failure -> {
                errors.addAll(existingState.errors)
            }
            else -> {}
        }
        if (add) errors.add(error) else errors.remove(error)
        if (errors.isNotEmpty()) {
            resetPasswordState.value = ResetPasswordState.Failure(errors, message)
        } else {
            resetPasswordState.postValue(ResetPasswordState.Empty)
        }
    }

    fun updateEmail(value: String) {
        email.value = value
        val addError = !isEmailValid(value)
        errorUpdate(ResetPasswordError.INVALID_EMAIL, addError, null)
    }

    fun clearValues() {
        updateEmail("")
    }

    fun clearServerError() {
        errorUpdate(ResetPasswordError.SERVER, false, null)
    }

    fun resetPassword() {
        val emailString = email.value ?: ""
        if (emailString.isEmpty()) {
            return
        }
        resetPasswordState.postValue(ResetPasswordState.Loading)

        viewModelScope.launch {
            syncAccountManager.forgotPassword(
                email = emailString,
                syncServerManager = syncServerManager,
                onSuccess = {
                    resetPasswordState.postValue(ResetPasswordState.Success)
                },
                onError = { message ->
                    val errors = mutableSetOf(ResetPasswordError.SERVER)
                    resetPasswordState.postValue(ResetPasswordState.Failure(errors, message))
                }
            )
        }
    }
}

enum class ResetPasswordError {
    INVALID_EMAIL,
    SERVER
}

sealed class ResetPasswordState {
    object Empty : ResetPasswordState()
    object Loading : ResetPasswordState()
    object Success : ResetPasswordState()
    data class Failure(val errors: MutableSet<ResetPasswordError>, val message: String?) : ResetPasswordState()
}
