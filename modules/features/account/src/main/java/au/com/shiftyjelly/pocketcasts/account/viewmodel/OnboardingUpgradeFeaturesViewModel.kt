package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingSubscriptionPlan
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.PurchaseResult
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.flatMap
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = OnboardingUpgradeFeaturesViewModel.Factory::class)
class OnboardingUpgradeFeaturesViewModel @AssistedInject constructor(
    private val paymentClient: PaymentClient,
    private val analyticsTracker: AnalyticsTracker,
    @Assisted private val flow: OnboardingFlow,
) : ViewModel() {
    private val _state = MutableStateFlow<OnboardingUpgradeFeaturesState>(OnboardingUpgradeFeaturesState.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val subscriptionPlans = paymentClient.loadSubscriptionPlans().getOrNull()
            if (subscriptionPlans == null) {
                _state.value = OnboardingUpgradeFeaturesState.NoSubscriptions
            } else {
                _state.value = createInitialLoadedState(subscriptionPlans)
            }
        }
    }

    private fun createInitialLoadedState(
        subscriptionPlans: SubscriptionPlans,
    ): OnboardingUpgradeFeaturesState.Loaded {
        val showPatronOnly = flow.source == OnboardingUpgradeSource.ACCOUNT_DETAILS
        return OnboardingUpgradeFeaturesState.Loaded(
            subscriptionPlans,
            selectedBillingCycle = flow.preselectedBillingCycle,
            selectedTier = if (showPatronOnly) SubscriptionTier.Patron else flow.preselectedTier,
            showOnlyPatronPlans = showPatronOnly,
            purchaseFailed = false,
        )
    }

    fun changeBillingCycle(billingCycle: BillingCycle) {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_SUBSCRIPTION_FREQUENCY_CHANGED, mapOf("value" to billingCycle.analyticsValue))
        _state.update { state ->
            (_state.value as? OnboardingUpgradeFeaturesState.Loaded)
                ?.let { loadedState -> loadedState.copy(selectedBillingCycle = billingCycle) }
                ?: state
        }
    }

    fun changeSubscriptionTier(tier: SubscriptionTier) {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_SUBSCRIPTION_TIER_CHANGED, mapOf("value" to tier.analyticsValue))
        _state.update { state ->
            (_state.value as? OnboardingUpgradeFeaturesState.Loaded)
                ?.let { loadedState -> loadedState.copy(selectedTier = tier) }
                ?: state
        }
    }

    fun purchaseSelectedPlan(
        activity: Activity,
        onComplete: () -> Unit,
    ) {
        _state.update { value -> (value as? OnboardingUpgradeFeaturesState.Loaded)?.copy(purchaseFailed = false) ?: value }
        (state.value as? OnboardingUpgradeFeaturesState.Loaded)?.let { loadedState ->
            val planKey = loadedState.selectedPlan.key
            trackPaymentFrequencyButtonTapped(planKey)
            viewModelScope.launch {
                val purchaseResult = paymentClient.purchaseSubscriptionPlan(planKey, activity)
                trackPurchaseResult(planKey, purchaseResult)

                when (purchaseResult) {
                    is PurchaseResult.Purchased -> {
                        onComplete()
                    }

                    is PurchaseResult.Cancelled -> Unit
                    is PurchaseResult.Failure -> {
                        _state.update { value -> (value as? OnboardingUpgradeFeaturesState.Loaded)?.copy(purchaseFailed = true) ?: value }
                    }
                }
            }
        }
    }

    private fun trackPaymentFrequencyButtonTapped(plan: SubscriptionPlan.Key) {
        analyticsTracker.track(
            AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_NEXT_BUTTON_TAPPED,
            mapOf(
                "flow" to flow.analyticsValue,
                "source" to flow.source.analyticsValue,
                "product" to plan.productId,
            ),
        )
    }

    private fun trackPurchaseResult(
        plan: SubscriptionPlan.Key,
        purchaseResult: PurchaseResult,
    ) {
        val productValue = when (plan.tier) {
            SubscriptionTier.Plus -> plan.billingCycle.analyticsValue
            SubscriptionTier.Patron -> plan.productId
        }
        val analyticsProperties = buildMap {
            put("product", productValue)
            put("offer_type", plan.offer?.analyticsValue ?: "none")
            put("source", flow.source.analyticsValue)
        }
        when (purchaseResult) {
            is PurchaseResult.Purchased -> analyticsTracker.track(AnalyticsEvent.PURCHASE_SUCCESSFUL, analyticsProperties)

            is PurchaseResult.Cancelled -> analyticsTracker.track(AnalyticsEvent.PURCHASE_CANCELLED)

            is PurchaseResult.Failure -> {
                analyticsTracker.track(
                    AnalyticsEvent.PURCHASE_FAILED,
                    analyticsProperties + purchaseResult.code.analyticProperties(),
                )
            }
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

    fun onPrivacyPolicyPressed() {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_PRIVACY_POLICY_TAPPED)
    }

    fun onTermsAndConditionsPressed() {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_TERMS_AND_CONDITIONS_TAPPED)
    }

    fun onSelectPaymentFrequencyShown(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_SHOWN, analyticsProps(flow, source))
    }

    fun onSelectPaymentFrequencyDismissed(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_DISMISSED, analyticsProps(flow, source))
    }

    @AssistedFactory
    interface Factory {
        fun create(flow: OnboardingFlow): OnboardingUpgradeFeaturesViewModel
    }

    companion object {
        private fun analyticsProps(flow: OnboardingFlow, source: OnboardingUpgradeSource) = mapOf(
            "flow" to flow.analyticsValue,
            "source" to source.analyticsValue,
        )
    }
}

private fun PaymentResultCode.analyticProperties() = buildMap {
    put("error", analyticsValue)
    if (this@analyticProperties is PaymentResultCode.Unknown) {
        put("error_code", code)
    }
}

sealed class OnboardingUpgradeFeaturesState {
    data object Loading : OnboardingUpgradeFeaturesState()

    data object NoSubscriptions : OnboardingUpgradeFeaturesState()

    data class Loaded(
        val subscriptionPlans: SubscriptionPlans,
        val selectedTier: SubscriptionTier,
        val selectedBillingCycle: BillingCycle,
        val showOnlyPatronPlans: Boolean,
        val purchaseFailed: Boolean,
    ) : OnboardingUpgradeFeaturesState() {
        val availablePlans = listOfNotNull(
            plusYearlyPlanWithOffer().takeUnless { showOnlyPatronPlans },
            plusMonthlyPlan().takeUnless { showOnlyPatronPlans },
            patronYearlyPlan(),
            patronMonthlyPlan(),
        )

        val selectedPlan get() = getPlan(selectedTier, selectedBillingCycle)

        private fun getPlan(tier: SubscriptionTier, billingCycle: BillingCycle) = availablePlans.first { plan ->
            plan.key.tier == tier && plan.key.billingCycle == billingCycle
        }

        private fun plusYearlyPlanWithOffer(): OnboardingSubscriptionPlan {
            val offer = if (FeatureFlag.isEnabled(Feature.INTRO_PLUS_OFFER_ENABLED)) {
                SubscriptionOffer.IntroOffer
            } else {
                SubscriptionOffer.Trial
            }
            return subscriptionPlans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, offer)
                .flatMap { OnboardingSubscriptionPlan.create(it) }
                .getOrNull()
                ?: plusYearlyPlan()
        }

        private fun plusYearlyPlan(): OnboardingSubscriptionPlan {
            return OnboardingSubscriptionPlan.create(subscriptionPlans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Yearly))
        }

        private fun patronYearlyPlan(): OnboardingSubscriptionPlan {
            return OnboardingSubscriptionPlan.create(subscriptionPlans.getBasePlan(SubscriptionTier.Patron, BillingCycle.Yearly))
        }

        private fun plusMonthlyPlan(): OnboardingSubscriptionPlan {
            return OnboardingSubscriptionPlan.create(subscriptionPlans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Monthly))
        }

        private fun patronMonthlyPlan(): OnboardingSubscriptionPlan {
            return OnboardingSubscriptionPlan.create(subscriptionPlans.getBasePlan(SubscriptionTier.Patron, BillingCycle.Monthly))
        }
    }
}
