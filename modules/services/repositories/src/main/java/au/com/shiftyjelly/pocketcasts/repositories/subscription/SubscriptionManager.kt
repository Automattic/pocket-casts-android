package au.com.shiftyjelly.pocketcasts.repositories.subscription

import androidx.appcompat.app.AppCompatActivity
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

interface SubscriptionManager {
    suspend fun initializeBillingConnection(): Nothing
    suspend fun refreshPurchases()
    fun launchBillingFlow(activity: AppCompatActivity, productDetails: ProductDetails, offerToken: String)

    fun signOut()

    fun observeProductDetails(): Flowable<ProductDetailsState>
    fun observePurchaseEvents(): Flowable<PurchaseEvent>
    fun observeSubscriptionStatus(): Flowable<Optional<SubscriptionStatus>>
    fun subscriptionTier(): Flow<SubscriptionTier>
    fun getSubscriptionStatusRxSingle(allowCache: Boolean = true): Single<SubscriptionStatus>
    suspend fun getSubscriptionStatus(allowCache: Boolean = true): SubscriptionStatus

    fun getCachedStatus(): SubscriptionStatus?
    fun clearCachedStatus()
    fun isOfferEligible(tier: SubscriptionTier): Boolean
    fun updateOfferEligible(tier: SubscriptionTier, eligible: Boolean)
    fun getDefaultSubscription(
        subscriptions: List<Subscription>,
        tier: SubscriptionTier? = null,
        frequency: SubscriptionFrequency? = null,
    ): Subscription?
    fun freeTrialForSubscriptionTierFlow(subscriptionTier: SubscriptionTier): Flow<FreeTrial>
}

internal fun logSubscriptionInfo(message: String) {
    Timber.tag(LogBuffer.TAG_SUBSCRIPTIONS).i(message)
}

internal fun logSubscriptionWarning(message: String) {
    Timber.tag(LogBuffer.TAG_SUBSCRIPTIONS).w(message)
    LogBuffer.w(LogBuffer.TAG_SUBSCRIPTIONS, message)
}

internal fun logSubscriptionError(e: Throwable, message: String) {
    Timber.tag(LogBuffer.TAG_SUBSCRIPTIONS).e(e, message)
    LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, e, message)
}
