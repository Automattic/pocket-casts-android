package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusBottomSheetState.Loaded
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusBottomSheetState.Loading
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingPlusBottomSheetState.NoSubscriptions
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.TrialSubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnboardingPlusBottomSheetViewModel @Inject constructor(
    private val subscriptionManager: SubscriptionManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {

    private val _state = MutableStateFlow<OnboardingPlusBottomSheetState>(Loading)
    val state: StateFlow<OnboardingPlusBottomSheetState> = _state
        .map {
            // If selected subscription has trialPricingPhase, update mostRecentlySelectedTrialPhase
            (it as? Loaded)?.selectedSubscription?.trialPricingPhase?.let { trialPhase ->
                it.copy(mostRecentlySelectedTrialPhase = trialPhase)
            } ?: it
        }.stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

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
                    _state.update { stateFromList(subscriptions) }
                }
        }
    }

    fun updateSelectedSubscription(subscription: Subscription) {
        val current = state.value
        when (current) {
            is Loaded -> {
                _state.update {
                    current.copy(
                        selectedSubscription = subscription,
                        purchaseFailed = false,
                    )
                }
            }
            else -> {
                LogBuffer.e(
                    LogBuffer.TAG_INVALID_STATE,
                    "Updating selected subscription without any available subscriptions. This should never happen."
                )
            }
        }
    }

    fun onClickSubscribe(
        activity: Activity,
        onComplete: () -> Unit,
    ) {
        (state.value as? Loaded)?.let { loadedState ->
            _state.update { loadedState.copy(purchaseFailed = false) }
            val subscription = loadedState.selectedSubscription
            viewModelScope.launch {
                val purchaseEvent = subscriptionManager
                    .observePurchaseEvents()
                    .asFlow()
                    .firstOrNull()

                when (purchaseEvent) {
                    PurchaseEvent.Success -> {
                        onComplete()
                    }
                    is PurchaseEvent.Cancelled -> {
                        // User cancelled subscription creation. Do nothing.
                    }
                    is PurchaseEvent.Failure -> {
                        _state.update { loadedState.copy(purchaseFailed = true) }
                    }
                    null -> {
                        Timber.e("Purchase event was null. This should never happen.")
                    }
                }

                if (purchaseEvent != null) {
                    CreateAccountViewModel.trackPurchaseEvent(subscription, purchaseEvent, analyticsTracker)
                }
            }
            subscriptionManager.launchBillingFlow(
                activity,
                subscription.productDetails,
                subscription.offerToken
            )
        }
    }

    private fun stateFromList(subscriptions: List<Subscription>): OnboardingPlusBottomSheetState {
        val defaultSelected = subscriptionManager.getDefaultSubscription(subscriptions)
        return if (defaultSelected == null) {
            NoSubscriptions
        } else {
            Loaded(
                subscriptions = subscriptions,
                selectedSubscription = defaultSelected,
                purchaseFailed = false,
            )
        }
    }
}

sealed class OnboardingPlusBottomSheetState {
    object Loading : OnboardingPlusBottomSheetState()
    object NoSubscriptions : OnboardingPlusBottomSheetState()
    data class Loaded constructor(
        val subscriptions: List<Subscription>, // This list should never be empty
        val selectedSubscription: Subscription,
        // Need to retain the most recently selected trial phase so that information is still available as
        // it animates out of view after a subscription without a trial phase is selected
        val mostRecentlySelectedTrialPhase: TrialSubscriptionPricingPhase? = null,
        val purchaseFailed: Boolean = false
    ) : OnboardingPlusBottomSheetState() {
        val showTrialInfo = selectedSubscription.trialPricingPhase != null

        init {
            if (subscriptions.isEmpty()) {
                LogBuffer.e(
                    LogBuffer.TAG_INVALID_STATE,
                    "Loaded subscription selection bottom sheet during onboarding with no subscriptions. This should never happen."
                )
            }
        }
    }
}
