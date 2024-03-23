package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ChangeEmailViewModel
@Inject constructor(
    private val syncManager: SyncManager,
) : AccountViewModel() {

    var existingEmail = syncManager.getEmail()

    private val _changeEmailState = mutableStateOf<ChangeEmailState>(ChangeEmailState.Empty)
    val changeEmailState: State<ChangeEmailState> = _changeEmailState

    private fun errorUpdate(error: ChangeEmailError, add: Boolean, message: String?) {
        val errors = mutableSetOf<ChangeEmailError>()
        when (val existingState = changeEmailState.value) {
            is ChangeEmailState.Failure -> {
                errors.addAll(existingState.errors)
            }
            else -> {}
        }
        if (add) errors.add(error) else errors.remove(error)
        if (errors.isNotEmpty()) {
            _changeEmailState.value = ChangeEmailState.Failure(errors, message)
        } else {
            _changeEmailState.value = ChangeEmailState.Empty
        }
    }

    fun updateEmail(value: String) {
        email.value = value
        val addError = !isEmailValid(value)
        errorUpdate(ChangeEmailError.INVALID_EMAIL, addError, null)
    }

    fun updatePassword(value: String) {
        password.value = value
        val addError = !isPasswordValid(value)
        errorUpdate(ChangeEmailError.INVALID_PASSWORD, addError, null)
    }

    fun clearValues() {
        confirmationMessages.value = Pair("", "")
        existingEmail = syncManager.getEmail()
        updateEmail("")
        updatePassword("")
    }

    fun clearServerError() {
        errorUpdate(ChangeEmailError.SERVER, false, null)
    }

    fun changeEmail() {
        viewModelScope.launch {
            val emailString = email.value ?: ""
            val pwdString = password.value ?: ""
            if (emailString.isEmpty() || pwdString.isEmpty()) {
                return@launch
            }
            _changeEmailState.value = ChangeEmailState.Loading
            try {
                withContext(Dispatchers.IO) {
                    val response = syncManager.emailChange(emailString, pwdString)
                    val success = response.success ?: false
                    if (success) {
                        existingEmail = emailString
                        _changeEmailState.value = ChangeEmailState.Success("OK")
                    } else {
                        val errors = mutableSetOf(ChangeEmailError.SERVER)
                        _changeEmailState.value = ChangeEmailState.Failure(errors, response.message)
                    }
                }
            } catch (e: Exception) {
                _changeEmailState.value = ChangeEmailState.Failure(mutableSetOf(ChangeEmailError.SERVER), e.message)
            }
        }
    }
}

enum class ChangeEmailError {
    INVALID_EMAIL,
    INVALID_PASSWORD,
    SERVER,
}

sealed class ChangeEmailState {
    object Empty : ChangeEmailState()
    object Loading : ChangeEmailState()
    data class Success(val result: String) : ChangeEmailState()
    data class Failure(val errors: MutableSet<ChangeEmailError>, val message: String?) : ChangeEmailState()
}
