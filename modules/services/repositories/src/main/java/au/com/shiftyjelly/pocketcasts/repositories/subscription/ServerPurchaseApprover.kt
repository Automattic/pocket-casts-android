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
        return runCatching {
            val request = SubscriptionPurchaseRequest(purchase.token, purchase.productIds.first())
            syncManager.subscriptionPurchaseRxSingle(request).await()
            PaymentResult.Success(purchase)
        }.getOrElse { error ->
            if (error is CancellationException) {
                throw error
            } else {
                PaymentResult.Failure(PaymentResultCode.Unknown(0), error.message ?: "Server confirmation error")
            }
        }
    }
}
