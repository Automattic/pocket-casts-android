package au.com.shiftyjelly.pocketcasts.repositories.payment

import au.com.shiftyjelly.pocketcasts.payment.AcknowledgedSubscription
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.Purchase
import au.com.shiftyjelly.pocketcasts.payment.PurchaseResult
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlans
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer

internal class LoggingPaymentListener : PaymentClient.Listener {
    override fun onLoadSubscriptionPlans() {
        log("Loading subscription plans")
    }

    override fun onSubscriptionPlansLoaded(result: PaymentResult<SubscriptionPlans>) {
        val message = when (result) {
            is PaymentResult.Success -> "Loaded subscription plans"
            is PaymentResult.Failure -> "Failed to load subscription plans. ${result.code}, ${result.message}"
        }
        log(message)
    }

    override fun onloadAcknowledgedSubscriptions() {
        log("Loading acknowledged subscriptions")
    }

    override fun onAcknowledgedSubscriptionsLoaded(result: PaymentResult<List<AcknowledgedSubscription>>) {
        val message = when (result) {
            is PaymentResult.Success -> "Loaded acknowledged subscriptions"
            is PaymentResult.Failure -> "Failed to load subscription plans. ${result.code}, ${result.message}"
        }
        log(message)
    }

    override fun onPurchaseSubscriptionPlan(key: SubscriptionPlan.Key) {
        log("Purchasing subscrition plan: $key")
    }

    override fun onSubscriptionPurchased(key: SubscriptionPlan.Key, purchaseSource: String, result: PurchaseResult) {
        val message = when (result) {
            is PurchaseResult.Purchased -> "Purchased subscription plan: $key"
            is PurchaseResult.Cancelled -> "Cancelled subscription plan purchase: $key"
            is PurchaseResult.Failure -> "Failed to purchase subscription plan: $key, ${result.code}"
        }
        log(message)
    }

    override fun onConfirmPurchase(purchase: Purchase) {
        log("Confirming purchase: ${purchase.state}")
    }

    override fun onPurchaseConfirmed(result: PaymentResult<Purchase>) {
        val message = when (result) {
            is PaymentResult.Success -> "Loaded subscription plans"
            is PaymentResult.Failure -> "Failed to load subscription plans. ${result.code}, ${result.message}"
        }
        log(message)
    }

    override fun onMessage(message: String) {
        log(message)
    }

    private fun log(message: String) {
        LogBuffer.i(LogBuffer.TAG_SUBSCRIPTIONS, message)
    }
}
