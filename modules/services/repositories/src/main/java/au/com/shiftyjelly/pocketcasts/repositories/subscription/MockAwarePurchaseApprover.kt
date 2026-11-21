package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.Purchase
import au.com.shiftyjelly.pocketcasts.payment.PurchaseApprover
import au.com.shiftyjelly.pocketcasts.payment.mock.MockBillingSettings
import javax.inject.Inject

// Mock purchases carry a fake token the server would reject, so they are approved locally
class MockAwarePurchaseApprover @Inject constructor(
    private val serverPurchaseApprover: ServerPurchaseApprover,
    private val mockBillingSettings: MockBillingSettings,
) : PurchaseApprover {
    override suspend fun approve(purchase: Purchase): PaymentResult<Purchase> {
        if (mockBillingSettings.isMockEnabled) {
            return PaymentResult.Success(purchase)
        }
        return serverPurchaseApprover.approve(purchase)
    }
}
