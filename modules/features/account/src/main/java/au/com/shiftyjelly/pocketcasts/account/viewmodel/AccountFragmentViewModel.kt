package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountFragmentViewModel @Inject constructor(
    userManager: UserManager
) : ViewModel() {
    val signInState = userManager.getSignInState().toLiveData()
}
