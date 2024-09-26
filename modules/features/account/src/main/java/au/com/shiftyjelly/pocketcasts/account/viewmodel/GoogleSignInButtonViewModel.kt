package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.PasswordCredential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.isGooglePlayServicesAvailableSuccess
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class GoogleSignInButtonViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    @ApplicationContext private val context: Context,
    private val podcastManager: PodcastManager,
    private val syncManager: SyncManager,
) : ViewModel() {

    companion object {
        fun showContinueWithGoogleButton(context: Context) =
            Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID.isNotEmpty() &&
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailableSuccess(context)
    }

    /**
     * Try to sign in with Google One Tap.
     * It's common for the One Tap to fail so then try the legacy Google Sign-In.
     */
    fun startCredentialManagerSignIn(
        flow: OnboardingFlow?,
        onSuccess: (GoogleSignInState) -> Unit,
        onError: suspend () -> Unit,
    ) {
        if (flow != null) {
            analyticsTracker.track(AnalyticsEvent.SSO_STARTED, mapOf("source" to "google"))

            analyticsTracker.track(
                AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
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
                val googleIdOption = GetGoogleIdOption.Builder()
                    // automatic sign-in when single credential matching the request, user has not explicitly signed out, and user hasn't disabled automatic sign-in
                    .setAutoSelectEnabled(false)
                    // use the Google Cloud credentials OAuth Server Client ID, not the Android Client ID.
                    .setServerClientId(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
                    // don't just show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
                val signInRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(
                    request = signInRequest,
                    context = context,
                )
                when (val credential = result.credential) {
                    // TODO Do we need Passkey
                    // Passkey credential
                    // is PublicKeyCredential -> {
                    //    // Share responseJson such as a GetCredentialResponse on your server to
                    //    // validate and authenticate
                    //    responseJson = credential.authenticationResponseJson
                    // }

                    // Password credential
                    is PasswordCredential -> {
                        // Send ID and password to your server to validate and authenticate.
                        val email = credential.id
                        val password = credential.password

                        val loginResult = syncManager.loginWithEmailAndPassword(
                            email = email,
                            password = password,
                            signInSource = SignInSource.UserInitiated.Onboarding,
                        )
                        when (loginResult) {
                            is LoginResult.Success -> {
                                podcastManager.refreshPodcastsAfterSignIn()
                                onSuccess(GoogleSignInState(isNewAccount = loginResult.result.isNewAccount))
                            }

                            is LoginResult.Failed -> {
                                onError()
                                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to sign in with email and password. ${loginResult.message}")
                            }
                        }
                    }

                    // GoogleIdToken credential
                    is CustomCredential -> {
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            // Use googleIdTokenCredential and extract the ID to validate and authenticate on your server.
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            signInWithGoogleToken(
                                idToken = googleIdTokenCredential.idToken,
                                onSuccess = onSuccess,
                                onError = onError,
                            )
                        }
                    }

                    else -> {
                        // Catch any unrecognized credential type here.
                        LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Unexpected type of credential ${credential::class.java}")
                    }
                }
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_CRASH, e, "Unable to sign in with Google One Tap")
                onError()
            }
        }
    }

    private suspend fun signInWithGoogleToken(
        idToken: String,
        onSuccess: (GoogleSignInState) -> Unit,
        onError: suspend () -> Unit,
    ) =
        when (val authResult = syncManager.loginWithGoogle(idToken = idToken, signInSource = SignInSource.UserInitiated.Onboarding)) {
            is LoginResult.Success -> {
                podcastManager.refreshPodcastsAfterSignIn()
                Timber.i("PHILIP new account? ${authResult.result.isNewAccount}")
                onSuccess(GoogleSignInState(isNewAccount = authResult.result.isNewAccount))
            }
            is LoginResult.Failed -> {
                onError()
            }
        }
}
