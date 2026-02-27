package au.com.shiftyjelly.pocketcasts.utils.extensions

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull

/**
 * Get the appropriate yearly plan, with optional installment plan support.
 *
 * When shouldUseInstallmentPlan is true AND installment plan is available
 * (user in supported country), returns the installment plan. Otherwise returns the upfront yearly plan.
 *
 * Note: Installment plans are only available in Brazil, France, Italy, and Spain.
 * Google Play automatically filters out installment plans for users in other countries.
 */
fun SubscriptionPlans.getYearlyPlanWithFeatureFlag(
    tier: SubscriptionTier,
    shouldUseInstallmentPlan: Boolean = false,
): SubscriptionPlan.Base {
    if (tier == SubscriptionTier.Plus && shouldUseInstallmentPlan) {
        val installmentPlanResult = findInstallmentPlan(tier, BillingCycle.Yearly)
        val installmentPlan = installmentPlanResult.getOrNull()
        if (installmentPlan != null) {
            return installmentPlan
        }
    }

    return getBasePlan(tier, BillingCycle.Yearly)
}
