package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfilePlusUpgradeBannerViewModel @Inject constructor(
    subscriptionManager: SubscriptionManager,
    app: Application,
) : AndroidViewModel(app) {

    data class State(val numPeriodFree: String?) {
        companion object {
            val EMPTY = State(null)
        }
    }

    private val _state = MutableStateFlow(State.EMPTY)
    val state = _state as StateFlow<State>

    init {
        val isFreeTrialEligible = subscriptionManager.isFreeTrialEligible()
        if (isFreeTrialEligible) {
            viewModelScope.launch {
                subscriptionManager
                    .observeProductDetails()
                    .asFlow()
                    .collect { productDetailsState ->
                        val subscriptions = (productDetailsState as? ProductDetailsState.Loaded)
                            ?.productDetails
                            ?.mapNotNull { details ->
                                Subscription.fromProductDetails(
                                    productDetails = details,
                                    isFreeTrialEligible = isFreeTrialEligible
                                )
                            } ?: emptyList()
                        val defaultSubscription = subscriptionManager.getDefaultSubscription(subscriptions)
                        if (defaultSubscription is Subscription.WithTrial) {
                            val numPeriodFree = defaultSubscription.trialPricingPhase.numPeriodFreeTrial(getApplication<Application>().resources)
                            _state.value = state.value.copy(
                                numPeriodFree = numPeriodFree.uppercase(Locale.getDefault())
                            )
                        }
                    }
            }
        }
    }
}
