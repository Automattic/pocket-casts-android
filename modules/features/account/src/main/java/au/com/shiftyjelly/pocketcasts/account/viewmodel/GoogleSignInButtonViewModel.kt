@file:Suppress("DEPRECATION")

package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.isGooglePlayServicesAvailableSuccess
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class GoogleSignInButtonViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    @ApplicationContext private val context: Context,
    private val podcastManager: PodcastManager,
    private val syncManager: SyncManager,
    private val subscriptionManager: SubscriptionManager,
) : ViewModel() {

    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    companion object {
        fun showContinueWithGoogleButton(context: Context) = Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID.isNotEmpty() &&
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailableSuccess(context)
    }

    /**
     * Try to sign in with Google One Tap.
     */
    fun startGoogleOneTapSignIn(
        flow: OnboardingFlow?,
        onSuccess: (GoogleSignInState, Subscription?) -> Unit,
        onError: suspend () -> Unit,
        activity: Activity,
        onLegacySignInIntent: (Intent) -> Unit,
        event: AnalyticsEvent = AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
    ) {
        if (flow != null) {
            analyticsTracker.track(AnalyticsEvent.SSO_STARTED, mapOf("source" to "google"))

            analyticsTracker.track(
                event,
                mapOf(
                    OnboardingLoginOrSignUpViewModel.Companion.AnalyticsProp.flow(flow),
                    OnboardingLoginOrSignUpViewModel.Companion.AnalyticsProp.ButtonTapped.continueWithGoogle,
                ),
            )
        } else if (!Util.isAutomotive(context)) {
            throw IllegalArgumentException("OnboardingFlow must be provided for non-automotive devices")
        }

        viewModelScope.launch {
            try {
                val googleIdOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                    serverClientId = Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID,
                ).setNonce(UUID.randomUUID().toString()).build()

                val request: GetCredentialRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(
                    request = request,
                    context = activity,
                )
                val credential = result.credential as CustomCredential
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential
                        .createFrom(credential.data)

                    signInWithGoogleToken(
                        idToken = googleIdTokenCredential.idToken,
                        onSuccess = onSuccess,
                        onError = onError,
                    )
                } else {
                    LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to sign in with Google One Tap")
                    onError()
                }
            } catch (e: Exception) {
                if (e !is GetCredentialCancellationException) {
                    LogBuffer.e(LogBuffer.TAG_CRASH, e, "Unable to sign in with Google One Tap")

                    try {
                        launchLegacyGoogleSignIn(onLegacySignInIntent)
                    } catch (e: Exception) {
                        LogBuffer.e(LogBuffer.TAG_CRASH, e, "Failed to launch legacy Google login")
                        onError()
                    }
                }
            }
        }
    }

    private fun launchLegacyGoogleSignIn(onLegacySignInIntent: (Intent) -> Unit) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        onLegacySignInIntent(googleSignInClient.signInIntent)
    }

    /**
     * Handle the response from the legacy Google Sign-In intent.
     */
    fun onGoogleLegacySignInResult(result: ActivityResult, onSuccess: (GoogleSignInState, Subscription?) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val credential = Identity.getSignInClient(context).getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken ?: throw Exception("Unable to sign in because no token was returned.")
                signInWithGoogleToken(
                    idToken = idToken,
                    onSuccess = onSuccess,
                    onError = onError,
                )
            } catch (e: Exception) {
                LogBuffer.e(
                    LogBuffer.TAG_CRASH,
                    e,
                    "Unable to get sign in credentials from legacy Google Sign-In result.",
                )
                onError()
            }
        }
    }

    private suspend fun signInWithGoogleToken(
        idToken: String,
        onSuccess: (GoogleSignInState, Subscription?) -> Unit,
        onError: suspend () -> Unit,
    ) = when (val authResult = syncManager.loginWithGoogle(idToken = idToken, signInSource = SignInSource.UserInitiated.Onboarding)) {
        is LoginResult.Success -> {
            podcastManager.refreshPodcastsAfterSignIn()
            val subscription = subscriptionManager.fetchFreshSubscription()
            onSuccess(GoogleSignInState(isNewAccount = authResult.result.isNewAccount), subscription)
        }

        is LoginResult.Failed -> {
            onError()
        }
    }
}
