package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class UpNextViewModel @Inject constructor(
    val userManager: UserManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _isSignedInAsPaidUser = MutableStateFlow(false)
    val isSignedInAsPaidUser: StateFlow<Boolean> get() = _isSignedInAsPaidUser

    init {
        viewModelScope.launch(ioDispatcher) {
            userManager.getSignInState().asFlow().collect { signInState ->
                _isSignedInAsPaidUser.value = signInState.isSignedInAsPlusOrPatron
            }
        }
    }
}
