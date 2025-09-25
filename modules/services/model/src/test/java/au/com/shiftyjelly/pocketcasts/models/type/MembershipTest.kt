package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class MembershipTest {
    class NoSubscriptionWithFeatures {
        @Test
        fun `feature list is respected`() {
            val membership = Membership(
                subscription = null,
                createdAt = null,
                features = listOf(MembershipFeature.NoBannerAds),
            )

            assertTrue(membership.hasFeature(MembershipFeature.NoBannerAds))
            assertFalse(membership.hasFeature(MembershipFeature.NoDiscoverAds))
        }
    }

    @RunWith(Parameterized::class)
    class TierPerFeature(
        private val feature: MembershipFeature,
        private val tier: SubscriptionTier?,
    ) {
        private val membership = Membership(
            subscription = if (tier != null) {
                Subscription(
                    tier = tier,
                    billingCycle = BillingCycle.Monthly,
                    platform = SubscriptionPlatform.Android,
                    expiryDate = Instant.MAX,
                    isAutoRenewing = true,
                    giftDays = 0,
                )
            } else {
                null
            },
            createdAt = null,
            features = emptyList(),
        )

        @Test
        fun `feature availability per tier`() {
            val hasFeature = membership.hasFeature(feature)

            val expected = when (tier) {
                SubscriptionTier.Plus -> when (feature) {
                    MembershipFeature.NoBannerAds -> true
                    MembershipFeature.NoDiscoverAds -> true
                }

                SubscriptionTier.Patron -> when (feature) {
                    MembershipFeature.NoBannerAds -> true
                    MembershipFeature.NoDiscoverAds -> true
                }

                null -> when (feature) {
                    MembershipFeature.NoBannerAds -> false
                    MembershipFeature.NoDiscoverAds -> false
                }
            }

            assertEquals(expected, hasFeature)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "Feature: {0}, Tier: {1}")
            fun params() = MembershipFeature.entries.flatMap { feature ->
                val tiers = (SubscriptionTier.entries + null)
                tiers.map { tier ->
                    arrayOf(feature, tier)
                }
            }
        }
    }
}
