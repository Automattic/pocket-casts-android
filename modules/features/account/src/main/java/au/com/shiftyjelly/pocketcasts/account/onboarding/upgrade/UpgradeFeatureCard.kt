package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.account.R
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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

data class FeatureCardsState(
    val subscriptions: List<Subscription>,
    val currentFeatureCard: UpgradeFeatureCard,
) {
    val featureCards = SubscriptionTier.values().toList()
        .filter { tier -> tier != SubscriptionTier.UNKNOWN && tier in subscriptions.map { it.tier } }
        .map { it.toUpgradeFeatureCard() }

    val showPageIndicator = featureCards.size > 1
}

fun SubscriptionTier.toUpgradeFeatureCard() = when (this) {
    SubscriptionTier.PLUS -> UpgradeFeatureCard.PLUS
    SubscriptionTier.PATRON -> UpgradeFeatureCard.PATRON
    SubscriptionTier.UNKNOWN -> throw IllegalStateException("Unknown subscription tier")
}
