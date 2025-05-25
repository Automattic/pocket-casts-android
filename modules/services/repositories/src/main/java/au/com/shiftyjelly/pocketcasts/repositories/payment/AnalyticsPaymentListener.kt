package au.com.shiftyjelly.pocketcasts.repositories.payment

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.PurchaseResult
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier

internal class AnalyticsPaymentListener(
    private val tracker: AnalyticsTracker,
) : PaymentClient.Listener {
    override fun onSubscriptionPurchased(key: SubscriptionPlan.Key, purchaseSource: String, result: PurchaseResult) {
        val baseProperties = mutableMapOf(
            "tier" to key.tier.analyticsValue,
            "frequency" to key.billingCycle.analyticsValue,
            "offer_type" to (key.offer?.analyticsValue ?: "none"),
            "product" to key.productLegacyAnalyticsValue(),
            "source" to purchaseSource,
        )
        val (event, properties) = when (result) {
            is PurchaseResult.Purchased -> {
                AnalyticsEvent.PURCHASE_SUCCESSFUL to baseProperties
            }

            is PurchaseResult.Cancelled -> {
                val properties = baseProperties + PaymentResultCode.UserCancelled.analyticProperties()
                AnalyticsEvent.PURCHASE_CANCELLED to baseProperties + properties
            }

            is PurchaseResult.Failure -> {
                val properties = baseProperties + result.code.analyticProperties()
                AnalyticsEvent.PURCHASE_FAILED to properties
            }
        }
        tracker.track(event, properties)
    }

    private fun PaymentResultCode.analyticProperties() = buildMap {
        put("error", analyticsValue)
        if (this@analyticProperties is PaymentResultCode.Unknown) {
            put("error_code", code)
        }
    }

    private fun SubscriptionPlan.Key.productLegacyAnalyticsValue() = when (tier) {
        SubscriptionTier.Plus -> billingCycle.analyticsValue
        SubscriptionTier.Patron -> productId
    }
}
