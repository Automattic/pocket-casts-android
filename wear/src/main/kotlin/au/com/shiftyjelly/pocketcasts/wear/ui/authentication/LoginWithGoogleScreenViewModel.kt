package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import android.net.Uri
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.horologist.auth.data.googlesignin.GoogleSignInEventListener
import com.google.android.horologist.auth.ui.googlesignin.signin.GoogleSignInViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LoginWithGoogleScreenViewModel @Inject constructor(
    googleSignInClient: GoogleSignInClient,
    private val podcastManager: PodcastManager,
    private val syncManager: SyncManager,
) : ViewModel(), GoogleSignInEventListener {

    data class State(
        val googleSignInViewModel: GoogleSignInViewModel,
        val firstName: String?,
        val avatarUri: Uri?,
    )

    private val _state = MutableStateFlow(
        State(
            googleSignInViewModel = GoogleSignInViewModel(googleSignInClient, this),
            firstName = null,
            avatarUri = null,
        )
    )
    val state = _state.asStateFlow()

    override suspend fun onSignedIn(account: GoogleSignInAccount) {
        _state.update {
            it.copy(
                firstName = account.givenName,
                avatarUri = account.photoUrl,
            )
        }

        account.idToken?.let { idToken ->
            val loginResult = syncManager.loginWithGoogle(idToken, SignInSource.WatchPhoneSync)
            when (loginResult) {
                is LoginResult.Failed -> {
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Failed to login with Google: ${loginResult.message}")
                }
                is LoginResult.Success -> {
                    podcastManager.refreshPodcastsAfterSignIn()
                }
            }
        }
    }
}
