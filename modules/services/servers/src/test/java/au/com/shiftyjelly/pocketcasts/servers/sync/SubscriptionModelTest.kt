package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import java.time.Instant
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionModelTest {
    @Test
    fun `toMembership maps isInstallment field when true`() {
        val expiryDate = Date()
        val subscriptionResponse = SubscriptionResponse(
            type = 1,
            tier = "plus",
            platform = 2,
            frequency = 2,
            expiryDate = expiryDate,
            autoRenewing = true,
            giftDays = 0,
            isInstallment = true,
        )
        val statusResponse = SubscriptionStatusResponse(
            autoRenewing = true,
            expiryDate = expiryDate,
            giftDays = 0,
            paid = 1,
            platform = 2,
            frequency = 2,
            subscriptions = listOf(subscriptionResponse),
            type = 1,
            tier = "plus",
            index = 0,
            createdAt = Instant.now(),
            features = null,
        )

        val membership = statusResponse.toMembership()

        assertNotNull(membership.subscription)
        assertEquals(SubscriptionTier.Plus, membership.subscription?.tier)
        assertEquals(BillingCycle.Yearly, membership.subscription?.billingCycle)
        assertTrue(membership.subscription?.isInstallment == true)
    }

    @Test
    fun `toMembership maps isInstallment field when false`() {
        val expiryDate = Date()
        val subscriptionResponse = SubscriptionResponse(
            type = 1,
            tier = "plus",
            platform = 2,
            frequency = 2,
            expiryDate = expiryDate,
            autoRenewing = true,
            giftDays = 0,
            isInstallment = false,
        )
        val statusResponse = SubscriptionStatusResponse(
            autoRenewing = true,
            expiryDate = expiryDate,
            giftDays = 0,
            paid = 1,
            platform = 2,
            frequency = 2,
            subscriptions = listOf(subscriptionResponse),
            type = 1,
            tier = "plus",
            index = 0,
            createdAt = Instant.now(),
            features = null,
        )

        val membership = statusResponse.toMembership()

        assertNotNull(membership.subscription)
        assertEquals(SubscriptionTier.Plus, membership.subscription?.tier)
        assertEquals(BillingCycle.Yearly, membership.subscription?.billingCycle)
        assertFalse(membership.subscription?.isInstallment == true)
    }

    @Test
    fun `toMembership defaults isInstallment to false when not specified`() {
        val expiryDate = Date()
        val subscriptionResponse = SubscriptionResponse(
            type = 1,
            tier = "plus",
            platform = 2,
            frequency = 1,
            expiryDate = expiryDate,
            autoRenewing = true,
            giftDays = 0,
        )
        val statusResponse = SubscriptionStatusResponse(
            autoRenewing = true,
            expiryDate = expiryDate,
            giftDays = 0,
            paid = 1,
            platform = 2,
            frequency = 1,
            subscriptions = listOf(subscriptionResponse),
            type = 1,
            tier = "plus",
            index = 0,
            createdAt = Instant.now(),
            features = null,
        )

        val membership = statusResponse.toMembership()

        assertNotNull(membership.subscription)
        assertEquals(SubscriptionTier.Plus, membership.subscription?.tier)
        assertEquals(BillingCycle.Monthly, membership.subscription?.billingCycle)
        assertFalse(membership.subscription?.isInstallment == true)
    }

    @Test
    fun `toMembership uses top-level isInstallment as fallback when subscription-level is false`() {
        val expiryDate = Date()
        val subscriptionResponse = SubscriptionResponse(
            type = 1,
            tier = "plus",
            platform = 2,
            frequency = 2,
            expiryDate = expiryDate,
            autoRenewing = true,
            giftDays = 0,
            isInstallment = false,
        )
        val statusResponse = SubscriptionStatusResponse(
            autoRenewing = true,
            expiryDate = expiryDate,
            giftDays = 0,
            paid = 1,
            platform = 2,
            frequency = 2,
            subscriptions = listOf(subscriptionResponse),
            type = 1,
            tier = "plus",
            index = 0,
            createdAt = Instant.now(),
            features = null,
            isInstallment = true,
        )

        val membership = statusResponse.toMembership()

        assertTrue(membership.subscription?.isInstallment == true)
    }

    @Test
    fun `toMembership uses top-level isInstallment in fallback subscription when subscriptions list is empty`() {
        val expiryDate = Date()
        val statusResponse = SubscriptionStatusResponse(
            autoRenewing = true,
            expiryDate = expiryDate,
            giftDays = 0,
            paid = 1,
            platform = 2,
            frequency = 2,
            subscriptions = emptyList(),
            type = 1,
            tier = "plus",
            index = 0,
            createdAt = Instant.now(),
            features = null,
            isInstallment = true,
        )

        val membership = statusResponse.toMembership()

        assertTrue(membership.subscription?.isInstallment == true)
    }

    @Test
    fun `toMembership isInstallment is false when both top-level and subscription-level are false`() {
        val expiryDate = Date()
        val subscriptionResponse = SubscriptionResponse(
            type = 1,
            tier = "plus",
            platform = 2,
            frequency = 2,
            expiryDate = expiryDate,
            autoRenewing = true,
            giftDays = 0,
            isInstallment = false,
        )
        val statusResponse = SubscriptionStatusResponse(
            autoRenewing = true,
            expiryDate = expiryDate,
            giftDays = 0,
            paid = 1,
            platform = 2,
            frequency = 2,
            subscriptions = listOf(subscriptionResponse),
            type = 1,
            tier = "plus",
            index = 0,
            createdAt = Instant.now(),
            features = null,
            isInstallment = false,
        )

        val membership = statusResponse.toMembership()

        assertFalse(membership.subscription?.isInstallment == true)
    }
}
