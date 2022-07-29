package au.com.shiftyjelly.pocketcasts.repositories.subscription

import android.app.Activity
import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.utils.Optional
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import io.reactivex.Flowable
import io.reactivex.Single

interface SubscriptionManager {

    companion object {
        const val MONTHLY_SKU = "com.pocketcasts.plus.monthly"
        const val YEARLY_SKU = "com.pocketcasts.plus.yearly"
        const val TEST_FREE_TRIAL_SKU = "com.pocketcasts.plus.testfreetrialoffer"
    }

    fun signOut()
    fun observeSubscriptionChangeEvents(): Flowable<SubscriptionChangedEvent>
    fun observeProductDetails(): Flowable<ProductDetailsState>
    fun observePrices(): Flowable<PricePair>
    fun observePurchaseEvents(): Flowable<PurchaseEvent>
    fun observeSubscriptionStatus(): Flowable<Optional<SubscriptionStatus>>
    fun getSubscriptionStatus(allowCache: Boolean = true): Single<SubscriptionStatus>
    fun connectToGooglePlay(context: Context)
    fun loadProducts()
    fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?)
    fun onAcknowledgePurchaseResponse(billingResult: BillingResult)
    fun handlePurchase(purchase: Purchase)
    suspend fun sendPurchaseToServer(purchase: Purchase)
    fun refreshPurchases()
    suspend fun getPurchases(): PurchasesResult?
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult?
    fun getCachedStatus(): SubscriptionStatus?
    fun clearCachedStatus()
}
