package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
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

    private fun subscription(isAutoRenewing: Boolean, expiresInDays: Long) = Subscription(
        tier = SubscriptionTier.Plus,
        billingCycle = BillingCycle.Monthly,
        platform = SubscriptionPlatform.Android,
        expiryDate = Instant.now().plus(expiresInDays, ChronoUnit.DAYS),
        isAutoRenewing = isAutoRenewing,
        giftDays = 0,
    )
}
