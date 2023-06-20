package au.com.shiftyjelly.pocketcasts.repositories.subscription

import android.app.Activity
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PATRON_MONTHLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PATRON_YEARLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_MONTHLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_PRODUCT_BASE
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_YEARLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionPurchaseRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionStatusResponse
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionManagerImpl @Inject constructor(
    private val syncManager: SyncManager,
    private val settings: Settings,
) : SubscriptionManager,
    PurchasesUpdatedListener,
    AcknowledgePurchaseResponseListener {

    private var cachedSubscriptionStatus: SubscriptionStatus?
        get() = settings.getCachedSubscription()
        set(value) = settings.setCachedSubscription(value)

    private var subscriptionStatus = BehaviorRelay.create<Optional<SubscriptionStatus>>().apply {
        val cachedStatus = cachedSubscriptionStatus
        if (cachedStatus != null) {
            accept(Optional.of(cachedStatus))
        } else {
            accept(Optional.of(null))
        }
    }

    private lateinit var billingClient: BillingClient
    private val productDetails = BehaviorRelay.create<ProductDetailsState>()
    private val purchaseEvents = PublishRelay.create<PurchaseEvent>()
    private val subscriptionChangedEvents = PublishRelay.create<SubscriptionChangedEvent>()
    private var freeTrialEligible: Boolean = true

    override fun signOut() {
        clearCachedStatus()
    }

    override fun observeSubscriptionChangeEvents(): Flowable<SubscriptionChangedEvent> {
        return subscriptionChangedEvents.toFlowable(BackpressureStrategy.LATEST).distinctUntilChanged()
    }

    override fun observeProductDetails(): Flowable<ProductDetailsState> {
        return productDetails.toFlowable(BackpressureStrategy.LATEST)
    }

    override fun observePurchaseEvents(): Flowable<PurchaseEvent> {
        return purchaseEvents.toFlowable(BackpressureStrategy.LATEST)
    }

    override fun observeSubscriptionStatus(): Flowable<Optional<SubscriptionStatus>> {
        return subscriptionStatus.toFlowable(BackpressureStrategy.LATEST)
    }

    override fun getSubscriptionStatus(allowCache: Boolean): Single<SubscriptionStatus> {
        val cache = cachedSubscriptionStatus
        if (cache != null && allowCache) {
            return Single.just(cache)
        }

        return syncManager.subscriptionStatus()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                it.toStatus()
            }
            .doOnSuccess {
                subscriptionStatus.accept(Optional.of(it))
                val oldStatus = cachedSubscriptionStatus
                if (oldStatus != it) {
                    if (it is SubscriptionStatus.Plus && oldStatus is SubscriptionStatus.Free) {
                        subscriptionChangedEvents.accept(SubscriptionChangedEvent.AccountUpgradedToPlus)
                    } else if (it is SubscriptionStatus.Free && oldStatus is SubscriptionStatus.Plus) {
                        subscriptionChangedEvents.accept(SubscriptionChangedEvent.AccountDowngradedToFree)
                    }
                }
                cachedSubscriptionStatus = it

                if (!it.isLifetimePlus && it is SubscriptionStatus.Plus && it.platform == SubscriptionPlatform.GIFT) { // This account is a trial account
                    settings.setTrialFinishedSeen(false) // Make sure on expiry we show the trial finished dialog
                }
            }
    }

    override fun connectToGooglePlay(context: Context) {
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("Connected to google play")
                    loadProducts()
                } else {
                    Timber.e("Couldn't set up billing connection: ${billingResult.debugMessage}")
                    loadProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Timber.d("Disconnected from Google Play")
            }
        })
    }

    override fun loadProducts() {
        val productList =
            mutableListOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PLUS_MONTHLY_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PLUS_YEARLY_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            ).apply {
                if (FeatureFlag.isEnabled(Feature.ADD_PATRON_ENABLED)) {
                    add(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PATRON_MONTHLY_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                    )
                    add(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PATRON_YEARLY_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                    )
                }
            }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Timber.d("Billing products loaded")
                productDetails.accept(ProductDetailsState.Loaded(productDetailsList))

                refreshPurchases()
            } else {
                productDetails.accept(ProductDetailsState.Error(billingResult.debugMessage))
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            purchaseEvents.accept(PurchaseEvent.Cancelled(billingResult.responseCode))
        } else {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                GlobalScope.launch {
                    val purchasesResult = getPurchases()
                    if (purchasesResult == null) {
                        LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "unable to update purchase because billing result returned null purchases")
                        return@launch
                    }

                    if (purchasesResult.purchasesList.isNotEmpty()) {
                        val existingPurchase = purchasesResult.purchasesList.first()

                        try {
                            sendPurchaseToServer(existingPurchase)
                        } catch (e: Exception) {
                            LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, e, "Could not send purchase info")
                            val failureEvent = PurchaseEvent.Failure(
                                e.message ?: "Unknown error",
                                billingResult.responseCode
                            )
                            purchaseEvents.accept(failureEvent)
                        }
                    } else {
                        LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "Subscription purchase returned already owned but we couldn't load it")
                        val failureEvent = PurchaseEvent.Failure(
                            purchasesResult.billingResult.debugMessage,
                            billingResult.responseCode
                        )
                        purchaseEvents.accept(failureEvent)
                    }
                }
            } else {
                LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "Could not purchase subscription: ${billingResult.debugMessage}")
                val failureEvent = PurchaseEvent.Failure(billingResult.debugMessage, billingResult.responseCode)
                purchaseEvents.accept(failureEvent)
            }
        }
    }

    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
        Timber.d("Purchase ack")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            LogBuffer.i(LogBuffer.TAG_SUBSCRIPTIONS, "Purchase ack OK")
        } else {
            LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "Purchase ack FAILED. code= ${billingResult.responseCode} message=${billingResult.debugMessage}")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            Timber.d("Purchase: ${purchase.purchaseToken}")
            // Grant entitlement to the user.
            GlobalScope.launch {
                try {
                    sendPurchaseToServer(purchase)
                    // Acknowledge the purchase if it hasn't already been acknowledged.
                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams, this@SubscriptionManagerImpl)
                    }
                    updateFreeTrialEligible(false)
                    FirebaseAnalyticsTracker.plusPurchased()
                } catch (e: Exception) {
                    LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, e, "Could not send purchase info")
                    purchaseEvents.accept(PurchaseEvent.Failure(e.message ?: "Unknown error", null))
                }
            }
        }
    }

    override suspend fun sendPurchaseToServer(purchase: Purchase) {
        if (purchase.products.size != 1) {
            LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "expected 1 product when sending purchase to server, but there were ${purchase.products.size}")
        }

        try {
            val response = syncManager.subscriptionPurchase(SubscriptionPurchaseRequest(purchase.purchaseToken, purchase.products.first())).await()
            val newStatus = response.toStatus()
            cachedSubscriptionStatus = newStatus
            subscriptionStatus.accept(Optional.of(newStatus))
            purchaseEvents.accept(PurchaseEvent.Success)
        } catch (ex: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, ex, "Failed to send purchase to server.")
        }
    }

    override fun refreshPurchases() {
        if (!billingClient.isReady) return

        updateFreeTrialEligibilityIfPurchaseHistoryExists()

        val queryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(queryPurchasesParams) { _, purchases ->
            purchases.forEach {
                if (!it.isAcknowledged) { // Purchase was purchased in the play store, or in the background somehow
                    handlePurchase(it)
                }
            }
        }
    }

    private fun updateFreeTrialEligibilityIfPurchaseHistoryExists() {
        val queryPurchaseHistoryParams = QueryPurchaseHistoryParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchaseHistoryAsync(queryPurchaseHistoryParams) { _, purchases ->
            // TODO: Patron - Update free trial eligibility for Patron
            if (purchases?.any { it.products.toString().contains(PLUS_PRODUCT_BASE) } == true) {
                updateFreeTrialEligible(false)
            }
        }
    }

    override suspend fun getPurchases(): PurchasesResult? {
        if (!billingClient.isReady) return null

        val queryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        return billingClient.queryPurchasesAsync(params = queryPurchasesParams)
    }

    override fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String): BillingResult {
        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        val productDetailsParamsList = listOf(productDetailsParams)
        val billingFlowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun getCachedStatus(): SubscriptionStatus? {
        return subscriptionStatus.value?.get()
    }

    override fun clearCachedStatus() {
        cachedSubscriptionStatus = null
        subscriptionStatus.accept(Optional.empty())
    }

    override fun isFreeTrialEligible() = freeTrialEligible

    override fun updateFreeTrialEligible(eligible: Boolean) {
        freeTrialEligible = eligible
    }

    override fun getDefaultSubscription(
        subscriptions: List<Subscription>,
        tier: Subscription.SubscriptionTier?,
        frequency: SubscriptionFrequency?,
    ): Subscription? {
        val subscriptionTier = tier ?: Subscription.SubscriptionTier.PLUS
        val subscriptionFrequency = frequency ?: SubscriptionFrequency.YEARLY

        val tierSubscriptions = subscriptions.filter { it.tier == subscriptionTier }
        val trialsIfPresent = tierSubscriptions
            .filterIsInstance<Subscription.WithTrial>()

        return trialsIfPresent.find {
            it.recurringPricingPhase is SubscriptionPricingPhase.Months // trial is available for monthly only
        } ?: tierSubscriptions.firstOrNull {
            when (subscriptionFrequency) {
                SubscriptionFrequency.MONTHLY -> it.recurringPricingPhase is SubscriptionPricingPhase.Months
                SubscriptionFrequency.YEARLY -> it.recurringPricingPhase is SubscriptionPricingPhase.Years
                SubscriptionFrequency.NONE -> throw IllegalStateException("Unknown subscription frequency found")
            }
        } ?: tierSubscriptions.firstOrNull() // If no matching subscription is found, select first available one
    }
}

sealed class ProductDetailsState {
    data class Loaded(val productDetails: List<ProductDetails>) : ProductDetailsState()
    data class Error(val message: String) : ProductDetailsState()
}

sealed class PurchaseEvent {
    object Success : PurchaseEvent()
    data class Cancelled(@BillingClient.BillingResponseCode val responseCode: Int) : PurchaseEvent()
    data class Failure(
        val errorMessage: String,
        @BillingClient.BillingResponseCode val responseCode: Int?
    ) : PurchaseEvent()
}

sealed class SubscriptionChangedEvent {
    object AccountUpgradedToPlus : SubscriptionChangedEvent()
    object AccountDowngradedToFree : SubscriptionChangedEvent()
}

private fun SubscriptionStatusResponse.toStatus(): SubscriptionStatus {
    val originalPlatform = SubscriptionPlatform.values().getOrNull(platform) ?: SubscriptionPlatform.NONE

    val subs = subscriptions?.map { it.toSubscription() } ?: emptyList()
    subs.getOrNull(index)?.isPrimarySubscription = true // Mark the subscription that the server says is the main one
    return if (paid == 0) {
        SubscriptionStatus.Free(expiryDate, giftDays, originalPlatform, subs)
    } else {
        val freq = SubscriptionFrequency.values().getOrNull(frequency) ?: SubscriptionFrequency.NONE
        val enumType = SubscriptionType.values().getOrNull(type) ?: SubscriptionType.NONE
        SubscriptionStatus.Plus(expiryDate ?: Date(), autoRenewing, giftDays, freq, originalPlatform, subs, enumType, index)
    }
}

private fun SubscriptionResponse.toSubscription(): SubscriptionStatus.Subscription {
    val enumType = SubscriptionType.values().getOrNull(type) ?: SubscriptionType.NONE
    val freq = SubscriptionFrequency.values().getOrNull(frequency) ?: SubscriptionFrequency.NONE
    return SubscriptionStatus.Subscription(enumType, freq, expiryDate, autoRenewing, updateUrl)
}
