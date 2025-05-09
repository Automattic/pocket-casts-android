package au.com.shiftyjelly.pocketcasts.repositories.subscription

import androidx.appcompat.app.AppCompatActivity
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.PurchaseResult
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

interface SubscriptionManager {
    suspend fun initializeBillingConnection(): Nothing

    suspend fun loadPurchaseHistory(): PurchaseHistoryState

    suspend fun refresh()

    fun launchBillingFlow(activity: AppCompatActivity, productDetails: ProductDetails, offerToken: String)

    fun observeProductDetails(): Flowable<ProductDetailsState>
    fun observePurchaseEvents(): Flowable<PurchaseResult>
    fun observeSubscriptionStatus(): Flowable<Optional<SubscriptionStatus>>
    fun subscriptionTier(): Flow<SubscriptionTier>
    fun getSubscriptionStatusRxSingle(allowCache: Boolean = true): Single<SubscriptionStatus>
    suspend fun getSubscriptionStatus(allowCache: Boolean = true): SubscriptionStatus

    fun getCachedStatus(): SubscriptionStatus?
    fun clearCachedStatus()
    fun isOfferEligible(tier: SubscriptionTier): Boolean
    fun getDefaultSubscription(
        subscriptions: List<Subscription>,
        tier: SubscriptionTier? = null,
        frequency: SubscriptionFrequency? = null,
    ): Subscription?
    fun freeTrialForSubscriptionTierFlow(subscriptionTier: SubscriptionTier): Flow<FreeTrial>
}

internal fun logSubscriptionWarning(message: String) {
    Timber.tag(LogBuffer.TAG_SUBSCRIPTIONS).w(message)
    LogBuffer.w(LogBuffer.TAG_SUBSCRIPTIONS, message)
}
