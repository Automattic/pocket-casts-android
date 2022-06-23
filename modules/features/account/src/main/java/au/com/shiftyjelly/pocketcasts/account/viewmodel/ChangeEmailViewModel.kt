package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.MutableLiveData
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChangeEmailViewModel
@Inject constructor(
    private val syncServerManager: SyncServerManager,
    private val settings: Settings
) : AccountViewModel() {

    var existingEmail = settings.getSyncEmail()

    val changeEmailState = MutableLiveData<ChangeEmailState>().apply { value = ChangeEmailState.Empty }
    private val disposables = CompositeDisposable()

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
            changeEmailState.value = ChangeEmailState.Failure(errors, message)
        } else {
            changeEmailState.postValue(ChangeEmailState.Empty)
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
        existingEmail = settings.getSyncEmail()
        updateEmail("")
        updatePassword("")
    }

    fun clearServerError() {
        errorUpdate(ChangeEmailError.SERVER, false, null)
    }

    fun changeEmail() {
        val emailString = email.value ?: ""
        val pwdString = password.value ?: ""
        if (emailString.isEmpty() || pwdString.isEmpty()) {
            return
        }
        changeEmailState.postValue(ChangeEmailState.Loading)

        syncServerManager.emailChange(emailString, pwdString)
            .subscribeOn(Schedulers.io())
            .doOnSuccess { response ->
                val success = response.success ?: false
                if (success) {
                    settings.setSyncEmail(emailString)
                    existingEmail = emailString
                    changeEmailState.postValue(ChangeEmailState.Success("OK"))
                } else {
                    val errors = mutableSetOf(ChangeEmailError.SERVER)
                    changeEmailState.postValue(ChangeEmailState.Failure(errors, response.message))
                }
            }
            .subscribeBy(onError = { Timber.e(it) })
            .addTo(disposables)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

enum class ChangeEmailError {
    INVALID_EMAIL,
    INVALID_PASSWORD,
    SERVER
}

sealed class ChangeEmailState {
    object Empty : ChangeEmailState()
    object Loading : ChangeEmailState()
    data class Success(val result: String) : ChangeEmailState()
    data class Failure(val errors: MutableSet<ChangeEmailError>, val message: String?) : ChangeEmailState()
}
