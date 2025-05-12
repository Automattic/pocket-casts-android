package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.Util
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CreateAccountViewModel
@Inject constructor(
    private val syncManager: SyncManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
    private val podcastManager: PodcastManager,
) : AccountViewModel() {

    val upgradeMode = MutableLiveData<Boolean>()
    val subscriptionType = MutableLiveData<SubscriptionType>().apply { value = SubscriptionType.FREE }
    val newsletter = MutableLiveData<Boolean>().apply { postValue(false) }

    val createAccountState = MutableLiveData<CreateAccountState>().apply { value = CreateAccountState.CurrentlyValid }
    private val disposables = CompositeDisposable()
    var defaultSubscriptionType = SubscriptionType.FREE

    companion object {
        private const val SOURCE_KEY = "source"
        private const val ENABLED_KEY = "enabled"
    }

    private fun errorUpdate(error: CreateAccountError, add: Boolean) {
        val errors = mutableSetOf<CreateAccountError>()
        when (val existingState = createAccountState.value) {
            is CreateAccountState.Failure -> {
                errors.addAll(existingState.errors)
            }

            else -> {}
        }
        if (add) errors.add(error) else errors.remove(error)
        val newState = if (errors.isEmpty()) {
            CreateAccountState.CurrentlyValid
        } else {
            CreateAccountState.Failure(errors = errors, message = null)
        }
        // update the form state on the main thread if possible, when updating the email and password at the same time the state can be old when using postValue.
        if (Util.isOnMainThread()) {
            createAccountState.value = newState
        } else {
            createAccountState.postValue(newState)
        }
    }

    fun updateEmailRefresh() {
        val addError = !isEmailValid(email.value)
        errorUpdate(CreateAccountError.INVALID_EMAIL, addError)
    }

    fun updateEmail(value: String) {
        val valueClean = value.trim()
        email.value = valueClean
        val addError = !isEmailValid(valueClean)
        errorUpdate(CreateAccountError.INVALID_EMAIL, addError)
    }

    fun updatePassword(value: String) {
        password.value = value
        val addError = !isPasswordValid(value)
        errorUpdate(CreateAccountError.INVALID_PASSWORD, addError)
    }

    fun updatePasswordRefresh() {
        val addError = !isPasswordValid(password.value)
        errorUpdate(CreateAccountError.INVALID_PASSWORD, addError)
    }

    fun updateNewsletter(isChecked: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.NEWSLETTER_OPT_IN_CHANGED,
            mapOf(SOURCE_KEY to NewsletterSource.ACCOUNT_UPDATED.analyticsValue, ENABLED_KEY to isChecked),
        )
        newsletter.value = isChecked
        newsletter.value?.let {
            settings.marketingOptIn.set(it, updateModifiedAt = true)
        }
    }

    fun updateStateToFinished() {
        createAccountState.value = CreateAccountState.Finished
    }

    fun clearValues() {
        upgradeMode.value = false
        subscriptionType.value = defaultSubscriptionType
        newsletter.value = false
    }

    fun clearError(error: CreateAccountError) {
        errorUpdate(error, false)
    }

    fun currentStateHasError(error: CreateAccountError): Boolean {
        when (val state = createAccountState.value) {
            is CreateAccountState.Failure -> {
                return state.errors.contains(error)
            }

            else -> {}
        }
        return false
    }

    fun sendCreateAccount() {
        val emailString = email.value ?: ""
        val passwordString = password.value ?: ""
        if (emailString.isEmpty() || passwordString.isEmpty()) {
            return
        }

        createAccountState.postValue(CreateAccountState.AccountCreating)

        viewModelScope.launch {
            when (val result = syncManager.createUserWithEmailAndPassword(emailString, passwordString)) {
                is LoginResult.Success -> {
                    analyticsTracker.refreshMetadata()
                    podcastManager.refreshPodcastsAfterSignIn()
                    createAccountState.postValue(CreateAccountState.AccountCreated)
                }

                is LoginResult.Failed -> {
                    val message = result.message
                    val errors = mutableSetOf(CreateAccountError.CANNOT_CREATE_ACCOUNT)
                    createAccountState.postValue(CreateAccountState.Failure(errors, message))
                }
            }
        }
    }

    fun onCloseDoneForm() {
        analyticsTracker.track(AnalyticsEvent.ACCOUNT_UPDATED_DISMISSED)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}

enum class NewsletterSource(val analyticsValue: String) {
    ACCOUNT_UPDATED("account_updated"),
    PROFILE("profile"),
    WELCOME_NEW_ACCOUNT("welcome_new_account"),
}

enum class SubscriptionType(val value: String) {
    FREE("Free"),
    PLUS("Pocket Casts Plus"),
}

enum class CreateAccountError {
    INVALID_EMAIL,
    INVALID_PASSWORD,
    CANNOT_CREATE_ACCOUNT,
}

sealed class CreateAccountState {
    object CurrentlyValid : CreateAccountState()
    object AccountCreating : CreateAccountState()
    object AccountCreated : CreateAccountState()
    object SubscriptionCreated : CreateAccountState()
    object Finished : CreateAccountState()
    data class Failure(val errors: MutableSet<CreateAccountError>, val message: String?) : CreateAccountState()
}
