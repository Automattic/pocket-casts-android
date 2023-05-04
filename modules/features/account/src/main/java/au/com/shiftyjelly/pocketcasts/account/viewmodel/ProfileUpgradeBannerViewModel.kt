package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.BuildConfig
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.FeatureCardsState
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.toUpgradeFeatureCard
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
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
class ProfileUpgradeBannerViewModel @Inject constructor(
    subscriptionManager: SubscriptionManager,
    app: Application,
) : AndroidViewModel(app) {

    sealed class State(
        open val numPeriodFree: String?
    ) {
        data class Loaded(
            override val numPeriodFree: String?,
            val featureCardsState: FeatureCardsState,
        ) : State(numPeriodFree)

        data class OldLoaded(
            override val numPeriodFree: String?
        ) : State(numPeriodFree)

        object Empty : State(null)
    }

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Empty)
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
                        val numPeriodFree = if (defaultSubscription is Subscription.WithTrial) {
                            defaultSubscription.trialPricingPhase.numPeriodFreeTrial(getApplication<Application>().resources)
                        } else {
                            null
                        }
                        if (BuildConfig.ADD_PATRON_ENABLED) {
                            defaultSubscription?.let {
                                val currentTier = SubscriptionMapper.mapProductIdToTier(defaultSubscription.productDetails.productId)
                                _state.value = State.Loaded(
                                    numPeriodFree = numPeriodFree?.uppercase(Locale.getDefault()),
                                    featureCardsState = FeatureCardsState(
                                        subscriptions = subscriptions,
                                        currentFeatureCard = currentTier.toUpgradeFeatureCard()
                                    )
                                )
                            }
                        } else {
                            _state.value = State.OldLoaded(
                                numPeriodFree = numPeriodFree?.uppercase(Locale.getDefault())
                            )
                        }
                    }
            }
        }
    }
}
