package au.com.shiftyjelly.pocketcasts.repositories.payment

import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.PurchaseResult
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PurchaseCancelledEvent
import com.automattic.eventhorizon.PurchaseFailedEvent
import com.automattic.eventhorizon.PurchaseSuccessfulEvent

internal class AnalyticsPaymentListener(
    private val eventHorizon: EventHorizon,
) : PaymentClient.Listener {
    override fun onSubscriptionPurchased(key: SubscriptionPlan.Key, purchaseSource: String, purchaseFlow: String?, result: PurchaseResult) {
        val event = when (result) {
            is PurchaseResult.Purchased -> {
                PurchaseSuccessfulEvent(
                    tier = key.tier.eventHorizonValue,
                    frequency = key.billingCycle.eventHorizonValue,
                    offerType = key.offer?.eventHorizonValue,
                    isInstallment = key.isInstallment,
                    source = purchaseSource,
                    flow = purchaseFlow,
                )
            }

            is PurchaseResult.Cancelled -> {
                PurchaseCancelledEvent(
                    tier = key.tier.eventHorizonValue,
                    frequency = key.billingCycle.eventHorizonValue,
                    offerType = key.offer?.eventHorizonValue,
                    isInstallment = key.isInstallment,
                    source = purchaseSource,
                    flow = purchaseFlow,
                )
            }

            is PurchaseResult.Failure -> {
                PurchaseFailedEvent(
                    tier = key.tier.eventHorizonValue,
                    frequency = key.billingCycle.eventHorizonValue,
                    offerType = key.offer?.eventHorizonValue,
                    isInstallment = key.isInstallment,
                    source = purchaseSource,
                    flow = purchaseFlow,
                    error = result.code.analyticsValue,
                    errorCode = (result.code as? PaymentResultCode.Unknown)?.code?.toLong(),
                )
            }
        }
        eventHorizon.track(event)
    }
}
