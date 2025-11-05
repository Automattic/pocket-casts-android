package au.com.shiftyjelly.pocketcasts.repositories.payment

import android.app.Activity
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.Tracker
import au.com.shiftyjelly.pocketcasts.payment.FakePaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

class AnalyticsPaymentListenerTest {
    private val tracker = TestTracker()

    private val dataSource = FakePaymentDataSource()
    private val paymentClient = PaymentClient.test(
        dataSource,
        AnalyticsPaymentListener(AnalyticsTracker.test(tracker, isFirstPartyEnabled = true)),
    )

    @Test
    fun `successfull purchase`() = runTest {
        val key = SubscriptionPlan.PlusYearlyPreview.key

        paymentClient.purchaseSubscriptionPlan(key, "purchase_source", mock<Activity>())

        val event = tracker.events.first()
        event.assertType(AnalyticsEvent.PURCHASE_SUCCESSFUL)
        event.assertProperties(
            mapOf(
                "tier" to "plus",
                "frequency" to "yearly",
                "offer_type" to "none",
                "product" to "yearly",
                "source" to "purchase_source",
            ),
        )
    }

    @Test
    fun `cancelled purchase`() = runTest {
        val key = SubscriptionPlan.PatronMonthlyPreview.key.copy(offer = SubscriptionOffer.Referral)

        dataSource.purchasedProductsResultCode = PaymentResultCode.UserCancelled
        paymentClient.purchaseSubscriptionPlan(key, "purchase_source", mock<Activity>())

        val event = tracker.events.first()
        event.assertType(AnalyticsEvent.PURCHASE_CANCELLED)
        event.assertProperties(
            mapOf(
                "tier" to "patron",
                "frequency" to "monthly",
                "offer_type" to "referral",
                "product" to "com.pocketcasts.monthly.patron",
                "source" to "purchase_source",
                "error" to "user_cancelled",
            ),
        )
    }

    @Test
    fun `failure purchase`() = runTest {
        val key = SubscriptionPlan.PlusYearlyPreview.key

        dataSource.purchasedProductsResultCode = PaymentResultCode.Unknown(404)
        paymentClient.purchaseSubscriptionPlan(key, "purchase_source", mock<Activity>())

        val event = tracker.events.first()
        event.assertType(AnalyticsEvent.PURCHASE_FAILED)
        event.assertProperties(
            mapOf(
                "tier" to "plus",
                "frequency" to "yearly",
                "offer_type" to "none",
                "product" to "yearly",
                "source" to "purchase_source",
                "error" to "unknown",
                "error_code" to 404,
            ),
        )
    }

    @Test
    fun `legacy product property`() = runTest {
        val keys = listOf(
            SubscriptionPlan.PlusMonthlyPreview.key,
            SubscriptionPlan.PlusYearlyPreview.key,
            SubscriptionPlan.PatronMonthlyPreview.key,
            SubscriptionPlan.PatronYearlyPreview.key,
        )

        for (key in keys) {
            paymentClient.purchaseSubscriptionPlan(key, "purchase_source", mock<Activity>())
        }

        val productProperties = tracker.events.map { it.properties["product"] }
        assertEquals(
            listOf(
                "monthly",
                "yearly",
                "com.pocketcasts.monthly.patron",
                "com.pocketcasts.yearly.patron",
            ),
            productProperties,
        )
    }
}

private class TestTracker : Tracker {
    private val _events = mutableListOf<TrackEvent>()

    val events get() = _events.toList()

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
        _events += TrackEvent(event, properties)
    }

    override fun refreshMetadata() = Unit

    override fun flush() = Unit

    override fun clearAllData() = Unit
}

private data class TrackEvent(
    val type: AnalyticsEvent,
    val properties: Map<String, Any>,
) {
    fun assertType(type: AnalyticsEvent) {
        assertEquals(type, this.type)
    }

    fun assertProperties(properties: Map<String, Any>) {
        assertEquals(properties, this.properties)
    }
}
