package au.com.shiftyjelly.pocketcasts.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val userManager: UserManager,
    private val paymentClient: PaymentClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val subscriptionPlans = paymentClient.loadSubscriptionPlans().getOrNull()
            val trialOffer = subscriptionPlans?.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Monthly, SubscriptionOffer.Trial)
            _uiState.update { state ->
                state.copy(isFreeTrialAvailable = trialOffer != null)
            }
        }
        viewModelScope.launch {
            userManager.getSignInState().asFlow().collect { signInState ->
                _uiState.update { state ->
                    state.copy(isPlusUser = signInState.isSignedInAsPlusOrPatron)
                }
            }
        }
    }
}

data class ChatUiState(
    val isPlusUser: Boolean = false,
    val isFreeTrialAvailable: Boolean = false,
) {
    val isPaywallVisible get() = !isPlusUser
}
