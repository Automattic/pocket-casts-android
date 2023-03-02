package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.MutableLiveData
import au.com.shiftyjelly.pocketcasts.servers.account.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChangePwdViewModel
@Inject constructor(
    private val syncServerManager: SyncServerManager,
    private val syncAccountManager: SyncAccountManager
) : AccountViewModel() {

    val pwdCurrent = MutableLiveData<String>().apply { postValue("") }
    val pwdNew = MutableLiveData<String>().apply { postValue("") }
    val pwdConfirm = MutableLiveData<String>().apply { postValue("") }

    val changePasswordState = MutableLiveData<ChangePasswordState>().apply { value = ChangePasswordState.Empty }
    private val disposables = CompositeDisposable()

    private fun pwdCurrentValid(): Boolean {
        return isPasswordValid(pwdCurrent.value)
    }

    private fun pwdNewValid(): Boolean {
        return isPasswordValid(pwdNew.value)
    }

    private fun pwdConfirmValid(): Boolean {
        val pwd0 = pwdNew.value ?: ""
        val pwd1 = pwdConfirm.value ?: ""
        val isNewAndConfirmMatch = (pwd0.isNotEmpty() && pwd0 == pwd1)
        return isPasswordValid(pwdConfirm.value) && (isNewAndConfirmMatch)
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
            changePasswordState.postValue(ChangePasswordState.Empty)
        }
    }

    fun updatePwdCurrent(value: String) {
        pwdCurrent.value = value
        val invalid = !pwdCurrentValid()
        errorUpdate(ChangePasswordError.INVALID_PASSWORD_CURRENT, invalid, null)
    }

    fun updatePwdNew(value: String) {
        pwdNew.value = value
        val invalid = !pwdNewValid()
        errorUpdate(ChangePasswordError.INVALID_PASSWORD_NEW, invalid, null)
        val password = pwdConfirm.value ?: ""
        updatePwdConfirm(password)
    }

    fun updatePwdConfirm(value: String) {
        pwdConfirm.value = value
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
        val pwdCurrentString = pwdCurrent.value ?: ""
        val pwdNewString = pwdNew.value ?: ""
        val pwdConfirmString = pwdConfirm.value ?: ""
        if (pwdCurrentString.isEmpty() || pwdNewString.isEmpty() || pwdConfirmString.isEmpty()) {
            return
        }

        changePasswordState.postValue(ChangePasswordState.Loading)

        syncServerManager.pwdChange(pwdNewString, pwdCurrentString)
            .subscribeOn(Schedulers.io())
            .doOnSuccess { response ->
                val success = response.success ?: false
                if (success) {
                    // TODO can't implement this until we have a way to get the refresh token
//                    val refreshToken = response.refreshToken
//                    if (refreshToken != null) {
//                        syncAccountManager.setRefreshToken(refreshToken)
//                    }
                    changePasswordState.postValue(ChangePasswordState.Success("OK"))
                } else {
                    val errors = mutableSetOf(ChangePasswordError.SERVER)
                    changePasswordState.postValue(ChangePasswordState.Failure(errors, response.message))
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

enum class ChangePasswordError {
    INVALID_PASSWORD_CURRENT,
    INVALID_PASSWORD_NEW,
    INVALID_PASSWORD_CONFIRM,
    SERVER
}

sealed class ChangePasswordState {
    object Empty : ChangePasswordState()
    object Loading : ChangePasswordState()
    data class Success(val result: String) : ChangePasswordState()
    data class Failure(val errors: MutableSet<ChangePasswordError>, val message: String?) : ChangePasswordState()
}
