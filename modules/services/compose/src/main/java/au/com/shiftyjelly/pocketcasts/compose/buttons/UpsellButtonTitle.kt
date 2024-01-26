package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun UpsellButtonTitle(
    tier: SubscriptionTier,
    hasFreeTrial: Boolean,
) = if (hasFreeTrial && !FeatureFlag.isEnabled(Feature.INTRO_PLUS_OFFER_ENABLED)) {
    stringResource(LR.string.profile_start_free_trial)
} else {
    stringResource(
        LR.string.upgrade_to,
        when (tier) {
            SubscriptionTier.PATRON -> stringResource(LR.string.pocket_casts_patron_short)
            SubscriptionTier.PLUS -> stringResource(LR.string.pocket_casts_plus_short)
            SubscriptionTier.UNKNOWN -> stringResource(LR.string.pocket_casts_plus_short)
        },
    )
}
