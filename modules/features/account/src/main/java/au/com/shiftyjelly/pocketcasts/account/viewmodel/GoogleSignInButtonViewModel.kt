package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
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
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import timber.log.Timber

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
     * It's common for the One Tap to fail so then try the legacy Google Sign-In.
     */
    fun startGoogleOneTapSignIn(
        flow: OnboardingFlow?,
        //onSuccess: (IntentSenderRequest) -> Unit,
        onSuccess: (GoogleSignInState, Subscription?) -> Unit,
        onError: suspend () -> Unit,
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
                    serverClientId = Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID
                ).setNonce(UUID.randomUUID().toString()).build()

                val request: GetCredentialRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )
                when (val credential = result.credential) {
                    is CustomCredential -> {
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            try {
                                val googleIdTokenCredential = GoogleIdTokenCredential
                                    .createFrom(credential.data)

                                // TODO do magic here!
                                signInWithGoogleToken(
                                    idToken = googleIdTokenCredential.idToken,
                                    onSuccess = onSuccess,
                                    onError = onError
                                )

                            } catch (e: GoogleIdTokenParsingException) {
                                LogBuffer.e(LogBuffer.TAG_CRASH, "Received an invalid google id token response", e)
                            }
                        } else {
                            // Catch any unrecognized custom credential type here.
                        }
                    }

                    else -> {}
                }
//                val beginSignInRequest = BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
//                    .setSupported(true)
//                    // use the Google Cloud credentials OAuth Server Client ID, not the Android Client ID.
//                    .setServerClientId(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
//                    // don't just show accounts previously used to sign in.
//                    .setFilterByAuthorizedAccounts(false)
//                    .build()
//                val signInRequest = BeginSignInRequest.builder()
//                    .setGoogleIdTokenRequestOptions(beginSignInRequest)
//                    .build()
//                val result = Identity.getSignInClient(context).beginSignIn(signInRequest).await()
//                val intentRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
//                onSuccess(intentRequest)
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_CRASH, e, "Unable to sign in with Google One Tap")
                onError()
            }
        }
    }

    /**
     * Try to sign in with the legacy Google Sign-In.
     */
    fun startGoogleLegacySignIn(
        onSuccess: (IntentSenderRequest) -> Unit,
        onError: () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                Timber.i("Using legacy Google Sign-In")
//                val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(context)
//                val idToken = lastSignedInAccount?.idToken
//                val email = lastSignedInAccount?.email
//                if (idToken == null || email == null) {
//                    val request = GetSignInIntentRequest.builder().setServerClientId(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID).build()
//                    val signInIntent = Identity.getSignInClient(context).getSignInIntent(request).await()
//                    val intentSenderRequest = IntentSenderRequest.Builder(signInIntent.intentSender).build()
//                    onSuccess(intentSenderRequest)
//                } else {
//                    signInWithGoogleToken(
//                        idToken = idToken,
//                        onSuccess = { _, _ -> },
//                        onError = onError,
//                    )
//                }
            } catch (ex: Exception) {
                LogBuffer.e(LogBuffer.TAG_CRASH, ex, "Unable to sign in with legacy Google Sign-In")
                onError()
            }
        }
    }

    /**
     * Handle the response from the Google One Tap intent to sign in.
     */
    fun onGoogleOneTapSignInResult(
        result: ActivityResult,
        onSuccess: (GoogleSignInState, Subscription?) -> Unit,
        onError: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                onGoogleSignInResult(
                    result = result,
                    onSuccess = onSuccess,
                    onError = {
                        onError()
                    },
                )
            } catch (e: Exception) {
                if (e is ApiException && e.statusCode == CommonStatusCodes.CANCELED) {
                    // user declined to sign in
                    return@launch
                }
                LogBuffer.e(LogBuffer.TAG_CRASH, e, "Unable to get sign in credentials from Google One Tap result.")
                onError()
            }
        }
    }

    /**
     * Handle the response from the legacy Google Sign-In intent.
     */
    fun onGoogleLegacySignInResult(result: ActivityResult, onSuccess: (GoogleSignInState, Subscription?) -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                onGoogleSignInResult(
                    result = result,
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

    private suspend fun onGoogleSignInResult(
        result: ActivityResult,
        onSuccess: (GoogleSignInState, Subscription?) -> Unit,
        onError: suspend () -> Unit,
    ) {
//        val credential = Identity.getSignInClient(context).getSignInCredentialFromIntent(result.data)
//        val idToken = credential.googleIdToken ?: throw Exception("Unable to sign in because no token was returned.")
//        signInWithGoogleToken(
//            idToken = idToken,
//            onSuccess = onSuccess,
//            onError = onError,
//        )
    }

    private suspend fun signInWithGoogleToken(
        idToken: String,
        onSuccess: (GoogleSignInState, Subscription?) -> Unit,
        onError: suspend () -> Unit,
    ) = when (val authResult = syncManager.loginWithGoogle(idToken = idToken, signInSource = SignInSource.UserInitiated.Onboarding)) {
        is LoginResult.Success -> {
            podcastManager.refreshPodcastsAfterSignIn()
            val subscritpion = subscriptionManager.fetchFreshSubscription()
            onSuccess(GoogleSignInState(isNewAccount = authResult.result.isNewAccount), subscritpion)
        }

        is LoginResult.Failed -> {
            onError()
        }
    }
}
