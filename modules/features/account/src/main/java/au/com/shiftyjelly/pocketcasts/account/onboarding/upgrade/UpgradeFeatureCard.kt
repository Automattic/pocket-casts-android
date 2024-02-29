package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.account.R
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeFeatureCard.PATRON.getTitleForSource as getTitleForSourcePatron
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.UpgradeFeatureCard.PLUS.getTitleForSource as getTitleForSourcePlus
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class UpgradeFeatureCard(
    val titleRes: (OnboardingUpgradeSource) -> Int,
    @StringRes val shortNameRes: Int,
    @DrawableRes val backgroundGlowsRes: Int,
    @DrawableRes val iconRes: Int,
    val featureItems: (SubscriptionFrequency) -> List<UpgradeFeatureItem>,
    val subscriptionTier: SubscriptionTier,
) {
    data object PLUS : UpgradeFeatureCard(
        titleRes = { source -> getTitleForSourcePlus(source) },
        shortNameRes = LR.string.pocket_casts_plus_short,
        backgroundGlowsRes = R.drawable.upgrade_background_plus_glows,
        iconRes = IR.drawable.ic_plus,
        featureItems = { subscriptionFreq ->
            when (subscriptionFreq) {
                SubscriptionFrequency.YEARLY -> PlusUpgradeFeatureItem.entries.filter { it.isYearlyFeature }
                SubscriptionFrequency.MONTHLY, SubscriptionFrequency.NONE -> PlusUpgradeFeatureItem.entries.filter { it.isMonthlyFeature }
            }
        },
        subscriptionTier = SubscriptionTier.PLUS,
    ) {
        fun getTitleForSource(
            source: OnboardingUpgradeSource,
        ) = when {
            (
                source in listOf(OnboardingUpgradeSource.SKIP_CHAPTERS, OnboardingUpgradeSource.WHATS_NEW_SKIP_CHAPTERS) &&
                    FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS) &&
                    SubscriptionTier.fromFeatureTier(Feature.DESELECT_CHAPTERS) == SubscriptionTier.PLUS
                )
            -> LR.string.skip_chapters_plus_prompt

            else -> LR.string.onboarding_plus_features_title
        }
    }

    data object PATRON : UpgradeFeatureCard(
        titleRes = { source -> getTitleForSourcePatron(source) },
        shortNameRes = LR.string.pocket_casts_patron_short,
        backgroundGlowsRes = R.drawable.upgrade_background_patron_glows,
        iconRes = IR.drawable.ic_patron,
        featureItems = { subscriptionFreq ->
            when (subscriptionFreq) {
                SubscriptionFrequency.YEARLY -> PatronUpgradeFeatureItem.entries.filter { it.isYearlyFeature }
                SubscriptionFrequency.MONTHLY, SubscriptionFrequency.NONE -> PatronUpgradeFeatureItem.entries.filter { it.isMonthlyFeature }
            }
        },
        subscriptionTier = SubscriptionTier.PATRON,
    ) {
        fun getTitleForSource(
            source: OnboardingUpgradeSource,
        ) = when {
            (
                source in listOf(OnboardingUpgradeSource.SKIP_CHAPTERS, OnboardingUpgradeSource.WHATS_NEW_SKIP_CHAPTERS) &&
                    FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS) &&
                    SubscriptionTier.fromFeatureTier(Feature.DESELECT_CHAPTERS) == SubscriptionTier.PATRON
                )
            -> LR.string.skip_chapters_patron_prompt

            else -> LR.string.onboarding_patron_features_title
        }
    }
}

data class FeatureCardsState(
    val subscriptions: List<Subscription>,
    val currentFeatureCard: UpgradeFeatureCard,
    val currentFrequency: SubscriptionFrequency,
) {
    val featureCards = SubscriptionTier.entries
        .filter { tier -> tier != SubscriptionTier.UNKNOWN && tier in subscriptions.map { it.tier } }
        .map { it.toUpgradeFeatureCard() }

    val showPageIndicator = featureCards.size > 1
}

fun SubscriptionTier.toUpgradeFeatureCard() = when (this) {
    SubscriptionTier.PLUS -> UpgradeFeatureCard.PLUS
    SubscriptionTier.PATRON -> UpgradeFeatureCard.PATRON
    SubscriptionTier.UNKNOWN -> throw IllegalStateException("Unknown subscription tier")
}
