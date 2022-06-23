package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountFragmentViewModel @Inject constructor(
    userManager: UserManager
) : ViewModel() {
    val signInState = LiveDataReactiveStreams.fromPublisher(userManager.getSignInState())
}
