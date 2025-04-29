package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.Purchase
import au.com.shiftyjelly.pocketcasts.payment.PurchaseApprover
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionPurchaseRequest
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.rx2.await

class ServerPurchaseApprover @Inject constructor(
    private val syncManager: SyncManager,
) : PurchaseApprover {
    override suspend fun approve(purchase: Purchase): PaymentResult<Purchase> {
        return try {
            val request = SubscriptionPurchaseRequest(purchase.token, purchase.productIds.first())
            syncManager.subscriptionPurchaseRxSingle(request).await()
            PaymentResult.Success(purchase)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            PaymentResult.Failure(PaymentResultCode.Unknown(0), e.message ?: "Server confirmation error")
        }
    }
}
