package au.com.shiftyjelly.pocketcasts.repositories.subscription

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PATRON_MONTHLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PATRON_YEARLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_MONTHLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_YEARLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionPurchaseRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.SubscriptionStatusResponse
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.rxSingle

private const val SUBSCRIPTION_REPLACEMENT_MODE_NOT_SET = -1

@Singleton
class SubscriptionManagerImpl @Inject constructor(
    private val billingClient: BillingClientWrapper,
    private val subscriptionMapper: SubscriptionMapper,
    private val syncManager: SyncManager,
    private val settings: Settings,
) : SubscriptionManager {

    private var cachedSubscriptionStatus: SubscriptionStatus?
        get() = settings.cachedSubscriptionStatus.value
        set(value) = settings.cachedSubscriptionStatus.set(value, updateModifiedAt = false)

    private var subscriptionStatus = BehaviorRelay.create<Optional<SubscriptionStatus>>().apply {
        val cachedStatus = cachedSubscriptionStatus
        if (cachedStatus != null) {
            accept(Optional.of(cachedStatus))
        } else {
            accept(Optional.of(null))
        }
    }

    private val productDetails = BehaviorRelay.create<ProductDetailsState>()
    private val purchaseEvents = PublishRelay.create<PurchaseEvent>()
    private val subscriptionChangedEvents = PublishRelay.create<SubscriptionChangedEvent>()

    private var hasOfferEligible = ConcurrentHashMap<SubscriptionTier, Boolean>()

    override fun signOut() {
        clearCachedStatus()
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

    override fun subscriptionTier(): Flow<SubscriptionTier> {
        return observeSubscriptionStatus().asFlow().map { status ->
            (status.get() as? SubscriptionStatus.Paid)?.tier ?: SubscriptionTier.NONE
        }.distinctUntilChanged()
    }

    override fun getSubscriptionStatusRxSingle(allowCache: Boolean): Single<SubscriptionStatus> {
        return rxSingle {
            getSubscriptionStatus(allowCache)
        }
    }

    override suspend fun getSubscriptionStatus(allowCache: Boolean): SubscriptionStatus {
        val cache = cachedSubscriptionStatus
        if (cache != null && allowCache) {
            return cache
        }

        val status = syncManager.subscriptionStatus().toStatus()

        subscriptionStatus.accept(Optional.of(status))
        val oldStatus = cachedSubscriptionStatus
        if (oldStatus != status) {
            if (status is SubscriptionStatus.Paid && oldStatus is SubscriptionStatus.Free) {
                subscriptionChangedEvents.accept(SubscriptionChangedEvent.AccountUpgradedToPlus)
            } else if (status is SubscriptionStatus.Free && oldStatus is SubscriptionStatus.Paid) {
                subscriptionChangedEvents.accept(SubscriptionChangedEvent.AccountDowngradedToFree)
            }
        }
        cachedSubscriptionStatus = status

        if (!status.isPocketCastsChampion && status is SubscriptionStatus.Paid && status.platform == SubscriptionPlatform.GIFT) { // This account is a trial account
            settings.setTrialFinishedSeen(false) // Make sure on expiry we show the trial finished dialog
        }
        return status
    }

    override suspend fun initializeBillingConnection() = coroutineScope {
        launch { listenToPurchaseUpdates() }
        launch { loadProducts() }
        awaitCancellation()
    }

    private suspend fun listenToPurchaseUpdates(): Nothing {
        billingClient.purchaseUpdates.collect { (billingResult, purchases) ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases.forEach { purchase ->
                        handlePurchase(purchase)
                    }
                }

                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    purchaseEvents.accept(PurchaseEvent.Cancelled(billingResult.responseCode))
                }

                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    val (result, freshPurchases) = billingClient.loadPurchases(purchasesParams)
                    if (result.isOk()) {
                        val existingPurchase = freshPurchases.firstOrNull()
                        if (existingPurchase != null) {
                            try {
                                sendPurchaseToServer(existingPurchase)
                            } catch (e: Throwable) {
                                logSubscriptionError(e, "Failed to send purchase info")
                                val failureEvent = PurchaseEvent.Failure(e.message ?: "Unknown errror", billingResult.responseCode)
                                purchaseEvents.accept(failureEvent)
                            }
                        } else {
                            logSubscriptionWarning("Failed to load already owned purchase: ${billingResult.debugMessage}")
                            val failureEvent = PurchaseEvent.Failure(result.debugMessage, billingResult.responseCode)
                            purchaseEvents.accept(failureEvent)
                        }
                    }
                }

                else -> {
                    logSubscriptionWarning("Could not purchase subscription: ${billingResult.debugMessage}")
                    val failureEvent = PurchaseEvent.Failure(billingResult.debugMessage, billingResult.responseCode)
                    purchaseEvents.accept(failureEvent)
                }
            }
        }
    }

    private suspend fun loadProducts() {
        productDetails.accept(ProductDetailsState.Loading)
        val (result, products) = billingClient.loadProducts(productDetailsParams)
        if (result.isOk()) {
            productDetails.accept(ProductDetailsState.Loaded(products))
            refreshPurchases()
        } else {
            productDetails.accept(ProductDetailsState.Failure)
        }
    }

    override suspend fun refreshPurchases() = coroutineScope {
        val (historyResults, historyRecords) = billingClient.loadPurchaseHistory(purchaseHistoryParams)
        if (historyResults.isOk()) {
            historyRecords.forEach(::handleHistoryRecord)
        }

        val (purchasesResult, purchases) = billingClient.loadPurchases(purchasesParams)
        if (purchasesResult.isOk()) {
            purchases.forEach { purchase ->
                if (!purchase.isAcknowledged) {
                    launch { handlePurchase(purchase) }
                }
            }
        }
    }

    private fun handleHistoryRecord(record: PurchaseHistoryRecord) {
        record.products
            .map(SubscriptionTier::fromProductId)
            .distinct()
            .forEach { tier -> updateOfferEligible(tier, false) }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            try {
                sendPurchaseToServer(purchase)
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
                purchase.products
                    .map(SubscriptionTier::fromProductId)
                    .distinct()
                    .forEach { tier -> updateOfferEligible(tier, false) }
            } catch (e: Throwable) {
                logSubscriptionError(e, "Could not send purchase info: ${purchase.orderId}")
            }
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        billingClient.acknowledgePurchase(acknowledgePurchaseParams(purchase))
    }

    private suspend fun sendPurchaseToServer(purchase: Purchase) {
        if (purchase.products.size != 1) {
            LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "expected 1 product when sending purchase to server, but there were ${purchase.products.size}")
        }

        val response = syncManager.subscriptionPurchaseRxSingle(SubscriptionPurchaseRequest(purchase.purchaseToken, purchase.products.first())).await()
        val newStatus = response.toStatus()
        cachedSubscriptionStatus = newStatus
        subscriptionStatus.accept(Optional.of(newStatus))
        purchaseEvents.accept(PurchaseEvent.Success)
    }

    override fun launchBillingFlow(
        activity: AppCompatActivity,
        productDetails: ProductDetails,
        offerToken: String,
    ) {
        activity.lifecycleScope.launch {
            val (result, subscriptionUpdateParams) = loadSubscriptionUpdateParamsMode(productDetails)
            if (!result.isOk()) {
                logSubscriptionWarning("Unable to upgrade subscription plan: ${result.debugMessage}")
                return@launch
            }
            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .let { builder ->
                    if (subscriptionUpdateParams != null) {
                        builder.setSubscriptionUpdateParams(subscriptionUpdateParams)
                    } else {
                        builder
                    }
                }
                .build()
            billingClient.launchBillingFlow(activity, billingFlowParams)
        }
    }

    private suspend fun loadSubscriptionUpdateParamsMode(
        productDetails: ProductDetails,
    ): Pair<BillingResult, BillingFlowParams.SubscriptionUpdateParams?> {
        val replacementMode = settings.cachedSubscriptionStatus.value
            ?.let { subscriptionStatus -> getSubscriptionReplacementMode(subscriptionStatus, productDetails) }
            ?.takeIf { it != SUBSCRIPTION_REPLACEMENT_MODE_NOT_SET }

        if (replacementMode == null) {
            return okResult to null
        }

        val (result, purchases) = billingClient.loadPurchases(purchasesParams)
        return if (result.isOk()) {
            val params = purchases.firstOrNull()?.let { purchase ->
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(purchase.purchaseToken)
                    .setSubscriptionReplacementMode(replacementMode)
                    .build()
            }
            okResult to params
        } else {
            result to null
        }
    }

    override fun getCachedStatus(): SubscriptionStatus? {
        return subscriptionStatus.value?.get()
    }

    override fun clearCachedStatus() {
        cachedSubscriptionStatus = null
        subscriptionStatus.accept(Optional.empty())
    }
    override fun isOfferEligible(tier: SubscriptionTier): Boolean = hasOfferEligible[tier] ?: true

    override fun updateOfferEligible(tier: SubscriptionTier, eligible: Boolean) {
        hasOfferEligible[tier] = eligible
    }
    override fun getDefaultSubscription(
        subscriptions: List<Subscription>,
        tier: SubscriptionTier?,
        frequency: SubscriptionFrequency?,
    ): Subscription? {
        val subscriptionTier = tier ?: SubscriptionTier.PLUS
        val subscriptionFrequency = frequency ?: SubscriptionFrequency.YEARLY

        val tierSubscriptions = subscriptions.filter { it.tier == subscriptionTier }
        val withOffers = tierSubscriptions.filterIsInstance<Subscription.WithOffer>()

        return withOffers.find {
            it.recurringPricingPhase is SubscriptionPricingPhase.Months
        } ?: tierSubscriptions.firstOrNull {
            when (subscriptionFrequency) {
                SubscriptionFrequency.MONTHLY -> it.recurringPricingPhase is SubscriptionPricingPhase.Months
                SubscriptionFrequency.YEARLY -> it.recurringPricingPhase is SubscriptionPricingPhase.Years
                SubscriptionFrequency.NONE -> throw IllegalStateException("Unknown subscription frequency found")
            }
        } ?: tierSubscriptions.firstOrNull() // If no matching subscription is found, select first available one
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun freeTrialForSubscriptionTierFlow(subscriptionTier: SubscriptionTier) = this
        .observeProductDetails()
        .asFlow()
        .transformLatest { productDetails ->
            val subscriptions = when (productDetails) {
                is ProductDetailsState.Loading, ProductDetailsState.Failure -> null
                is ProductDetailsState.Loaded -> productDetails.productDetails.mapNotNull { productDetailsState ->
                    subscriptionMapper.mapFromProductDetails(
                        productDetails = productDetailsState,
                        isOfferEligible = isOfferEligible(
                            SubscriptionTier.fromProductId(productDetailsState.productId),
                        ),
                    )
                }
            } ?: emptyList()

            val filteredOffer = Subscription.filterOffers(subscriptions)
            val updatedSubscriptions = filteredOffer.filter { it.tier == subscriptionTier }
            val defaultSubscription = getDefaultSubscription(
                subscriptions = updatedSubscriptions,
                tier = subscriptionTier,
            )
            emit(
                FreeTrial(
                    subscriptionTier = subscriptionTier,
                    exists = defaultSubscription?.offerPricingPhase != null,
                ),
            )
        }

    private companion object {
        val okResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build()

        val purchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val purchaseHistoryParams = QueryPurchaseHistoryParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val productDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PLUS_MONTHLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PLUS_YEARLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PATRON_MONTHLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PATRON_YEARLY_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                ),
            )
            .build()

        fun acknowledgePurchaseParams(purchase: Purchase) = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
    }
}

private fun getSubscriptionReplacementMode(
    subscribedPlanStatus: SubscriptionStatus,
    newPlanDetails: ProductDetails,
) = when {
    /* Since upgrading to a more expensive tier, recommended replacement mode is CHARGE_PRORATED_PRICE (https://rb.gy/acghw) */
    subscribedPlanStatus is SubscriptionStatus.Paid &&
        subscribedPlanStatus.tier == SubscriptionTier.PLUS &&
        subscribedPlanStatus.platform == SubscriptionPlatform.ANDROID &&
        newPlanDetails.productId in listOf(PATRON_MONTHLY_PRODUCT_ID, PATRON_YEARLY_PRODUCT_ID)
    -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE

    /* Since upgrading from shorter to longer billing period within same Plus tier, recommended replacement mode is CHARGE_FULL_PRICE (https://rb.gy/8h4adz) */
    subscribedPlanStatus is SubscriptionStatus.Paid &&
        subscribedPlanStatus.tier == SubscriptionTier.PLUS &&
        subscribedPlanStatus.platform == SubscriptionPlatform.ANDROID &&
        subscribedPlanStatus.frequency == SubscriptionFrequency.MONTHLY &&
        newPlanDetails.productId == PLUS_YEARLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE

    /* Since upgrading from shorter to longer billing period within same Patron tier, recommended replacement mode is CHARGE_FULL_PRICE (https://rb.gy/8h4adz) */
    subscribedPlanStatus is SubscriptionStatus.Paid &&
        subscribedPlanStatus.tier == SubscriptionTier.PATRON &&
        subscribedPlanStatus.platform == SubscriptionPlatform.ANDROID &&
        subscribedPlanStatus.frequency == SubscriptionFrequency.MONTHLY &&
        newPlanDetails.productId == PATRON_YEARLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE

    /* Since downgrading to less expensive plan, recommended replacement mode is DEFERRED (https://rb.gy/8y0wx0) */
    subscribedPlanStatus is SubscriptionStatus.Paid &&
        subscribedPlanStatus.platform == SubscriptionPlatform.ANDROID &&
        subscribedPlanStatus.frequency == SubscriptionFrequency.YEARLY &&
        newPlanDetails.productId in listOf(PLUS_MONTHLY_PRODUCT_ID, PATRON_MONTHLY_PRODUCT_ID) -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED

    else -> SUBSCRIPTION_REPLACEMENT_MODE_NOT_SET
}

sealed interface ProductDetailsState {
    data class Loaded(val productDetails: List<ProductDetails>) : ProductDetailsState

    data object Loading : ProductDetailsState

    data object Failure : ProductDetailsState
}

sealed class PurchaseEvent {
    object Success : PurchaseEvent()
    data class Cancelled(@BillingClient.BillingResponseCode val responseCode: Int) : PurchaseEvent()
    data class Failure(
        val errorMessage: String,
        @BillingClient.BillingResponseCode val responseCode: Int?,
    ) : PurchaseEvent()
}

sealed class SubscriptionChangedEvent {
    object AccountUpgradedToPlus : SubscriptionChangedEvent()
    object AccountDowngradedToFree : SubscriptionChangedEvent()
}

data class FreeTrial(
    val subscriptionTier: SubscriptionTier,
    val exists: Boolean = false,
)

private fun SubscriptionStatusResponse.toStatus(): SubscriptionStatus {
    val originalPlatform = SubscriptionPlatform.entries.getOrNull(platform) ?: SubscriptionPlatform.NONE

    return if (paid == 0) {
        SubscriptionStatus.Free(expiryDate, giftDays, originalPlatform)
    } else {
        val subs = subscriptions?.map { it.toSubscription() } ?: emptyList()
        subs.getOrNull(index)?.isPrimarySubscription = true // Mark the subscription that the server says is the main one
        val freq = SubscriptionFrequency.entries.getOrNull(frequency) ?: SubscriptionFrequency.NONE
        val enumTier = SubscriptionTier.fromString(tier)
        SubscriptionStatus.Paid(expiryDate ?: Date(), autoRenewing, giftDays, freq, originalPlatform, subs, enumTier, index)
    }
}

private fun SubscriptionResponse.toSubscription(): SubscriptionStatus.Subscription {
    val enumTier = SubscriptionTier.fromString(tier)
    val freq = SubscriptionFrequency.entries.getOrNull(frequency) ?: SubscriptionFrequency.NONE
    return SubscriptionStatus.Subscription(enumTier, freq, expiryDate, autoRenewing, updateUrl)
}
