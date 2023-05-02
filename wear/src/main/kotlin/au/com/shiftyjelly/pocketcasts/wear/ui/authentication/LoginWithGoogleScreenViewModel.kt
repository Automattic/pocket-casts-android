package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.horologist.auth.data.googlesignin.GoogleSignInEventListener
import com.google.android.horologist.auth.ui.googlesignin.signin.GoogleSignInViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginWithGoogleScreenViewModel @Inject constructor(
    googleSignInClient: GoogleSignInClient,
    googleSignInEventListener: GoogleSignInEventListener,
) : GoogleSignInViewModel(googleSignInClient, googleSignInEventListener)

class GoogleSignInEventListenerImpl @Inject constructor(
    private val syncManager: SyncManager,
    private val podcastManager: PodcastManager,
) : GoogleSignInEventListener {

    override suspend fun onSignedIn(account: GoogleSignInAccount) {
        account.idToken?.let { idToken ->
            syncManager.loginWithGoogle(idToken, SignInSource.WatchPhoneSync)
            podcastManager.refreshPodcastsAfterSignIn()
        }
    }
}
