package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingLoginOrSignUpViewModel.Companion.AnalyticsProp
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentProvider
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
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
class OnboardingLogInViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val podcastManager: PodcastManager,
    private val subscriptionManager: SubscriptionManager,
    private val syncManager: SyncManager,
    private val experiments: ExperimentProvider,
    @ApplicationContext context: Context,
) : AndroidViewModel(context as Application),
    CoroutineScope {

    val showContinueWithGoogleButton =
        Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID.isNotEmpty() &&
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailableSuccess(context)

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val _state = MutableStateFlow(
        LogInState(
            noNetworkErrorMessage = getApplication<Application>().getString(LR.string.log_in_no_network),
        ),
    )
    val state: StateFlow<LogInState> = _state

    fun updateEmail(email: String) {
        _state.update { it.copy(email = email.trim()) }
    }

    fun updatePassword(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun onSignInButtonTapped(flow: OnboardingFlow) {
        analyticsTracker.track(
            AnalyticsEvent.SIGNIN_BUTTON_TAPPED,
            mapOf(AnalyticsProp.flow(flow), AnalyticsProp.ButtonTapped.signIn),
        )
    }

    fun onForgotPasswordTapped(flow: OnboardingFlow) {
        analyticsTracker.track(
            AnalyticsEvent.SIGNIN_FORGOT_PASSWORD_TAPPED,
            mapOf(AnalyticsProp.flow(flow)),
        )
    }

    fun logIn(onSuccessfulLogin: (Subscription?) -> Unit) {
        _state.update { it.copy(hasAttemptedLogIn = true, isNetworkAvailable = Network.isConnected(getApplication())) }

        val state = state.value
        if (!state.isEmailValid || !state.isPasswordValid || !state.isNetworkAvailable) {
            return
        }

        _state.update {
            it.copy(
                isCallInProgress = true,
                serverErrorMessage = null,
            )
        }

        subscriptionManager.clearCachedMembership()

        viewModelScope.launch {
            val result = syncManager.loginWithEmailAndPassword(
                email = state.email,
                password = state.password,
                signInSource = SignInSource.UserInitiated.Onboarding,
            )
            when (result) {
                is LoginResult.Success -> {
                    podcastManager.refreshPodcastsAfterSignIn()
                    experiments.refreshExperiments()
                    onSuccessfulLogin(subscriptionManager.fetchFreshSubscription())
                }

                is LoginResult.Failed -> {
                    _state.update {
                        it.copy(
                            isCallInProgress = false,
                            serverErrorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.SIGNIN_SHOWN)
    }

    fun onBackPressed() {
        analyticsTracker.track(AnalyticsEvent.SIGNIN_DISMISSED)
    }
}

data class LogInState(
    val email: String = "",
    val password: String = "",
    private val noNetworkErrorMessage: String,
    private val serverErrorMessage: String? = null,
    private val isCallInProgress: Boolean = false,
    private val hasAttemptedLogIn: Boolean = false,
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
