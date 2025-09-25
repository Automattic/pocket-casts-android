package au.com.shiftyjelly.pocketcasts.repositories.subscription

import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.Purchase
import au.com.shiftyjelly.pocketcasts.payment.PurchaseApprover
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionPurchaseRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.toMembership
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class ServerPurchaseApprover @Inject constructor(
    private val syncManager: SyncManager,
    private val settings: Settings,
) : PurchaseApprover {
    override suspend fun approve(purchase: Purchase): PaymentResult<Purchase> {
        return runCatching {
            val request = SubscriptionPurchaseRequest(purchase.token, purchase.productIds.first())
            val membership = syncManager.subscriptionPurchase(request).toMembership()
            settings.cachedMembership.set(membership, updateModifiedAt = false)
            PaymentResult.Success(purchase)
        }.getOrElse { error ->
            if (error is CancellationException) {
                throw error
            } else {
                LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, error, "Failed to approve a purchase")
                PaymentResult.Failure(PaymentResultCode.ItemNotApproved, error.message ?: "Server confirmation error")
            }
        }
    }
}
