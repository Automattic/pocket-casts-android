package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import au.com.shiftyjelly.pocketcasts.account.R
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.PatronUpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.PlusUpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeFeatureItem
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.type.RecurringSubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.repositories.BuildConfig
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class OnboardingUpgradeFeaturesViewModel @Inject constructor(
    app: Application,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val subscriptionManager: SubscriptionManager,
) : AndroidViewModel(app) {

    private val _state: MutableStateFlow<OnboardingUpgradeFeaturesState> = MutableStateFlow(OnboardingUpgradeFeaturesState.Loading)
    val state: StateFlow<OnboardingUpgradeFeaturesState> = _state

    fun start(subscriptions: List<Subscription>) {
        if (BuildConfig.ADD_PATRON_ENABLED) {
            updateState(subscriptions)
        } else {
            val accessibiltyManager = getApplication<Application>().getSystemService(Context.ACCESSIBILITY_SERVICE)
                as? AccessibilityManager

            var isTouchExplorationEnabled = accessibiltyManager?.isTouchExplorationEnabled ?: false
            accessibiltyManager?.addTouchExplorationStateChangeListener {
                isTouchExplorationEnabled = it
            }
            _state.update { OnboardingUpgradeFeaturesState.OldLoaded(isTouchExplorationEnabled) }
        }
    }

    private fun updateState(
        subscriptions: List<Subscription>,
    ) {
        val defaultSelected = subscriptionManager.getDefaultSubscription(subscriptions) // TODO: Patron or Plus?
        defaultSelected?.let {
            val currentSubscriptionFrequency = defaultSelected.recurringPricingPhase.toSubscriptionFrequency()
            val defaultTier = SubscriptionMapper.mapProductIdToTier(defaultSelected.productDetails.productId)
            val currentFeatureCard = defaultTier.toUpgradeFeatureCard()
            _state.update {
                OnboardingUpgradeFeaturesState.Loaded(
                    subscriptions = subscriptions,
                    currentSubscription = defaultSelected,
                    currentFeatureCard = currentFeatureCard,
                    currentSubscriptionFrequency = currentSubscriptionFrequency
                )
            }
        } ?: Timber.e("No subscriptions found")
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

    fun onUpgradePressed(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_UPGRADE_BUTTON_TAPPED, analyticsProps(flow, source))
    }

    fun onSubscriptionFrequencyChanged(frequency: SubscriptionFrequency) {
        (_state.value as? OnboardingUpgradeFeaturesState.Loaded)?.let { loadedState ->
            val currentSubscription = subscriptionManager
                .getSubscriptionByTierAndFrequency(
                    subscriptions = loadedState.subscriptions,
                    tier = loadedState.currentFeatureCard.subscriptionTier,
                    frequency = frequency
                )
            currentSubscription?.let {
                _state.update {
                    loadedState.copy(
                        currentSubscription = currentSubscription,
                        currentSubscriptionFrequency = frequency
                    )
                }
            }
        }
    }

    fun onFeatureCardChanged(upgradeFeatureCard: UpgradeFeatureCard) {
        (_state.value as? OnboardingUpgradeFeaturesState.Loaded)?.let { loadedState ->
            val currentSubscription = subscriptionManager
                .getSubscriptionByTierAndFrequency(
                    subscriptions = loadedState.subscriptions,
                    tier = upgradeFeatureCard.subscriptionTier,
                    frequency = loadedState.currentSubscriptionFrequency
                )
            currentSubscription?.let {
                _state.update {
                    loadedState.copy(
                        currentSubscription = currentSubscription,
                        currentFeatureCard = upgradeFeatureCard
                    )
                }
            }
        }
    }

    companion object {
        private fun analyticsProps(flow: OnboardingFlow, source: OnboardingUpgradeSource) =
            mapOf("flow" to flow.analyticsValue, "source" to source.analyticsValue)
    }
}

sealed class OnboardingUpgradeFeaturesState {
    object Loading : OnboardingUpgradeFeaturesState()

    data class OldLoaded(
        private val isTouchExplorationEnabled: Boolean,
    ) : OnboardingUpgradeFeaturesState() {
        val scrollAutomatically = !isTouchExplorationEnabled
    }

    data class Loaded(
        val subscriptions: List<Subscription>,
        val currentFeatureCard: UpgradeFeatureCard = UpgradeFeatureCard.PLUS,
        val currentSubscriptionFrequency: SubscriptionFrequency = SubscriptionFrequency.YEARLY,
        val currentSubscription: Subscription,

    ) : OnboardingUpgradeFeaturesState() {

        val featureCards = UpgradeFeatureCard.values().toList()
        val subscriptionFrequencies =
            listOf(SubscriptionFrequency.YEARLY, SubscriptionFrequency.MONTHLY)
        val currentUpgradeButton: UpgradeButton
            get() = currentSubscription.toUpgradeButton()
    }
}

private fun SubscriptionTier.toUpgradeFeatureCard() = when (this) {
    SubscriptionTier.PLUS -> UpgradeFeatureCard.PLUS
    SubscriptionTier.PATRON -> UpgradeFeatureCard.PATRON
    SubscriptionTier.UNKNOWN -> throw IllegalStateException("Unknown subscription tier")
}

private fun Subscription.toUpgradeButton() = when (this.tier) {
    SubscriptionTier.PLUS -> UpgradeButton.Plus(this)
    SubscriptionTier.PATRON -> UpgradeButton.Patron(this)
    SubscriptionTier.UNKNOWN -> throw IllegalStateException("Unknown subscription tier")
}

private fun RecurringSubscriptionPricingPhase.toSubscriptionFrequency() = when (this) {
    is SubscriptionPricingPhase.Months -> SubscriptionFrequency.MONTHLY
    is SubscriptionPricingPhase.Years -> SubscriptionFrequency.YEARLY
}

enum class UpgradeFeatureCard(
    @StringRes val titleRes: Int,
    @StringRes val shortNameRes: Int,
    @DrawableRes val backgroundGlowsRes: Int,
    @DrawableRes val iconRes: Int,
    val featureItems: List<UpgradeFeatureItem>,
    val subscriptionTier: SubscriptionTier,
) {
    PLUS(
        titleRes = LR.string.onboarding_plus_features_title,
        shortNameRes = LR.string.pocket_casts_plus_short,
        backgroundGlowsRes = R.drawable.upgrade_background_plus_glows,
        iconRes = IR.drawable.ic_plus,
        featureItems = PlusUpgradeFeatureItem.values().toList(),
        subscriptionTier = SubscriptionTier.PLUS,
    ),
    PATRON(
        titleRes = LR.string.onboarding_patron_features_title,
        shortNameRes = LR.string.pocket_casts_patron_short,
        backgroundGlowsRes = R.drawable.upgrade_background_patron_glows,
        iconRes = IR.drawable.ic_patron,
        featureItems = PatronUpgradeFeatureItem.values().toList(),
        subscriptionTier = SubscriptionTier.PATRON,
    )
}

sealed class UpgradeButton(
    @StringRes val shortNameRes: Int,
    val backgroundColor: Long,
    val textColor: Long,
    open val subscription: Subscription,
) {
    data class Plus(
        override val subscription: Subscription,
    ) : UpgradeButton(
        shortNameRes = LR.string.pocket_casts_plus_short,
        backgroundColor = 0xFFFFD846,
        textColor = 0xFF000000,
        subscription = subscription,
    )

    data class Patron(
        override val subscription: Subscription,
    ) : UpgradeButton(
        shortNameRes = LR.string.pocket_casts_patron_short,
        backgroundColor = 0xFF6046F5,
        textColor = 0xFFFFFFFF,
        subscription = subscription,
    )
}
