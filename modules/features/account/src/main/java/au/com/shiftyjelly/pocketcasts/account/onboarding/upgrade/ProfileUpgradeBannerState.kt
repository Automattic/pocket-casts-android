package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.flatMap
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

sealed interface ProfileUpgradeBannerState {
    data class NewOnboardingUpgradeState(
        val recommendedSubscription: OnboardingSubscriptionPlan,
    ) : ProfileUpgradeBannerState

    data class OldProfileUpgradeBannerState(
        val subscriptionPlans: SubscriptionPlans,
        val currentSubscription: SubscriptionPlan.Key?,
        val isRenewingSubscription: Boolean,
        val selectedFeatureCard: SubscriptionPlan.Key?,
    ) : ProfileUpgradeBannerState {
        val onboardingPlans = buildList<OnboardingSubscriptionPlan> {
            when (currentSubscription?.billingCycle) {
                null -> {
                    add(plusYearlyPlanWithOffer())
                    add(patronYearlyPlan())
                }
                BillingCycle.Yearly -> {
                    if (isRenewingSubscription) {
                        add(plusYearlyPlan())
                        add(patronYearlyPlan())
                    }
                }
                BillingCycle.Monthly -> {
                    if (isRenewingSubscription) {
                        add(plusMonthlyPlan())
                        add(patronMonthlyPlan())
                    }
                }
            }
            if (currentSubscription?.tier == SubscriptionTier.Patron) {
                removeIf { it.key.tier != SubscriptionTier.Patron }
            }
        }

        private fun plusYearlyPlanWithOffer(): OnboardingSubscriptionPlan {
            val offer = if (FeatureFlag.isEnabled(Feature.INTRO_PLUS_OFFER_ENABLED)) {
                SubscriptionOffer.IntroOffer
            } else {
                SubscriptionOffer.Trial
            }
            return subscriptionPlans.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Yearly, offer)
                .flatMap { OnboardingSubscriptionPlan.create(it) }
                .getOrNull()
                ?: plusYearlyPlan()
        }

        private fun plusYearlyPlan(): OnboardingSubscriptionPlan {
            return OnboardingSubscriptionPlan.create(subscriptionPlans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Yearly))
        }

        private fun patronYearlyPlan(): OnboardingSubscriptionPlan {
            return OnboardingSubscriptionPlan.create(subscriptionPlans.getBasePlan(SubscriptionTier.Patron, BillingCycle.Yearly))
        }

        private fun plusMonthlyPlan(): OnboardingSubscriptionPlan {
            return OnboardingSubscriptionPlan.create(subscriptionPlans.getBasePlan(SubscriptionTier.Plus, BillingCycle.Monthly))
        }

        private fun patronMonthlyPlan(): OnboardingSubscriptionPlan {
            return OnboardingSubscriptionPlan.create(subscriptionPlans.getBasePlan(SubscriptionTier.Patron, BillingCycle.Monthly))
        }
    }
}
