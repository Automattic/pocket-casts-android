package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingLoginOrSignUpViewModel.Companion.AnalyticsProp
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentProvider
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.extensions.isGooglePlayServicesAvailableSuccess
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class OnboardingCreateAccountViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val analyticsTracker: AnalyticsTracker,
    private val subscriptionManager: SubscriptionManager,
    private val podcastManager: PodcastManager,
    private val experimentProvider: ExperimentProvider,
    @ApplicationContext context: Context,
) : AndroidViewModel(context as Application),
    CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    val showGoogleSignUp =
        Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID.isNotEmpty() &&
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailableSuccess(context)

    private val _stateFlow = MutableStateFlow(
        OnboardingCreateAccountState(
            noNetworkErrorMessage = getApplication<Application>().getString(LR.string.log_in_no_network),
        ),
    )
    val stateFlow: StateFlow<OnboardingCreateAccountState> = _stateFlow

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.CREATE_ACCOUNT_SHOWN)
    }

    fun onBackPressed() {
        analyticsTracker.track(AnalyticsEvent.CREATE_ACCOUNT_DISMISSED)
    }

    fun onSignUpEmailPressed(flow: OnboardingFlow) {
        analyticsTracker.track(
            AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
            mapOf(AnalyticsProp.flow(flow), AnalyticsProp.ButtonTapped.createAccount),
        )
    }

    fun updateEmail(email: String) {
        _stateFlow.update { it.copy(email = email.trim()) }
    }

    fun updatePassword(password: String) {
        _stateFlow.update { it.copy(password = password) }
    }

    fun createAccount(onAccountCreated: () -> Unit) {
        _stateFlow.update { it.copy(hasAttemptedLogIn = true, isNetworkAvailable = Network.isConnected(getApplication())) }

        val state = stateFlow.value
        if (!state.isEmailValid || !state.isPasswordValid || !state.isNetworkAvailable) {
            return
        }

        _stateFlow.update {
            it.copy(
                isCallInProgress = true,
                serverErrorMessage = null,
            )
        }

        subscriptionManager.clearCachedMembership()

        viewModelScope.launch {
            val result = syncManager.createUserWithEmailAndPassword(
                email = state.email,
                password = state.password,
            )
            when (result) {
                is LoginResult.Success -> {
                    podcastManager.refreshPodcastsAfterSignIn()
                    experimentProvider.refreshExperiments()
                    analyticsTracker.refreshMetadata()
                    experimentProvider.refreshExperiments()
                    onAccountCreated()
                }

                is LoginResult.Failed -> {
                    _stateFlow.update {
                        it.copy(
                            isCallInProgress = false,
                            serverErrorMessage = result.message,
                        )
                    }
                }
            }
        }
    }
}

data class OnboardingCreateAccountState(
    val email: String = "",
    val password: String = "",
    val serverErrorMessage: String? = null,
    private val noNetworkErrorMessage: String,
    private val hasAttemptedLogIn: Boolean = false,
    private val isCallInProgress: Boolean = false,
    val isNetworkAvailable: Boolean = true,
) {
    val isEmailValid = AccountViewModel.isEmailValid(email)
    val isPasswordValid = AccountViewModel.isPasswordValid(password)

    val showEmailError = hasAttemptedLogIn && !isEmailValid
    val showPasswordError = hasAttemptedLogIn && !isPasswordValid

    val errorMessage: String? = if (!hasAttemptedLogIn) {
        null
    } else if (!isNetworkAvailable) {
        noNetworkErrorMessage
    } else {
        serverErrorMessage
    }

    val enableSubmissionFields = !isCallInProgress
}
