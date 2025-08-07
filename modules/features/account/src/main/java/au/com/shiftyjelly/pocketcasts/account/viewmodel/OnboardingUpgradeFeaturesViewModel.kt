package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingSubscriptionPlan
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Experiment
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentProvider
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Variation
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PurchaseResult
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = OnboardingUpgradeFeaturesViewModel.Factory::class)
class OnboardingUpgradeFeaturesViewModel @AssistedInject constructor(
    private val paymentClient: PaymentClient,
    private val analyticsTracker: AnalyticsTracker,
    private val notificationManager: NotificationManager,
    private val experimentProvider: ExperimentProvider,
    @Assisted private val flow: OnboardingFlow,
) : ViewModel() {
    private val _state = MutableStateFlow<OnboardingUpgradeFeaturesState>(OnboardingUpgradeFeaturesState.Loading)
    val state = _state.asStateFlow()

    init {
        loadSubscriptionPlans()
    }

    private fun createInitialLoadedState(
        subscriptionPlans: SubscriptionPlans,
        variant: OnboardingUpgradeFeaturesState.NewOnboardingVariant,
    ): OnboardingUpgradeFeaturesState.Loaded {
        val plansFilter =
            if (flow is OnboardingFlow.PatronAccountUpgrade) {
                OnboardingUpgradeFeaturesState.LoadedPlansFilter.PATRON_ONLY
            } else if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                OnboardingUpgradeFeaturesState.LoadedPlansFilter.PLUS_ONLY
            } else {
                OnboardingUpgradeFeaturesState.LoadedPlansFilter.BOTH
            }
        return OnboardingUpgradeFeaturesState.Loaded(
            subscriptionPlans,
            selectedBillingCycle = flow.preselectedBillingCycle,
            selectedTier = if (plansFilter == OnboardingUpgradeFeaturesState.LoadedPlansFilter.PATRON_ONLY) SubscriptionTier.Patron else flow.preselectedTier,
            plansFilter = plansFilter,
            purchaseFailed = false,
            onboardingVariant = variant,
        )
    }

    private var subscriptionPlansJob: Job? = null

    fun loadSubscriptionPlans() {
        if (subscriptionPlansJob?.isActive == true) {
            return
        }
        subscriptionPlansJob = viewModelScope.launch {
            val subscriptionPlans = paymentClient.loadSubscriptionPlans().getOrNull()
            if (subscriptionPlans == null) {
                _state.value = OnboardingUpgradeFeaturesState.NoSubscriptions
            } else {
                val variant = experimentProvider.getVariation(Experiment.NewOnboardingABTest).toNewOnboardingVariant()
                _state.value = createInitialLoadedState(
                    subscriptionPlans = subscriptionPlans,
                    variant = variant,
                )
            }
        }
    }

    private fun Variation?.toNewOnboardingVariant() = when (this) {
        is Variation.Treatment -> OnboardingUpgradeFeaturesState.NewOnboardingVariant.TRIAL_FIRST_WHEN_ELIGIBLE
        else -> OnboardingUpgradeFeaturesState.NewOnboardingVariant.FEATURES_FIRST
    }

    fun changeBillingCycle(billingCycle: BillingCycle) {
        val properties = analyticsProps(flow = flow, variant = experimentProvider.getVariation(Experiment.NewOnboardingABTest).toNewOnboardingVariant()).toMutableMap().apply {
            put("value", billingCycle.analyticsValue)
        }
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_SUBSCRIPTION_FREQUENCY_CHANGED, properties)
        _state.update { state ->
            (_state.value as? OnboardingUpgradeFeaturesState.Loaded)?.copy(selectedBillingCycle = billingCycle)
                ?: state
        }
    }

    fun changeSubscriptionTier(tier: SubscriptionTier) {
        val properties = analyticsProps(flow = flow, variant = experimentProvider.getVariation(Experiment.NewOnboardingABTest).toNewOnboardingVariant()).toMutableMap().apply {
            put("value", tier.analyticsValue)
        }
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_SUBSCRIPTION_TIER_CHANGED, properties)
        _state.update { state ->
            (_state.value as? OnboardingUpgradeFeaturesState.Loaded)?.copy(selectedTier = tier)
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
                val purchaseResult = paymentClient.purchaseSubscriptionPlan(planKey, flow.source.analyticsValue, activity)

                when (purchaseResult) {
                    is PurchaseResult.Purchased -> {
                        notificationManager.updateUserFeatureInteraction(OnboardingNotificationType.PlusUpsell)
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

    fun onShown(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_SHOWN, analyticsProps(flow = flow, source = source, variant = experimentProvider.getVariation(Experiment.NewOnboardingABTest).toNewOnboardingVariant()))
    }

    fun onDismiss(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_DISMISSED, analyticsProps(flow = flow, source = source, variant = experimentProvider.getVariation(Experiment.NewOnboardingABTest).toNewOnboardingVariant()))
    }

    fun onNotNow(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_NOT_NOW_BUTTON_TAPPED, analyticsProps(flow = flow, source = source, variant = experimentProvider.getVariation(Experiment.NewOnboardingABTest).toNewOnboardingVariant()))
    }

    fun onPrivacyPolicyPressed() {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_PRIVACY_POLICY_TAPPED, analyticsProps(flow = flow, variant = experimentProvider.getVariation(Experiment.NewOnboardingABTest).toNewOnboardingVariant()))
    }

    fun onTermsAndConditionsPressed() {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_TERMS_AND_CONDITIONS_TAPPED, analyticsProps(flow = flow, variant = experimentProvider.getVariation(Experiment.NewOnboardingABTest).toNewOnboardingVariant()))
    }

    fun onSelectPaymentFrequencyShown(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_SHOWN, analyticsProps(flow = flow, source = source, variant = experimentProvider.getVariation(Experiment.NewOnboardingABTest).toNewOnboardingVariant()))
    }

    fun onSelectPaymentFrequencyDismissed(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.SELECT_PAYMENT_FREQUENCY_DISMISSED, analyticsProps(flow = flow, source = source, variant = experimentProvider.getVariation(Experiment.NewOnboardingABTest).toNewOnboardingVariant()))
    }

    @AssistedFactory
    interface Factory {
        fun create(flow: OnboardingFlow): OnboardingUpgradeFeaturesViewModel
    }

    companion object {
        private fun analyticsProps(
            flow: OnboardingFlow,
            variant: OnboardingUpgradeFeaturesState.NewOnboardingVariant,
            source: OnboardingUpgradeSource? = null,
        ) = buildMap {
            put("flow", flow.analyticsValue)
            source?.let {
                put("source", it.analyticsValue)
            }
            if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_UPGRADE)) {
                put("version", "1")
                put("variant", if (variant == OnboardingUpgradeFeaturesState.NewOnboardingVariant.FEATURES_FIRST) "A" else "B")
            }
        }
    }
}

sealed class OnboardingUpgradeFeaturesState {
    data object Loading : OnboardingUpgradeFeaturesState()

    data object NoSubscriptions : OnboardingUpgradeFeaturesState()

    enum class LoadedPlansFilter {
        PATRON_ONLY,
        PLUS_ONLY,
        BOTH,
    }

    enum class NewOnboardingVariant {
        FEATURES_FIRST,
        TRIAL_FIRST_WHEN_ELIGIBLE,
    }

    data class Loaded(
        val subscriptionPlans: SubscriptionPlans,
        val selectedTier: SubscriptionTier,
        val selectedBillingCycle: BillingCycle,
        val plansFilter: LoadedPlansFilter,
        val purchaseFailed: Boolean,
        val onboardingVariant: NewOnboardingVariant,
    ) : OnboardingUpgradeFeaturesState() {
        val availableBasePlans = listOfNotNull(
            plusYearlyPlanWithOffer().takeUnless { plansFilter == LoadedPlansFilter.PATRON_ONLY },
            plusMonthlyPlan().takeUnless { plansFilter == LoadedPlansFilter.PATRON_ONLY },
            patronYearlyPlan().takeUnless { plansFilter == LoadedPlansFilter.PLUS_ONLY },
            patronMonthlyPlan().takeUnless { plansFilter == LoadedPlansFilter.PLUS_ONLY },
        )

        val availablePlans: List<OnboardingSubscriptionPlan> by lazy {
            availableBasePlans.mapNotNull {
                when (it) {
                    is SubscriptionPlan.WithOffer -> OnboardingSubscriptionPlan.create(it).getOrNull()
                    is SubscriptionPlan.Base -> OnboardingSubscriptionPlan.create(it)
                }
            }
        }

        val selectedPlan get() = getPlan(selectedTier, selectedBillingCycle)

        val selectedBasePlan get() = getBasePlan(selectedTier, selectedBillingCycle)

        private fun getPlan(tier: SubscriptionTier, billingCycle: BillingCycle) = availablePlans.first { plan ->
            plan.key.tier == tier && plan.key.billingCycle == billingCycle
        }

        private fun getBasePlan(tier: SubscriptionTier, billingCycle: BillingCycle) = availableBasePlans.first { plan ->
            plan.key.tier == tier && plan.key.billingCycle == billingCycle
        }

        private fun plusYearlyPlanWithOffer(): SubscriptionPlan {
            val offer = if (FeatureFlag.isEnabled(Feature.INTRO_PLUS_OFFER_ENABLED)) {
                SubscriptionOffer.IntroOffer
            } else {
                SubscriptionOffer.Trial
            }

            val offerPlan = subscriptionPlans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, offer).getOrNull()
            return if (offerPlan == null || OnboardingSubscriptionPlan.create(offerPlan).getOrNull() == null) {
                plusYearlyPlan()
            } else {
                offerPlan
            }
        }

        private fun plusYearlyPlan(): SubscriptionPlan.Base {
            return subscriptionPlans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Yearly)
        }

        private fun patronYearlyPlan(): SubscriptionPlan.Base {
            return subscriptionPlans.getBasePlan(SubscriptionTier.Patron, BillingCycle.Yearly)
        }

        private fun plusMonthlyPlan(): SubscriptionPlan.Base {
            return subscriptionPlans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Monthly)
        }

        private fun patronMonthlyPlan(): SubscriptionPlan.Base {
            return subscriptionPlans.getBasePlan(SubscriptionTier.Patron, BillingCycle.Monthly)
        }
    }
}
