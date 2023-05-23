package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class RequirePlusViewModel @Inject constructor(
    userManager: UserManager
) : ViewModel() {

    data class State(
        val status: RequirePlusStatus? = null,
    )

    enum class RequirePlusStatus { Plus, Free, SignedOut }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userManager
                .getSignInState()
                .asFlow()
                .collect { signInState ->

                    val status = when (signInState) {
                        is SignInState.SignedIn -> when (signInState.subscriptionStatus) {
                            is SubscriptionStatus.Free,
                            SubscriptionStatus.NotSignedIn -> RequirePlusStatus.Free

                            is SubscriptionStatus.Plus -> RequirePlusStatus.Plus
                        }
                        is SignInState.SignedOut -> RequirePlusStatus.SignedOut
                    }

                    _state.value = State(status)
                }
        }
    }
}
