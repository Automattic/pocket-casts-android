package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusBottomSheetState.Loaded
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusBottomSheetState.Loading
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingPlusBottomSheetViewModel @Inject constructor(
    subscriptionManager: SubscriptionManager
) : ViewModel() {

    private val _state = MutableStateFlow<OnboardingPlusBottomSheetState>(Loading)
    val state: StateFlow<OnboardingPlusBottomSheetState> = _state

    init {
        viewModelScope.launch {
            subscriptionManager
                .observeProductDetails()
                .asFlow()
                .stateIn(viewModelScope)
                .collect { productDetails ->
                    val subscriptions = when (productDetails) {
                        is ProductDetailsState.Error -> null
                        is ProductDetailsState.Loaded -> productDetails.productDetails.mapNotNull { productDetailsState ->
                            Subscription.fromProductDetails(
                                productDetails = productDetailsState,
                                isFreeTrialEligible = subscriptionManager.isFreeTrialEligible()
                            )
                        }
                    } ?: emptyList()
                    _state.update { Loaded(subscriptions) }
                }
        }
    }
}

sealed class OnboardingPlusBottomSheetState {
    object Loading : OnboardingPlusBottomSheetState()
    data class Loaded(
        val subscriptions: List<Subscription>
    ) : OnboardingPlusBottomSheetState()
}
