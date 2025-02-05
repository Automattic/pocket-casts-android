package au.com.shiftyjelly.pocketcasts.repositories.winback

import android.app.Activity
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchasesState
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

interface WinbackManager {
    suspend fun loadProducts(): ProductDetailsState

    suspend fun loadPurchases(): PurchasesState

    suspend fun changeProduct(
        currentPurchase: Purchase,
        currentPurchaseProductId: String,
        newProduct: ProductDetails,
        newProductOfferToken: String,
        activity: Activity,
    ): PurchaseEvent
}
