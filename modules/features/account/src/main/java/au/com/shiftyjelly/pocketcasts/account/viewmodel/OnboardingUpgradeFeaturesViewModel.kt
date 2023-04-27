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
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PATRON_PRODUCT_BASE
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_PRODUCT_BASE
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
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
            val currentSubscriptionFrequency = when (defaultSelected.recurringPricingPhase) {
                is SubscriptionPricingPhase.Months -> SubscriptionFrequency.MONTHLY
                is SubscriptionPricingPhase.Years -> SubscriptionFrequency.YEARLY
            }
            val currentFeatureCard =
                if (defaultSelected.productDetails.productId.startsWith(PLUS_PRODUCT_BASE)) {
                    UpgradeFeatureCard.PLUS
                } else {
                    UpgradeFeatureCard.PATRON
                }
            _state.update {
                OnboardingUpgradeFeaturesState.Loaded(
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
            _state.update { loadedState.copy(currentSubscriptionFrequency = frequency) }
        }
    }

    fun onFeatureCardChanged(index: Int) {
        (_state.value as? OnboardingUpgradeFeaturesState.Loaded)?.let { loadedState ->
            _state.update { loadedState.copy(currentFeatureCard = UpgradeFeatureCard.values()[index]) }
        }
    }

    fun getUpgradePrice(
        subscriptions: List<Subscription>,
        productIdPrefix: String,
    ): String {
        val loadedState = _state.value as? OnboardingUpgradeFeaturesState.Loaded
        return loadedState?.let {
            subscriptions
                .find {
                    if (loadedState.currentSubscriptionFrequency == SubscriptionFrequency.MONTHLY) {
                        it.recurringPricingPhase is SubscriptionPricingPhase.Months
                    } else {
                        it.recurringPricingPhase is SubscriptionPricingPhase.Years
                    } && it.productDetails.productId.startsWith(productIdPrefix)
                }
                ?.recurringPricingPhase?.formattedPrice
        } ?: ""
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
        val currentFeatureCard: UpgradeFeatureCard = UpgradeFeatureCard.PLUS,
        val currentSubscriptionFrequency: SubscriptionFrequency = SubscriptionFrequency.YEARLY,
        val currentSubscription: Subscription,
    ) : OnboardingUpgradeFeaturesState() {

        val featureCards = UpgradeFeatureCard.values().toList()
        val subscriptionFrequencies =
            listOf(SubscriptionFrequency.YEARLY, SubscriptionFrequency.MONTHLY)
    }
}

enum class UpgradeFeatureCard(
    @StringRes val titleRes: Int,
    @StringRes val shortNameRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val backgroundGlowsRes: Int,
    @DrawableRes val iconRes: Int,
    val buttonBackgroundColor: Long,
    val buttonTextColor: Long,
    val featureItems: List<UpgradeFeatureItem>,
    val productIdPrefix: String,
) {
    PLUS(
        titleRes = LR.string.onboarding_plus_features_title,
        shortNameRes = LR.string.pocket_casts_plus_short,
        descriptionRes = LR.string.onboarding_plus_features_description,
        backgroundGlowsRes = R.drawable.upgrade_background_plus_glows,
        iconRes = IR.drawable.ic_plus,
        buttonBackgroundColor = 0xFFFFD846,
        buttonTextColor = 0xFF000000,
        featureItems = PlusUpgradeFeatureItem.values().toList(),
        productIdPrefix = PLUS_PRODUCT_BASE,
    ),
    PATRON(
        titleRes = LR.string.onboarding_patron_features_title,
        shortNameRes = LR.string.pocket_casts_patron_short,
        descriptionRes = LR.string.onboarding_patron_features_description,
        backgroundGlowsRes = R.drawable.upgrade_background_patron_glows,
        iconRes = IR.drawable.ic_patron,
        buttonBackgroundColor = 0xFF6046F5,
        buttonTextColor = 0xFFFFFFFF,
        featureItems = PatronUpgradeFeatureItem.values().toList(),
        productIdPrefix = PATRON_PRODUCT_BASE,
    )
}
