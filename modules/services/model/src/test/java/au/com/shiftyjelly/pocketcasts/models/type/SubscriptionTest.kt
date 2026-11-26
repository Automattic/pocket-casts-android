package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionTest {
    @Test
    fun `auto-renewing subscription is not expiring`() {
        val subscription = subscription(isAutoRenewing = true, expiresInDays = 10)

        assertFalse(subscription.isExpiring)
        assertNull(subscription.expiresIn)
    }

    @Test
    fun `cancelled subscription within the expiring window is expiring`() {
        val subscription = subscription(isAutoRenewing = false, expiresInDays = 10)

        assertTrue(subscription.isExpiring)
        val expiresIn = requireNotNull(subscription.expiresIn)
        assertTrue(expiresIn in (10.days - 1.hours)..10.days)
    }

    @Test
    fun `cancelled subscription beyond the expiring window is not expiring`() {
        val subscription = subscription(isAutoRenewing = false, expiresInDays = 40)

        assertFalse(subscription.isExpiring)
        assertNull(subscription.expiresIn)
    }

    @Test
    fun `auto-renewing subscription beyond the expiring window is not expiring`() {
        val subscription = subscription(isAutoRenewing = true, expiresInDays = 40)

        assertFalse(subscription.isExpiring)
        assertNull(subscription.expiresIn)
    }

    @Test
    fun `cancelled subscription past its expiry date has no time left`() {
        val subscription = subscription(isAutoRenewing = false, expiresInDays = -1)

        assertTrue(subscription.isExpiring)
        assertNull(subscription.expiresIn)
    }

    @Test
    fun `subscription is managed on another platform only for iOS and Web`() {
        val results = SubscriptionPlatform.entries.associateWith { platform ->
            subscription(isAutoRenewing = true, expiresInDays = 10, platform = platform).isManagedOnAnotherPlatform
        }

        assertEquals(
            mapOf(
                SubscriptionPlatform.Android to false,
                SubscriptionPlatform.iOS to true,
                SubscriptionPlatform.Web to true,
                SubscriptionPlatform.Gift to false,
                SubscriptionPlatform.Unknown to false,
            ),
            results,
        )
    }

    private fun subscription(
        isAutoRenewing: Boolean,
        expiresInDays: Long,
        platform: SubscriptionPlatform = SubscriptionPlatform.Android,
    ) = Subscription(
        tier = SubscriptionTier.Plus,
        billingCycle = BillingCycle.Monthly,
        platform = platform,
        expiryDate = Instant.now().plus(expiresInDays, ChronoUnit.DAYS),
        isAutoRenewing = isAutoRenewing,
        giftDays = 0,
    )
}
