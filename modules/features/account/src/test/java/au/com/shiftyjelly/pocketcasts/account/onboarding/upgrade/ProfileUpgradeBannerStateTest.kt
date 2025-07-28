package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileUpgradeBannerStateTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun `state without current subscription with intro offer enabled`() {
        val introSubscritpionPlan = SubscriptionPlans.Preview.findOfferPlan(
            SubscriptionTier.Plus,
            BillingCycle.Yearly,
            SubscriptionOffer.IntroOffer,
        ).getOrNull()!!

        FeatureFlag.setEnabled(Feature.INTRO_PLUS_OFFER_ENABLED, true)
        val state = ProfileUpgradeBannerState.OldProfileUpgradeBannerState(
            subscriptionPlans = SubscriptionPlans.Preview,
            currentSubscription = null,
            isRenewingSubscription = false,
            selectedFeatureCard = null,
        )

        assertEquals(
            listOf(
                OnboardingSubscriptionPlan.create(introSubscritpionPlan).getOrNull()!!,
                OnboardingSubscriptionPlan.create(SubscriptionPlan.PatronYearlyPreview),
            ),
            state.onboardingPlans,
        )
    }

    @Test
    fun `state without current subscription with intro offer disabled`() {
        val trialSubscriptionPlan = SubscriptionPlans.Preview.findOfferPlan(
            SubscriptionTier.Plus,
            BillingCycle.Yearly,
            SubscriptionOffer.Trial,
        ).getOrNull()!!

        FeatureFlag.setEnabled(Feature.INTRO_PLUS_OFFER_ENABLED, false)
        val state = ProfileUpgradeBannerState.OldProfileUpgradeBannerState(
            subscriptionPlans = SubscriptionPlans.Preview,
            currentSubscription = null,
            isRenewingSubscription = false,
            selectedFeatureCard = null,
        )

        assertEquals(
            listOf(
                OnboardingSubscriptionPlan.create(trialSubscriptionPlan).getOrNull()!!,
                OnboardingSubscriptionPlan.create(SubscriptionPlan.PatronYearlyPreview),
            ),
            state.onboardingPlans,
        )
    }

    @Test
    fun `state with Plus Monthly renewing current subscription`() {
        val state = ProfileUpgradeBannerState.OldProfileUpgradeBannerState(
            subscriptionPlans = SubscriptionPlans.Preview,
            currentSubscription = SubscriptionPlan.PlusMonthlyPreview.key,
            isRenewingSubscription = true,
            selectedFeatureCard = null,
        )

        assertEquals(
            listOf(
                OnboardingSubscriptionPlan.create(SubscriptionPlan.PlusMonthlyPreview),
                OnboardingSubscriptionPlan.create(SubscriptionPlan.PatronMonthlyPreview),
            ),
            state.onboardingPlans,
        )
    }

    @Test
    fun `state with Plus Yearly renewing current subscription`() {
        val state = ProfileUpgradeBannerState.OldProfileUpgradeBannerState(
            subscriptionPlans = SubscriptionPlans.Preview,
            currentSubscription = SubscriptionPlan.PlusYearlyPreview.key,
            isRenewingSubscription = true,
            selectedFeatureCard = null,
        )

        assertEquals(
            listOf(
                OnboardingSubscriptionPlan.create(SubscriptionPlan.PlusYearlyPreview),
                OnboardingSubscriptionPlan.create(SubscriptionPlan.PatronYearlyPreview),
            ),
            state.onboardingPlans,
        )
    }

    @Test
    fun `state with Patron Monthly renewing current subscription`() {
        val state = ProfileUpgradeBannerState.OldProfileUpgradeBannerState(
            subscriptionPlans = SubscriptionPlans.Preview,
            currentSubscription = SubscriptionPlan.PatronMonthlyPreview.key,
            isRenewingSubscription = true,
            selectedFeatureCard = null,
        )

        assertEquals(
            listOf(
                OnboardingSubscriptionPlan.create(SubscriptionPlan.PatronMonthlyPreview),
            ),
            state.onboardingPlans,
        )
    }

    @Test
    fun `state with Patron Yearly renewing current subscription`() {
        val state = ProfileUpgradeBannerState.OldProfileUpgradeBannerState(
            subscriptionPlans = SubscriptionPlans.Preview,
            currentSubscription = SubscriptionPlan.PatronYearlyPreview.key,
            isRenewingSubscription = true,
            selectedFeatureCard = null,
        )

        assertEquals(
            listOf(
                OnboardingSubscriptionPlan.create(SubscriptionPlan.PatronYearlyPreview),
            ),
            state.onboardingPlans,
        )
    }

    @Test
    fun `state with not renewing current subscription`() {
        val state = ProfileUpgradeBannerState.OldProfileUpgradeBannerState(
            subscriptionPlans = SubscriptionPlans.Preview,
            currentSubscription = SubscriptionPlan.PlusYearlyPreview.key,
            isRenewingSubscription = false,
            selectedFeatureCard = null,
        )

        assertTrue(state.onboardingPlans.isEmpty())
    }
}
