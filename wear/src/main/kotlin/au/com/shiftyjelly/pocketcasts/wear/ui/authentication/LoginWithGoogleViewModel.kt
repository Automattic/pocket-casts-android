package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.isGooglePlayServicesAvailableSuccess
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginWithGoogleViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val syncManager: SyncManager,
    @ApplicationContext context: Context,
) : ViewModel() {

    sealed interface State {
        data object Idle : State
        data class SignedInWithGoogle(
            val token: String,
        ) : State
        data object Failed : State
    }

    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    fun tryCredentialsManager(
        @ActivityContext context: Context,
    ) {
        val isGoogleSignInAvailable = Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID.isNotEmpty() &&
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailableSuccess(context)

        if (isGoogleSignInAvailable) {
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
                        context = context,
                    )
                    val credential = result.credential as CustomCredential
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        signInWithGoogleToken(
                            idToken = googleIdTokenCredential.idToken,
                        )
                    } else {
                        Log.w("====", "else")
                        LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to sign in with Google One Tap")
                        _state.value = State.Failed
                    }
                } catch (e: Exception) {
                    Log.w("====", "error $e")
                    LogBuffer.e(LogBuffer.TAG_CRASH, e, "Unable to sign in with Google One Tap")
                    _state.value = State.Failed
                }
            }
        } else {
            _state.value = State.Failed
        }
    }

    private suspend fun signInWithGoogleToken(
        idToken: String,
    ) {
        val authResult = syncManager.loginWithGoogle(idToken = idToken, signInSource = SignInSource.UserInitiated.Onboarding)
        onLoginFromPhoneResult(authResult)
    }

    private fun onLoginFromPhoneResult(loginResult: LoginResult) {
        when (loginResult) {
            is LoginResult.Failed -> {
                _state.value = State.Failed
            }

            is LoginResult.Success -> {
                viewModelScope.launch {
                    podcastManager.refreshPodcastsAfterSignIn()
                }
                _state.value = State.SignedInWithGoogle(token = loginResult.result.token.value)
            }
        }
    }
}