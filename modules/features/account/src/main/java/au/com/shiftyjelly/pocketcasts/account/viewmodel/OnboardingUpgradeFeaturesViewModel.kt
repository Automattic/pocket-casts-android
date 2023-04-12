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
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class OnboardingUpgradeFeaturesViewModel @Inject constructor(
    app: Application,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : AndroidViewModel(app) {

    private val _state: MutableStateFlow<OnboardingUpgradeFeaturesState>
    val state: StateFlow<OnboardingUpgradeFeaturesState>

    init {
        val accessibiltyManager = getApplication<Application>().getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager

        val isTouchExplorationEnabled = accessibiltyManager?.isTouchExplorationEnabled ?: false
        _state = MutableStateFlow(OnboardingUpgradeFeaturesState(isTouchExplorationEnabled))
        state = _state

        accessibiltyManager?.addTouchExplorationStateChangeListener { enabled ->
            _state.value = OnboardingUpgradeFeaturesState(enabled)
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

    fun onUpgradePressed(flow: OnboardingFlow, source: OnboardingUpgradeSource) {
        analyticsTracker.track(AnalyticsEvent.PLUS_PROMOTION_UPGRADE_BUTTON_TAPPED, analyticsProps(flow, source))
    }

    fun onSubscriptionFrequencyChanged(index: Int) {
        _state.value = _state.value.copy(
            currentSubscriptionFrequency = when (index) {
                0 -> SubscriptionFrequency.YEARLY
                1 -> SubscriptionFrequency.MONTHLY
                else -> throw IllegalArgumentException("Invalid index: $index")
            }
        )
    }

    fun onFeatureCardChanged(index: Int) {
        _state.value = _state.value.copy(currentFeatureCard = UpgradeFeatureCard.values()[index])
    }

    companion object {
        private fun analyticsProps(flow: OnboardingFlow, source: OnboardingUpgradeSource) =
            mapOf("flow" to flow.analyticsValue, "source" to source.analyticsValue)
    }
}

data class OnboardingUpgradeFeaturesState(
    private val isTouchExplorationEnabled: Boolean,
    val currentFeatureCard: UpgradeFeatureCard = UpgradeFeatureCard.PLUS,
    val currentSubscriptionFrequency: SubscriptionFrequency = SubscriptionFrequency.YEARLY,
) {
    val scrollAutomatically = !isTouchExplorationEnabled
    val featureCards = UpgradeFeatureCard.values().toList()
    val subscriptionFrequencies = listOf(SubscriptionFrequency.YEARLY, SubscriptionFrequency.MONTHLY)
}

enum class UpgradeFeatureCard(
    @StringRes val shortNameRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val backgroundGlowsRes: Int,
    @DrawableRes val iconRes: Int,
    val buttonBackgroundColor: Long,
    val buttonTextColor: Long,
    val featureItems: List<UpgradeFeatureItem>,
) {
    PLUS(
        shortNameRes = LR.string.pocket_casts_plus_short,
        descriptionRes = LR.string.onboarding_patron_features_description,
        backgroundGlowsRes = R.drawable.upgrade_background_plus_glows,
        iconRes = IR.drawable.ic_plus,
        buttonBackgroundColor = 0xFFFFD845,
        buttonTextColor = 0xFF000000,
        featureItems = PlusUpgradeFeatureItem.values().toList(),
    ),
    PATRON(
        shortNameRes = LR.string.pocket_casts_patron_short,
        descriptionRes = LR.string.onboarding_plus_features_description,
        backgroundGlowsRes = R.drawable.upgrade_background_patron_glows,
        iconRes = IR.drawable.ic_patron,
        buttonBackgroundColor = 0xFF7A64F6,
        buttonTextColor = 0xFFFFFFFF,
        featureItems = PatronUpgradeFeatureItem.values().toList(),
    )
}
