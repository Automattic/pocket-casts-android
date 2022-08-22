package au.com.shiftyjelly.pocketcasts.repositories.subscription

import android.app.Activity
import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.BuildConfig
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager.Companion.MONTHLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager.Companion.PLUS_PRODUCT_BASE
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager.Companion.YEARLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionPurchaseRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionStatusResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.utils.AnalyticsHelper
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
class SubscriptionManagerImpl @Inject constructor(private val syncServerManager: SyncServerManager, private val settings: Settings) :
    SubscriptionManager,
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
            accept(Optional.of(SubscriptionStatus.Free()))
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

        return syncServerManager.subscriptionStatus()
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
                    .setProductId(MONTHLY_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(YEARLY_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            )

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
            purchaseEvents.accept(PurchaseEvent.Cancelled)
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
                            val failureEvent = PurchaseEvent.Failure(e.message ?: "Unknown error")
                            purchaseEvents.accept(failureEvent)
                        }
                    } else {
                        LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "Subscription purchase returned already owned but we couldn't load it")
                        val failureEvent = PurchaseEvent.Failure(purchasesResult.billingResult.debugMessage)
                        purchaseEvents.accept(failureEvent)
                    }
                }
            } else {
                LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "Could not purchase subscription: ${billingResult.debugMessage}")
                val failureEvent = PurchaseEvent.Failure(billingResult.debugMessage)
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
                    AnalyticsHelper.plusPurchased()
                } catch (e: Exception) {
                    LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, e, "Could not send purchase info")

                    purchaseEvents.accept(PurchaseEvent.Failure(e.message ?: "Unknown error"))
                }
            }
        }
    }

    override suspend fun sendPurchaseToServer(purchase: Purchase) {
        if (purchase.products.size != 1) {
            LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "expected 1 product when sending purchase to server, but there were ${purchase.products.size}")
        }

        val response = syncServerManager.subscriptionPurchase(SubscriptionPurchaseRequest(purchase.purchaseToken, purchase.products.first())).await()
        val newStatus = response.toStatus()
        cachedSubscriptionStatus = newStatus
        subscriptionStatus.accept(Optional.of(newStatus))
        purchaseEvents.accept(PurchaseEvent.Success)
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

    override fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult? {
        if (productDetails.subscriptionOfferDetails?.size != 1) {
            val message = "Expected 1 subscription offer when launching billing flow, but there were ${productDetails.subscriptionOfferDetails?.size}"
            LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, message)
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        return offerToken?.let {
            val productDetailsParamsList =
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            val billingFlowParams =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
            billingClient.launchBillingFlow(activity, billingFlowParams)
        }
    }

    override fun getCachedStatus(): SubscriptionStatus? {
        return subscriptionStatus.value?.get()
    }

    override fun clearCachedStatus() {
        cachedSubscriptionStatus = null
        subscriptionStatus.accept(Optional.empty())
    }

    override fun isFreeTrialEligible() = freeTrialEligible && BuildConfig.ENABLE_FREE_TRIAL

    override fun updateFreeTrialEligible(eligible: Boolean) {
        freeTrialEligible = eligible
    }

    override fun getDefaultSubscription(subscriptions: List<Subscription>): Subscription? {
        val trialsIfPresent = subscriptions
            .filterIsInstance<Subscription.WithTrial>()
            .ifEmpty { subscriptions }

        return trialsIfPresent.find {
            it.recurringPricingPhase is SubscriptionPricingPhase.Months
        } ?: trialsIfPresent.firstOrNull() // if no monthly subscriptions, just display the first
    }
}

sealed class ProductDetailsState {
    data class Loaded(val productDetails: List<ProductDetails>) : ProductDetailsState()
    data class Error(val message: String) : ProductDetailsState()
}

sealed class PurchaseEvent {
    object Success : PurchaseEvent()
    object Cancelled : PurchaseEvent()
    data class Failure(val errorMessage: String) : PurchaseEvent()
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
