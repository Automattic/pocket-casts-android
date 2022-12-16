package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import android.content.Context
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.BuildConfig
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.isGooglePlayServicesAvailableSuccess
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnboardingLoginOrSignUpViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext context: Context,
    private val podcastManager: PodcastManager
) : AndroidViewModel(context as Application) {

    val showContinueWithGoogleButton =
        BuildConfig.SINGLE_SIGN_ON_ENABLED &&
            Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID.isNotEmpty() &&
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailableSuccess(context)

    val randomPodcasts = mutableListOf<Podcast>()

    private val context: Context
        get() = getApplication()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            randomPodcasts.clear()
            randomPodcasts.addAll(podcastManager.findRandomPodcasts(limit = 6))
        }
    }

    /**
     * Try to sign in with Google One Tap.
     * It's common for the One Tap to fail so then try the legacy Google Sign-In.
     */
    fun startGoogleOneTapSignIn(
        flow: String,
        onSuccess: (IntentSenderRequest) -> Unit,
        onError: suspend () -> Unit,
    ) {
        analyticsTracker.track(
            AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
            mapOf(AnalyticsProp.flow(flow), AnalyticsProp.ButtonTapped.continueWithGoogle)
        )
        viewModelScope.launch {
            try {
                val beginSignInRequest = BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // use the Google Cloud credentials OAuth Server Client ID, not the Android Client ID.
                    .setServerClientId(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
                    // don't just show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
                val signInRequest = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(beginSignInRequest)
                    .build()
                val result = Identity.getSignInClient(context).beginSignIn(signInRequest).await()
                val intentRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                onSuccess(intentRequest)
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_CRASH, e, "Unable to sign in with Google One Tap")
                onError()
            }
        }
    }

    /**
     * Try to sign in with the legacy Google Sign-In.
     */
    suspend fun startGoogleLegacySignIn(onSuccess: (IntentSenderRequest) -> Unit, onError: () -> Unit) {
        try {
            Timber.i("Using legacy Google Sign-In")
            val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(context)
            val idToken = lastSignedInAccount?.idToken
            val email = lastSignedInAccount?.email
            if (idToken == null || email == null) {
                val request = GetSignInIntentRequest.builder().setServerClientId(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID).build()
                val signInIntent = Identity.getSignInClient(context).getSignInIntent(request).await()
                val intentSenderRequest = IntentSenderRequest.Builder(signInIntent.intentSender).build()
                onSuccess(intentSenderRequest)
            } else {
                signInWithGoogleToken(email = email, idToken = idToken)
            }
        } catch (ex: Exception) {
            LogBuffer.e(LogBuffer.TAG_CRASH, ex, "Unable to sign in with legacy Google Sign-In")
            onError()
        }
    }

    /**
     * Handle the response from the Google One Tap intent to sign in.
     */
    fun onGoogleOneTapSignInResult(
        result: ActivityResult,
        onSuccess: () -> Unit,
        onError: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                onGoogleSignInResult(result)
                onSuccess()
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
    fun onGoogleLegacySignInResult(result: ActivityResult, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                onGoogleSignInResult(result)
                onSuccess()
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_CRASH, e, "Unable to get sign in credentials from legacy Google Sign-In result.")
                onError()
            }
        }
    }

    fun onShown(flow: String) {
        analyticsTracker.track(
            AnalyticsEvent.SETUP_ACCOUNT_SHOWN,
            mapOf(AnalyticsProp.flow(flow))
        )
    }

    fun onDismiss(flow: String) {
        analyticsTracker.track(
            AnalyticsEvent.SETUP_ACCOUNT_DISMISSED,
            mapOf(AnalyticsProp.flow(flow))
        )
    }

    fun onSignUpClicked(flow: String) {
        analyticsTracker.track(
            AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
            mapOf(AnalyticsProp.flow(flow), AnalyticsProp.ButtonTapped.createAccount)
        )
    }

    fun onLoginClicked(flow: String) {
        analyticsTracker.track(
            AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
            mapOf(AnalyticsProp.flow(flow), AnalyticsProp.ButtonTapped.signIn)
        )
    }

    private suspend fun onGoogleSignInResult(result: ActivityResult) {
        val credential = Identity.getSignInClient(context).getSignInCredentialFromIntent(result.data)
        val idToken = credential.googleIdToken ?: throw Exception("Unable to sign in because no token was returned.")
        val email = credential.id
        signInWithGoogleToken(email = email, idToken = idToken)
    }

    private fun signInWithGoogleToken(email: String, idToken: String) {
        Timber.i("signInWithGoogleToken idToken: $idToken email: $email")
        // Todo send the Google ID token to the server, this will be done in a future change
        // accountAuth.signInWithGoogleToken(email = email, idToken = idToken)
    }

    companion object {
        private object AnalyticsProp {
            fun flow(s: String) = "flow" to s
            object ButtonTapped {
                private const val button = "button"
                val signIn = button to "sign_in"
                val createAccount = button to "create_account"
                val continueWithGoogle = button to "continue_with_google"
            }
        }
    }
}
