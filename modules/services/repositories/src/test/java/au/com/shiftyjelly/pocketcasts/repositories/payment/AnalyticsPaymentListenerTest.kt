package au.com.shiftyjelly.pocketcasts.repositories.payment

import android.app.Activity
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.FakePaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PurchaseCancelledEvent
import com.automattic.eventhorizon.PurchaseFailedEvent
import com.automattic.eventhorizon.PurchaseSuccessfulEvent
import com.automattic.eventhorizon.SubscriptionFrequencyType
import com.automattic.eventhorizon.SubscriptionOfferType
import com.automattic.eventhorizon.SubscriptionTierType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

class AnalyticsPaymentListenerTest {
    private val eventSink = TestEventSink()

    private val dataSource = FakePaymentDataSource()
    private val paymentClient = PaymentClient.test(
        dataSource,
        AnalyticsPaymentListener(EventHorizon(eventSink)),
    )

    @Test
    fun `successful purchase`() = runTest {
        val key = SubscriptionPlan.PlusYearlyPreview.key

        paymentClient.purchaseSubscriptionPlan(key, "purchase_source", mock<Activity>())

        val event = eventSink.pollEvent()
        assertEquals(
            PurchaseSuccessfulEvent(
                tier = SubscriptionTierType.Plus,
                frequency = SubscriptionFrequencyType.Yearly,
                isInstallment = false,
                source = "purchase_source",
            ),
            event,
        )
    }

    @Test
    fun `cancelled purchase`() = runTest {
        val key = SubscriptionPlan.PatronMonthlyPreview.key.copy(offer = SubscriptionOffer.Referral)

        dataSource.purchasedProductsResultCode = PaymentResultCode.UserCancelled
        paymentClient.purchaseSubscriptionPlan(key, "purchase_source", mock<Activity>())

        val event = eventSink.pollEvent()
        assertEquals(
            PurchaseCancelledEvent(
                tier = SubscriptionTierType.Patron,
                frequency = SubscriptionFrequencyType.Monthly,
                offerType = SubscriptionOfferType.Referral,
                isInstallment = false,
                source = "purchase_source",
            ),
            event,
        )
    }

    @Test
    fun `failure purchase`() = runTest {
        val key = SubscriptionPlan.PlusYearlyPreview.key

        dataSource.purchasedProductsResultCode = PaymentResultCode.Unknown(404)
        paymentClient.purchaseSubscriptionPlan(key, "purchase_source", mock<Activity>())

        val event = eventSink.pollEvent()
        assertEquals(
            PurchaseFailedEvent(
                tier = SubscriptionTierType.Plus,
                frequency = SubscriptionFrequencyType.Yearly,
                isInstallment = false,
                source = "purchase_source",
                error = "unknown",
                errorCode = 404,
            ),
            event,
        )
    }

    @Test
    fun `installment plan purchase`() = runTest {
        val key = SubscriptionPlan.Key(
            tier = SubscriptionTier.Plus,
            billingCycle = BillingCycle.Yearly,
            offer = null,
            isInstallment = true,
        )

        paymentClient.purchaseSubscriptionPlan(key, "purchase_source", mock<Activity>())

        val event = eventSink.pollEvent()
        assertEquals(
            PurchaseSuccessfulEvent(
                tier = SubscriptionTierType.Plus,
                frequency = SubscriptionFrequencyType.Yearly,
                isInstallment = true,
                source = "purchase_source",
            ),
            event,
        )
    }
}
