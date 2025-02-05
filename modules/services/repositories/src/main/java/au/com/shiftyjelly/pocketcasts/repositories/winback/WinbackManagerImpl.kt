package au.com.shiftyjelly.pocketcasts.repositories.winback

import android.app.Activity
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.isOk
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.asFlow

class WinbackManagerImpl @Inject constructor(
    val subscriptionManager: SubscriptionManager,
) : WinbackManager {
    override suspend fun loadProducts() = subscriptionManager.loadProducts()

    override suspend fun loadPurchases() = subscriptionManager.loadPurchases()

    override suspend fun changeProduct(
        currentPurchase: Purchase,
        currentPurchaseProductId: String,
        newProduct: ProductDetails,
        newProductOfferToken: String,
        activity: Activity,
    ): PurchaseEvent {
        val startResult = subscriptionManager.changeProduct(
            currentPurchase = currentPurchase,
            currentPurchaseProductId = currentPurchaseProductId,
            newProduct = newProduct,
            newProductOfferToken = newProductOfferToken,
            activity = activity,
        )
        return if (startResult.isOk()) {
            subscriptionManager.observePurchaseEvents().asFlow().first()
        } else {
            PurchaseEvent.Failure(
                "Failed to start change product flow. ${startResult.debugMessage}",
                startResult.responseCode,
            )
        }
    }
}
