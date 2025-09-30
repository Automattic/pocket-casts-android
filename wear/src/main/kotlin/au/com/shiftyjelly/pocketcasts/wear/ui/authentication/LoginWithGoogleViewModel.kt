package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.NoCredentialException
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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
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
            val name: String,
            val avatarUrl: String? = null,
        ) : State

        sealed interface Failed : State {
            data object GoogleLoginUnavailable : Failed
            data object Other : Failed
        }
    }

    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state = _state.asStateFlow()

    fun tryCredentialsManager(
        activity: Activity,
    ) {
        val isGoogleSignInAvailable = Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID.isNotEmpty() &&
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailableSuccess(activity)

        if (isGoogleSignInAvailable) {
            viewModelScope.launch {
                runCatching {
                    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
                        .setNonce(UUID.randomUUID().toString())
                        .build()

                    val request: GetCredentialRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .addCredentialOption(GetPasswordOption())
                        .build()

                    val result = credentialManager.getCredential(
                        request = request,
                        context = activity,
                    )
                    val credential = result.credential
                    when (credential) {
                        is CustomCredential -> {
                            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential = GoogleIdTokenCredential
                                    .createFrom(credential.data)

                                _state.value = State.SignedInWithGoogle(
                                    name = googleIdTokenCredential.givenName.orEmpty(),
                                    avatarUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                                )

                                signInWithGoogleToken(
                                    idToken = googleIdTokenCredential.idToken,
                                )
                            } else {
                                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to sign in with GetGoogleIdOption")
                                _state.value = State.Failed.Other
                            }
                        }
                        is PasswordCredential -> {
                            signInWithPassword(
                                email = credential.id,
                                password = credential.password,
                            )
                        }
                        else -> {
                            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to sign in with Google One Tap")
                            _state.value = State.Failed.Other
                        }
                    }
                }.onFailure {
                    LogBuffer.e(LogBuffer.TAG_CRASH, it, "Unable to sign in with Google One Tap")
                    _state.value = if (it is NoCredentialException) {
                        State.Failed.GoogleLoginUnavailable
                    } else {
                        State.Failed.Other
                    }
                }
            }
        } else {
            _state.value = State.Failed.GoogleLoginUnavailable
        }
    }

    private suspend fun signInWithGoogleToken(
        idToken: String,
    ) {
        val loginResult = syncManager.loginWithGoogle(idToken, SignInSource.UserInitiated.Watch)
        when (loginResult) {
            is LoginResult.Success -> {
                podcastManager.refreshPodcastsAfterSignIn()
            }
            is LoginResult.Failed -> {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Failed to login with Google: ${loginResult.message}")
                _state.value = State.Failed.Other
            }
        }
    }

    private suspend fun signInWithPassword(
        email: String,
        password: String,
    ) {
        val loginResult = syncManager.loginWithEmailAndPassword(email = email, password = password, signInSource = SignInSource.UserInitiated.Watch)
        when (loginResult) {
            is LoginResult.Success -> {
                podcastManager.refreshPodcastsAfterSignIn()
            }
            is LoginResult.Failed -> {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Failed to login with email and password: ${loginResult.message}")
                _state.value = State.Failed.Other
            }
        }
    }
}
