package au.com.shiftyjelly.pocketcasts.repositories.subscription

import android.app.Activity
import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.utils.Optional
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import io.reactivex.Flowable
import io.reactivex.Single

interface SubscriptionManager {

    fun signOut()
    fun observeSubscriptionChangeEvents(): Flowable<SubscriptionChangedEvent>
    fun observeProductDetails(): Flowable<ProductDetailsState>
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
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String)
    fun getCachedStatus(): SubscriptionStatus?
    fun clearCachedStatus()
    fun isFreeTrialEligible(tier: Subscription.SubscriptionTier): Boolean
    fun updateFreeTrialEligible(tier: Subscription.SubscriptionTier, eligible: Boolean)
    fun getDefaultSubscription(
        subscriptions: List<Subscription>,
        tier: Subscription.SubscriptionTier? = null,
        frequency: SubscriptionFrequency? = null,
    ): Subscription?
    fun trialExists(
        tier: Subscription.SubscriptionTier,
        subscriptions: List<Subscription>
    ): Boolean
}
