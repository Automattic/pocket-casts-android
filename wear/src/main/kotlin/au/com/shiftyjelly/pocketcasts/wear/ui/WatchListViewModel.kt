package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WatchListViewModel @Inject constructor(
    private val userManager: UserManager,
    private val playbackManager: PlaybackManager
) : ViewModel() {
    val signInState = userManager.getSignInState()

    fun signOut() {
        userManager.signOut(
            playbackManager = playbackManager,
            wasInitiatedByUser = true
        )
    }
}
