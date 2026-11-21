package au.com.shiftyjelly.pocketcasts.settings.developer

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRadioDialogRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.mock.MockBillingConfig
import au.com.shiftyjelly.pocketcasts.payment.mock.MockPurchaseOutcome
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

data class MockBillingState(
    val config: MockBillingConfig,
    val canToggleMock: Boolean,
)

class MockBillingActions(
    val onMockEnabledChange: (Boolean) -> Unit,
    val onOfferEligibilityChange: (SubscriptionOffer, Boolean) -> Unit,
    val onFailProductsLoadingChange: (Boolean) -> Unit,
    val onPurchaseOutcomeChange: (MockPurchaseOutcome) -> Unit,
    val onOpenPaywallClick: (OnboardingUpgradeSource) -> Unit,
)

enum class DeveloperPaywall(
    val label: String,
    val source: OnboardingUpgradeSource,
) {
    Generic("Generic (Superpowers for your podcasts)", OnboardingUpgradeSource.SETTINGS),
    Folders("Folders", OnboardingUpgradeSource.FOLDERS),
    Bookmarks("Bookmarks", OnboardingUpgradeSource.BOOKMARKS),
    PreselectChapters("Preselect Chapters", OnboardingUpgradeSource.SKIP_CHAPTERS),
    UpNextShuffle("Up Next Shuffle", OnboardingUpgradeSource.UP_NEXT_SHUFFLE),
    EpisodeChat("Episode chat", OnboardingUpgradeSource.EPISODE_CHAT),
    Blogs("Blogs", OnboardingUpgradeSource.BLOGS),
    Themes("Themes", OnboardingUpgradeSource.THEMES),
    Icons("App icons", OnboardingUpgradeSource.ICONS),
    Files("Files", OnboardingUpgradeSource.FILES),
    BannerAd("Banner ad", OnboardingUpgradeSource.BANNER_AD),
}

internal fun LazyListScope.mockBillingSection(
    state: MockBillingState,
    actions: MockBillingActions,
) {
    item {
        MockBillingToggle(state = state, onCheckedChange = actions.onMockEnabledChange)
    }
    SubscriptionOffer.entries.forEach { offer ->
        item(key = "mock-offer-${offer.name}") {
            OfferEligibilityRow(
                offer = offer,
                isEligible = offer in state.config.eligibleOffers,
                enabled = state.config.isEnabled,
                onCheckedChange = { isEligible -> actions.onOfferEligibilityChange(offer, isEligible) },
            )
        }
    }
    item {
        FailProductsLoadingRow(
            checked = state.config.failProductsLoading,
            enabled = state.config.isEnabled,
            onCheckedChange = actions.onFailProductsLoadingChange,
        )
    }
    item {
        SettingRadioDialogRow(
            primaryText = "Mock purchase outcome",
            secondaryText = state.config.purchaseOutcome.label,
            icon = rememberVectorPainter(Icons.Outlined.Payments),
            options = MockPurchaseOutcome.entries,
            savedOption = state.config.purchaseOutcome,
            optionToLocalisedString = { it.label },
            onSave = actions.onPurchaseOutcomeChange,
            enabled = state.config.isEnabled,
        )
    }
    item {
        OpenPaywallRow(onOpenPaywall = actions.onOpenPaywallClick)
    }
}

@Composable
private fun OpenPaywallRow(
    onOpenPaywall: (OnboardingUpgradeSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastPaywall by rememberSaveable { mutableStateOf(DeveloperPaywall.Generic) }
    SettingRadioDialogRow(
        primaryText = "Open upgrade paywall",
        secondaryText = "Generic or feature paywalls",
        icon = rememberVectorPainter(Icons.Outlined.Storefront),
        options = DeveloperPaywall.entries,
        savedOption = lastPaywall,
        optionToLocalisedString = { it.label },
        onSave = { paywall ->
            lastPaywall = paywall
            onOpenPaywall(paywall.source)
        },
        modifier = modifier,
    )
}

@Composable
private fun MockBillingToggle(
    state: MockBillingState,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Use mock Google Play billing",
        secondaryText = if (state.canToggleMock) {
            "Use local products, offers and purchases instead of Google Play"
        } else {
            "Always on. Google Play only works with the Play Store package name"
        },
        icon = rememberVectorPainter(Icons.Outlined.ShoppingCart),
        toggle = SettingRowToggle.Switch(checked = state.config.isEnabled, enabled = state.canToggleMock),
        enabled = state.canToggleMock,
        modifier = modifier.toggleable(
            value = state.config.isEnabled,
            enabled = state.canToggleMock,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    )
}

@Composable
private fun OfferEligibilityRow(
    offer: SubscriptionOffer,
    isEligible: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIntroOfferPlusEnabled by FeatureFlag.isEnabledFlow(Feature.INTRO_PLUS_OFFER_ENABLED).collectAsStateWithLifecycle()
    SettingRow(
        primaryText = "Eligible for ${offer.label}",
        secondaryText = if (offer == SubscriptionOffer.IntroOffer && !isIntroOfferPlusEnabled) {
            "Also turn on Intro Offer Plus in Beta features to see the 50% offer"
        } else {
            null
        },
        icon = rememberVectorPainter(Icons.Outlined.LocalOffer),
        toggle = SettingRowToggle.Checkbox(checked = isEligible, enabled = enabled),
        enabled = enabled,
        modifier = modifier.toggleable(
            value = isEligible,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ),
    )
}

@Composable
private fun FailProductsLoadingRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = "Fail to load products",
        secondaryText = "Simulate Google Play being unavailable",
        icon = rememberVectorPainter(Icons.Outlined.CloudOff),
        toggle = SettingRowToggle.Switch(checked = checked, enabled = enabled),
        enabled = enabled,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    )
}

private val SubscriptionOffer.label
    get() = when (this) {
        SubscriptionOffer.IntroOffer -> "intro offer (50% off first year)"
        SubscriptionOffer.Trial -> "free trial"
        SubscriptionOffer.Referral -> "referral guest pass"
        SubscriptionOffer.Winback -> "winback offer"
    }

private val MockPurchaseOutcome.label
    get() = when (this) {
        MockPurchaseOutcome.Success -> "Success"
        MockPurchaseOutcome.Cancelled -> "User cancelled"
        MockPurchaseOutcome.Error -> "Error"
    }
