package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.FeatureCardsState
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeFeatureCard
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeLayout
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.toUpgradeButton
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.toUpgradeFeatureCard
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Experiment
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentProvider
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Variation
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber

@HiltViewModel
class OnboardingUpgradeFeaturesViewModel @Inject constructor(
    app: Application,
    private val analyticsTracker: AnalyticsTracker,
    private val subscriptionManager: SubscriptionManager,
    private val settings: Settings,
    private val experimentProvider: ExperimentProvider,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app) {

    private val _state: MutableStateFlow<OnboardingUpgradeFeaturesState> = MutableStateFlow(OnboardingUpgradeFeaturesState.Loading)
    val state: StateFlow<OnboardingUpgradeFeaturesState> = _state

    private val source = savedStateHandle.get<OnboardingUpgradeSource>("source") ?: OnboardingUpgradeSource.UNKNOWN
    private val showPatronOnly = savedStateHandle.get<Boolean>("show_patron_only")

    init {
        viewModelScope.launch {
            subscriptionManager
                .observeProductDetails()
                .asFlow()
                .stateIn(viewModelScope)
                .collect { productDetails ->
                    val subscriptions = when (productDetails) {
                        is ProductDetailsState.Error -> emptyList()
                        is ProductDetailsState.Loaded -> productDetails.productDetails.mapNotNull { productDetailsState ->
                            Subscription.fromProductDetails(
                                productDetails = productDetailsState,
                                isOfferEligible = subscriptionManager.isOfferEligible(
                                    SubscriptionMapper.mapProductIdToTier(productDetailsState.productId),
                                ),
                            )
                        }
                    }
                    val filteredOffer = Subscription.filterOffers(subscriptions)
                    updateState(filteredOffer)
                }
        }
    }

    private fun updateState(
        subscriptions: List<Subscription>,
    ) {
        val lastSelectedTier = settings.getLastSelectedSubscriptionTier().takeIf { source in listOf(OnboardingUpgradeSource.LOGIN, OnboardingUpgradeSource.PROFILE) }
        val lastSelectedFrequency = settings.getLastSelectedSubscriptionFrequency().takeIf { source in listOf(OnboardingUpgradeSource.LOGIN, OnboardingUpgradeSource.PROFILE) }

        val showPatronOnly = source == OnboardingUpgradeSource.ACCOUNT_DETAILS || showPatronOnly == true
        val fromLogin = source == OnboardingUpgradeSource.LOGIN
        val updatedSubscriptions =
            if (showPatronOnly) {
                subscriptions.filter { it.tier == Subscription.SubscriptionTier.PATRON }
            } else {
                subscriptions
            }

        val selectedSubscription = subscriptionManager.getDefaultSubscription(
            subscriptions = updatedSubscriptions,
            tier = if (showPatronOnly) Subscription.SubscriptionTier.PATRON else { if (fromLogin) lastSelectedTier else null },
            frequency = if (fromLogin) lastSelectedFrequency else null,
        )

        val showNotNow = source == OnboardingUpgradeSource.RECOMMENDATIONS

        val upgradeLayout = when {
            showPatronOnly -> UpgradeLayout.Original
            FeatureFlag.isEnabled(Feature.PAYWALL_AB_EXPERIMENT) -> UpgradeLayout.Reviews
            FeatureFlag.isEnabled(Feature.PAYWALL_AA_EXPERIMENT) -> {
                val variation = experimentProvider.getVariation(Experiment.PaywallAATest)
                // For the A/A test show the same layout for both variations
                if (variation == Variation.Control) {
                    UpgradeLayout.Original
                } else {
                    UpgradeLayout.Original
                }
            }
            else -> UpgradeLayout.Original
        }

        selectedSubscription?.let {
            val currentSubscriptionFrequency = selectedSubscription.recurringPricingPhase.toSubscriptionFrequency()
            val currentTier = SubscriptionMapper.mapProductIdToTier(selectedSubscription.productDetails.productId)
            val currentFeatureCard = currentTier.toUpgradeFeatureCard()
            _state.update {
                OnboardingUpgradeFeaturesState.Loaded(
                    featureCardsState = FeatureCardsState(
                        subscriptions = updatedSubscriptions,
                        currentFeatureCard = currentFeatureCard,
                        currentFrequency = currentSubscriptionFrequency,
                    ),
                    currentSubscription = selectedSubscription,
                    currentFeatureCard = currentFeatureCard,
                    currentSubscriptionFrequency = currentSubscriptionFrequency,
                    showNotNow = showNotNow,
                    upgradeLayout = upgradeLayout,
                )
            }
        } ?: _state.update { // In ideal world, we should never get here
            OnboardingUpgradeFeaturesState.NoSubscriptions(showNotNow)
        }
    }

    fun onShown(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_SHOWN, analyticsProps(flow, source))
    }

    fun onDismiss(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_DISMISSED, analyticsProps(flow, source))
    }

    fun onNotNow(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_NOT_NOW_BUTTON_TAPPED, analyticsProps(flow, source))
    }

    fun onSubscriptionFrequencyChanged(frequency: SubscriptionFrequency) {
        (_state.value as? OnboardingUpgradeFeaturesState.Loaded)?.let { loadedState ->
            val currentSubscription = subscriptionManager
                .getDefaultSubscription(
                    subscriptions = loadedState.featureCardsState.subscriptions,
                    tier = loadedState.currentFeatureCard.subscriptionTier,
                    frequency = frequency,
                )
            settings.setLastSelectedSubscriptionFrequency(frequency)
            currentSubscription?.let {
                _state.update {
                    loadedState.copy(
                        currentSubscription = currentSubscription,
                        currentSubscriptionFrequency = frequency,
                    )
                }
            }
        }
    }

    fun onFeatureCardChanged(upgradeFeatureCard: UpgradeFeatureCard) {
        (_state.value as? OnboardingUpgradeFeaturesState.Loaded)?.let { loadedState ->
            val currentSubscription = subscriptionManager
                .getDefaultSubscription(
                    subscriptions = loadedState.featureCardsState.subscriptions,
                    tier = upgradeFeatureCard.subscriptionTier,
                    frequency = loadedState.currentSubscriptionFrequency,
                )
            settings.setLastSelectedSubscriptionTier(upgradeFeatureCard.subscriptionTier)
            currentSubscription?.let {
                _state.update {
                    loadedState.copy(
                        currentSubscription = currentSubscription,
                        currentFeatureCard = upgradeFeatureCard,
                    )
                }
            }
        }
    }

    fun onClickSubscribe(
        activity: Activity,
        flow: OnboardingFlow,
        source: OnboardingUpgradeSource,
        onComplete: () -> Unit,
    ) {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_UPGRADE_BUTTON_TAPPED)

        (state.value as? OnboardingUpgradeFeaturesState.Loaded)?.let { loadedState ->
            _state.update { loadedState.copy(purchaseFailed = false) }
            val currentSubscription = subscriptionManager
                .getDefaultSubscription(
                    subscriptions = loadedState.featureCardsState.subscriptions,
                    tier = loadedState.currentFeatureCard.subscriptionTier,
                    frequency = loadedState.currentSubscriptionFrequency,
                )

            currentSubscription?.let { subscription ->
                analyticsTracker.track(
                    AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_NEXT_BUTTON_TAPPED,
                    mapOf(
                        OnboardingUpgradeBottomSheetViewModel.flowKey to flow.analyticsValue,
                        OnboardingUpgradeBottomSheetViewModel.sourceKey to source.analyticsValue,
                        OnboardingUpgradeBottomSheetViewModel.selectedSubscriptionKey to subscription.productDetails.productId,
                    ),
                )

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
                        CreateAccountViewModel.trackPurchaseEvent(
                            subscription,
                            purchaseEvent,
                            analyticsTracker,
                        )
                    }
                }
                subscriptionManager.launchBillingFlow(
                    activity,
                    subscription.productDetails,
                    subscription.offerToken,
                )
            }
        }
    }

    companion object {
        private fun analyticsProps(flow: OnboardingFlow, source: OnboardingUpgradeSource) =
            mapOf("flow" to flow.analyticsValue, "source" to source.analyticsValue)
    }
}

sealed class OnboardingUpgradeFeaturesState {
    data object Loading : OnboardingUpgradeFeaturesState()

    data class NoSubscriptions(val showNotNow: Boolean) : OnboardingUpgradeFeaturesState()

    data class Loaded(
        val currentFeatureCard: UpgradeFeatureCard,
        val featureCardsState: FeatureCardsState,
        val currentSubscriptionFrequency: SubscriptionFrequency,
        val currentSubscription: Subscription,
        val purchaseFailed: Boolean = false,
        val showNotNow: Boolean,
        val upgradeLayout: UpgradeLayout,
    ) : OnboardingUpgradeFeaturesState() {
        val subscriptionFrequencies =
            listOf(SubscriptionFrequency.YEARLY, SubscriptionFrequency.MONTHLY)
        val currentUpgradeButton: UpgradeButton
            get() = currentSubscription.toUpgradeButton()
    }
}
