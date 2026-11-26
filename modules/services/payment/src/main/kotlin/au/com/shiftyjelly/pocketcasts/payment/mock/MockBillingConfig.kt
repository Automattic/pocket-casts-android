package au.com.shiftyjelly.pocketcasts.payment.mock

import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer

data class MockBillingConfig(
    val isEnabled: Boolean = false,
    val eligibleOffers: Set<SubscriptionOffer> = SubscriptionOffer.entries.toSet(),
    val failProductsLoading: Boolean = false,
    val purchaseOutcome: MockPurchaseOutcome = MockPurchaseOutcome.Success,
)

enum class MockPurchaseOutcome {
    Success,
    Cancelled,
    Error,
}
