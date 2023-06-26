package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.FeatureCardsState
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeButton.PlanType
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeFeatureCard
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.toUpgradeButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.toUpgradeFeatureCard
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
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
            val numPeriodFree: String?,
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
                    // Map product details to subscriptions
                    val subscriptions = (productDetailsState as? ProductDetailsState.Loaded)
                        ?.productDetails
                        ?.mapNotNull { details ->
                            val isFreeTrialEligible = subscriptionManager.isFreeTrialEligible(
                                SubscriptionMapper.mapProductIdToTier(details.productId)
                            )
                            Subscription.fromProductDetails(
                                productDetails = details,
                                isFreeTrialEligible = isFreeTrialEligible
                            )
                        } ?: emptyList()

                    // Get user's subscription status from cache
                    val cachedSubscriptionStatus = subscriptionManager.getCachedStatus()

                    // If the user is a patron, only show the patron subscription
                    val cachedTier = (cachedSubscriptionStatus as? SubscriptionStatus.Paid)?.tier
                    val filteredSubscriptions = if (cachedTier == SubscriptionTier.PATRON) {
                        subscriptions.filter { it.tier == Subscription.SubscriptionTier.PATRON }
                    } else {
                        subscriptions
                    }
                    val defaultSubscription = getDefaultSubscription(
                        filteredSubscriptions = filteredSubscriptions,
                        cachedTier = cachedTier,
                        cachedSubscriptionStatus = cachedSubscriptionStatus,
                    )

                    if (FeatureFlag.isEnabled(Feature.ADD_PATRON_ENABLED)) {
                        defaultSubscription?.let {
                            val upgradeButtons = filteredSubscriptions.map { it.tier }
                                .mapNotNull { productTier ->
                                    subscriptionManager.getDefaultSubscription(
                                        subscriptions = filteredSubscriptions,
                                        tier = productTier,
                                        frequency = getSubscriptionFrequency(cachedSubscriptionStatus)
                                    )?.toUpgradeButton(
                                        planType = getPlanType(
                                            productTier = productTier,
                                            cachedTier = cachedTier,
                                            cachedSubscriptionStatus = cachedSubscriptionStatus,
                                        )
                                    )
                                }

                            val currentTier = SubscriptionMapper
                                .mapProductIdToTier(defaultSubscription.productDetails.productId)

                            _state.value = State.Loaded(
                                featureCardsState = FeatureCardsState(
                                    subscriptions = filteredSubscriptions,
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

    private fun getDefaultSubscription(
        filteredSubscriptions: List<Subscription>,
        cachedTier: SubscriptionTier?,
        cachedSubscriptionStatus: SubscriptionStatus?,
    ) = subscriptionManager.getDefaultSubscription(
        subscriptions = filteredSubscriptions,
        tier = if (cachedTier == SubscriptionTier.PATRON) {
            Subscription.SubscriptionTier.PATRON
        } else {
            Subscription.SubscriptionTier.PLUS
        },
        frequency = getSubscriptionFrequency(cachedSubscriptionStatus)
    )

    private fun getSubscriptionFrequency(cachedSubscriptionStatus: SubscriptionStatus?) =
        if (cachedSubscriptionStatus is SubscriptionStatus.Paid) {
            when (cachedSubscriptionStatus.frequency) {
                SubscriptionFrequency.NONE -> SubscriptionFrequency.YEARLY
                SubscriptionFrequency.MONTHLY -> SubscriptionFrequency.MONTHLY
                SubscriptionFrequency.YEARLY -> SubscriptionFrequency.YEARLY
            }
        } else {
            SubscriptionFrequency.YEARLY
        }

    private fun getPlanType(
        productTier: Subscription.SubscriptionTier,
        cachedTier: SubscriptionTier?,
        cachedSubscriptionStatus: SubscriptionStatus?,
    ): PlanType {
        val productTierMatchesUserSubscriptionTier =
            productTier.name.lowercase() == cachedTier?.label?.lowercase()
        val isExpiring = if (productTierMatchesUserSubscriptionTier) {
            (cachedSubscriptionStatus as? SubscriptionStatus.Paid)?.isExpiring ?: false
        } else {
            false
        }
        return when {
            isExpiring -> PlanType.RENEW
            cachedSubscriptionStatus is SubscriptionStatus.Paid &&
                cachedTier == SubscriptionTier.PLUS &&
                productTier == Subscription.SubscriptionTier.PATRON -> PlanType.UPGRADE
            else -> PlanType.SUBSCRIBE
        }
    }
}
