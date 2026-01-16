package au.com.shiftyjelly.pocketcasts.utils.extensions

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

/**
 * Get the appropriate yearly plan based on the NEW_INSTALLMENT_PLAN feature flag.
 *
 * When feature flag is ON and installment plan is available (user in supported country),
 * returns the installment plan. Otherwise returns the upfront yearly plan.
 *
 * Note: Installment plans are only available in Brazil, France, Italy, and Spain.
 * Google Play automatically filters out installment plans for users in other countries.
 */
fun SubscriptionPlans.getYearlyPlanWithFeatureFlag(
    tier: SubscriptionTier,
): SubscriptionPlan.Base {
    val isInstallmentEnabled = FeatureFlag.isEnabled(Feature.NEW_INSTALLMENT_PLAN)

    if (tier == SubscriptionTier.Plus && isInstallmentEnabled) {
        val installmentPlanResult = findInstallmentPlan(tier, BillingCycle.Yearly)
        val installmentPlan = installmentPlanResult.getOrNull()
        if (installmentPlan != null) {
            return installmentPlan
        }
    }

    return getBasePlan(tier, BillingCycle.Yearly)
}
