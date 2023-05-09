package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.BuildConfig
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.FeatureCardsState
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeFeatureCard
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.toUpgradeButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.toUpgradeFeatureCard
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
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
    private val subscriptionManager: SubscriptionManager,
    private val settings: Settings,
    app: Application,
) : AndroidViewModel(app) {

    sealed class State {
        data class Loaded(
            val featureCardsState: FeatureCardsState,
            val upgradeButtons: List<UpgradeButton>,
        ) : State()

        data class OldLoaded(
            val numPeriodFree: String?
        ) : State()

        object Loading : State()
    }

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Loading)
    val state = _state as StateFlow<State>

    init {
        viewModelScope.launch {
            subscriptionManager
                .observeProductDetails()
                .asFlow()
                .collect { productDetailsState ->
                    val isFreeTrialEligible = subscriptionManager.isFreeTrialEligible()
                    val subscriptions = (productDetailsState as? ProductDetailsState.Loaded)
                        ?.productDetails
                        ?.mapNotNull { details ->
                            Subscription.fromProductDetails(
                                productDetails = details,
                                isFreeTrialEligible = isFreeTrialEligible
                            )
                        } ?: emptyList()
                    val defaultSubscription = subscriptionManager.getDefaultSubscription(subscriptions)
                    if (BuildConfig.ADD_PATRON_ENABLED) {
                        defaultSubscription?.let {
                            val upgradeButtons = subscriptions.map { it.tier }
                                .mapNotNull { tier ->
                                    subscriptionManager.getDefaultSubscription(
                                        subscriptions = subscriptions,
                                        tier = tier,
                                        frequency = SubscriptionFrequency.YEARLY
                                    )?.toUpgradeButton()
                                }

                            val currentTier = SubscriptionMapper.mapProductIdToTier(defaultSubscription.productDetails.productId)
                            _state.value = State.Loaded(
                                featureCardsState = FeatureCardsState(
                                    subscriptions = subscriptions,
                                    currentFeatureCard = currentTier.toUpgradeFeatureCard()
                                ),
                                upgradeButtons = upgradeButtons
                            )
                        }
                    } else {
                        val numPeriodFree = if (defaultSubscription is Subscription.WithTrial) {
                            defaultSubscription.trialPricingPhase.numPeriodFreeTrial(getApplication<Application>().resources)
                        } else {
                            null
                        }
                        _state.value = State.OldLoaded(
                            numPeriodFree = numPeriodFree?.uppercase(Locale.getDefault())
                        )
                    }
                }
        }
    }

    fun onFeatureCardChanged(upgradeFeatureCard: UpgradeFeatureCard) {
        (_state.value as? State.Loaded)?.let {
            settings.setLastSelectedSubscriptionFrequency(SubscriptionFrequency.YEARLY)
            settings.setLastSelectedSubscriptionTier(upgradeFeatureCard.subscriptionTier)
        }
    }
}
