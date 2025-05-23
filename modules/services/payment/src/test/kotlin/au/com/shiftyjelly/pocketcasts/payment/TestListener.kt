package au.com.shiftyjelly.pocketcasts.payment

import org.junit.Assert.assertEquals

class TestListener : PaymentClient.Listener {
    private val events = mutableListOf<Event>()
    private val messages = mutableListOf<String>()

    override fun onLoadSubscriptionPlans() {
        events += Event.LoadSubscriptionPlans
    }

    override fun onSubscriptionPlansLoaded(result: PaymentResult<SubscriptionPlans>) {
        events += when (result) {
            is PaymentResult.Success -> Event.LoadSubscriptionPlansSuccess
            is PaymentResult.Failure -> Event.LoadSubscriptionPlansFailure
        }
    }

    override fun onloadAcknowledgedSubscriptions() {
        events += Event.LoadAcknowledgedSubscriptions
    }

    override fun onAcknowledgedSubscriptionsLoaded(result: PaymentResult<List<AcknowledgedSubscription>>) {
        events += when (result) {
            is PaymentResult.Success -> Event.LoadAcknowledgedSubscriptionsSuccess
            is PaymentResult.Failure -> Event.LoadAcknowledgedSubscriptionsFailure
        }
    }

    override fun onPurchaseSubscriptionPlan(key: SubscriptionPlan.Key) {
        events += Event.PurchaseSubscriptionPlan
    }

    override fun onSubscriptionPurchased(key: SubscriptionPlan.Key, purchaseSource: String, result: PurchaseResult) {
        events += when (result) {
            is PurchaseResult.Purchased -> Event.PurchaseSubscriptionPlanSuccess
            is PurchaseResult.Cancelled -> Event.PurchaseSubscriptionPlanCancelled
            is PurchaseResult.Failure -> Event.PurchaseSubscriptionPlanFailure
        }
    }

    override fun onConfirmPurchase(purchase: Purchase) {
        events += Event.ConfirmPurchase
    }

    override fun onPurchaseConfirmed(result: PaymentResult<Purchase>) {
        events += when (result) {
            is PaymentResult.Success -> Event.ConfirmPurchaseSuccess
            is PaymentResult.Failure -> Event.ConfirmPurchaseFailure
        }
    }

    override fun onMessage(message: String) {
        messages += message
    }

    fun assertEvents(vararg events: Event) {
        assertEquals(events.toList(), this.events)
    }

    fun assertMessages(vararg messages: String) {
        assertEquals(messages.toList(), this.messages)
    }

    enum class Event {
        LoadSubscriptionPlans,
        LoadSubscriptionPlansSuccess,
        LoadSubscriptionPlansFailure,
        LoadAcknowledgedSubscriptions,
        LoadAcknowledgedSubscriptionsSuccess,
        LoadAcknowledgedSubscriptionsFailure,
        PurchaseSubscriptionPlan,
        PurchaseSubscriptionPlanSuccess,
        PurchaseSubscriptionPlanCancelled,
        PurchaseSubscriptionPlanFailure,
        ConfirmPurchase,
        ConfirmPurchaseSuccess,
        ConfirmPurchaseFailure,
    }
}
