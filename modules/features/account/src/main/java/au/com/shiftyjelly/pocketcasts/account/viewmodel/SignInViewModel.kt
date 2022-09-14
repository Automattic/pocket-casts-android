package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.AccountAuth
import au.com.shiftyjelly.pocketcasts.account.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel
@Inject constructor(
    private val auth: AccountAuth,
    private val subscriptionManager: SubscriptionManager
) : AccountViewModel() {

    val signInState = MutableLiveData<SignInState>().apply { value = SignInState.Empty }

    private fun errorUpdate(error: SignInError, add: Boolean) {
        val errors = mutableSetOf<SignInError>()
        when (val existingState = signInState.value) {
            is SignInState.Failure -> {
                errors.addAll(existingState.errors)
            }
            else -> {}
        }
        if (add) errors.add(error) else errors.remove(error)
        if (errors.isEmpty()) {
            signInState.postValue(SignInState.Empty)
        } else {
            signInState.value = SignInState.Failure(errors = errors, message = null)
        }
    }

    fun updateEmail(value: String) {
        val valueClean = value.trim()
        email.value = valueClean
        val addError = !isEmailValid(valueClean)
        errorUpdate(SignInError.INVALID_EMAIL, addError)
    }

    fun updatePassword(value: String) {
        password.value = value
        val addError = !isPasswordValid(value)
        errorUpdate(SignInError.INVALID_PASSWORD, addError)
    }

    fun clearValues() {
        updateEmail("")
        updatePassword("")
    }

    fun clearServerError() {
        errorUpdate(SignInError.SERVER, false)
    }

    fun signIn() {
        val emailString = email.value
        val pwdString = password.value
        if (emailString.isNullOrEmpty() || pwdString.isNullOrEmpty()) {
            return
        }
        signInState.postValue(SignInState.Loading)

        subscriptionManager.clearCachedStatus()
        viewModelScope.launch {
            val result = auth.signInWithEmailAndPassword(emailString, pwdString, SignInSource.SignInViewModel)
            when (result) {
                is AccountAuth.AuthResult.Success -> {
                    signInState.postValue(SignInState.Success)
                }
                is AccountAuth.AuthResult.Failed -> {
                    val message = result.message
                    val errors = mutableSetOf(SignInError.SERVER)
                    signInState.postValue(SignInState.Failure(errors, message))
                }
            }
        }
    }
}

enum class SignInError {
    INVALID_EMAIL,
    INVALID_PASSWORD,
    SERVER
}

sealed class SignInState {
    object Empty : SignInState()
    object Loading : SignInState()
    object Success : SignInState()
    data class Failure(val errors: MutableSet<SignInError>, val message: String?) : SignInState()
}
