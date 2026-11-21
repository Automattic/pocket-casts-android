package au.com.shiftyjelly.pocketcasts.payment.mock

import android.app.Activity
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.FakePaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.InAppMessageResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.Product
import au.com.shiftyjelly.pocketcasts.payment.Purchase
import au.com.shiftyjelly.pocketcasts.payment.PurchaseState
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MockPaymentDataSource(
    private val config: () -> MockBillingConfig,
) : PaymentDataSource {
    private val _purchaseResults = MutableSharedFlow<PaymentResult<List<Purchase>>>()

    override val purchaseResults = _purchaseResults.asSharedFlow()

    override suspend fun loadProducts(): PaymentResult<List<Product>> {
        val currentConfig = config()
        if (currentConfig.failProductsLoading) {
            return PaymentResult.Failure(PaymentResultCode.ServiceUnavailable, "Mock products failed to load")
        }
        val eligibleOfferIds = currentConfig.eligibleOffers.flatMap(::allOfferIds).toSet()
        val products = FakePaymentDataSource.DefaultLoadedProducts.map { product ->
            val eligibleOfferPlans = product.pricingPlans.offerPlans.filter { it.offerId in eligibleOfferIds }
            product.copy(pricingPlans = product.pricingPlans.copy(offerPlans = eligibleOfferPlans))
        }
        return PaymentResult.Success(products)
    }

    // Matches the old debug fake, so flows that check for an existing Google Play subscription keep working
    override suspend fun loadPurchases(): PaymentResult<List<Purchase>> {
        return PaymentResult.Success(listOf(FakePaymentDataSource.DefaultLoadedPurchase))
    }

    override suspend fun launchBillingFlow(key: SubscriptionPlan.Key, activity: Activity): PaymentResult<Unit> {
        val currentConfig = config()
        val productId = key.productId
        if (currentConfig.failProductsLoading) {
            return PaymentResult.Failure(PaymentResultCode.ServiceUnavailable, "Mock products failed to load")
        }
        if (productId == null || (key.offer != null && (key.offerId == null || key.offer !in currentConfig.eligibleOffers))) {
            return PaymentResult.Failure(PaymentResultCode.DeveloperError, "No mock plan available for $key")
        }

        val purchaseResult = when (currentConfig.purchaseOutcome) {
            MockPurchaseOutcome.Success -> PaymentResult.Success(listOf(mockPurchase(productId)))
            MockPurchaseOutcome.Cancelled -> PaymentResult.Failure(PaymentResultCode.UserCancelled, "Mock purchase cancelled")
            MockPurchaseOutcome.Error -> PaymentResult.Failure(PaymentResultCode.Error, "Mock purchase failed")
        }
        _purchaseResults.emit(purchaseResult)
        return PaymentResult.Success(Unit)
    }

    override suspend fun acknowledgePurchase(purchase: Purchase): PaymentResult<Purchase> {
        return PaymentResult.Success(purchase.copy(isAcknowledged = true))
    }

    override suspend fun showInAppMessages(activity: Activity): PaymentResult<InAppMessageResult> {
        return PaymentResult.Success(InAppMessageResult.NoActionNeeded)
    }

    private fun mockPurchase(productId: String) = Purchase(
        state = PurchaseState.Purchased(orderId = "mock-order-${UUID.randomUUID()}"),
        token = MOCK_PURCHASE_TOKEN,
        productIds = listOf(productId),
        isAcknowledged = false,
        isAutoRenewing = true,
    )

    private fun allOfferIds(offer: SubscriptionOffer) = SubscriptionTier.entries.flatMap { tier ->
        BillingCycle.entries.flatMap { billingCycle ->
            listOf(false, true).mapNotNull { isInstallment -> offer.offerId(tier, billingCycle, isInstallment) }
        }
    }

    companion object {
        const val MOCK_PURCHASE_TOKEN = "mock-purchase-token"
    }
}
