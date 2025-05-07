package au.com.shiftyjelly.pocketcasts.repositories.subscription

import android.app.Activity
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
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PurchaseResult
import au.com.shiftyjelly.pocketcasts.payment.billing.isOk
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.toStatus
import au.com.shiftyjelly.pocketcasts.utils.Optional
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.rxSingle

@Singleton
class SubscriptionManagerImpl @Inject constructor(
    private val paymentClient: PaymentClient,
    private val subscriptionMapper: SubscriptionMapper,
    private val syncManager: SyncManager,
    private val settings: Settings,
    private val productDetailsInterceptor: ProductDetailsInterceptor,
) : SubscriptionManager {

    private var cachedSubscriptionStatus: SubscriptionStatus?
        get() = settings.cachedSubscriptionStatus.value
        set(value) = settings.cachedSubscriptionStatus.set(value, updateModifiedAt = false)

    private val productDetails = BehaviorRelay.create<ProductDetailsState>()
    private val purchaseEvents = PublishRelay.create<PurchaseResult>()
    private val subscriptionChangedEvents = PublishRelay.create<SubscriptionChangedEvent>()

    private var hasOfferEligible = ConcurrentHashMap<SubscriptionTier, Boolean>()

    override fun signOut() {
        clearCachedStatus()
    }

    override fun observeProductDetails(): Flowable<ProductDetailsState> {
        return productDetails.toFlowable(BackpressureStrategy.LATEST)
    }

    override fun observePurchaseEvents(): Flowable<PurchaseResult> {
        return purchaseEvents.toFlowable(BackpressureStrategy.LATEST)
    }

    override fun observeSubscriptionStatus(): Flowable<Optional<SubscriptionStatus>> {
        return settings.cachedSubscriptionStatus.flow
            .map { status -> Optional.of(status) }
            .asFlowable()
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

    override suspend fun refresh() {
        loadProducts()
        loadPurchaseHistory()
        paymentClient.acknowledgePendingPurchases()
    }

    override suspend fun initializeBillingConnection(): Nothing = coroutineScope {
        launch { paymentClient.monitorPurchaseUpdates() }
        paymentClient.purchaseEvents.collect(purchaseEvents::accept)
    }

    override suspend fun loadProducts(): ProductDetailsState {
        val (result, products) = paymentClient.loadProducts(productDetailsParams)
        val interceptedResult = productDetailsInterceptor.intercept(result, products)
        val state = if (interceptedResult.first.isOk()) {
            ProductDetailsState.Loaded(interceptedResult.second)
        } else {
            ProductDetailsState.Failure
        }
        productDetails.accept(state)
        return state
    }

    override suspend fun loadPurchases() = coroutineScope {
        val (purchasesResult, purchases) = paymentClient.loadPurchases(purchasesParams)

        if (purchasesResult.isOk()) {
            PurchasesState.Loaded(purchases)
        } else {
            PurchasesState.Failure
        }
    }

    override suspend fun loadPurchaseHistory(): PurchaseHistoryState {
        val (historyResults, historyRecords) = paymentClient.loadPurchaseHistory(purchaseHistoryParams)
        return if (historyResults.isOk()) {
            historyRecords.forEach(::handleHistoryRecord)
            PurchaseHistoryState.Loaded(historyRecords)
        } else {
            PurchaseHistoryState.Failure
        }
    }

    private fun handleHistoryRecord(record: PurchaseHistoryRecord) {
        record.products
            .map(SubscriptionTier::fromProductId)
            .distinct()
            .forEach { tier -> updateOfferEligible(tier, false) }
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
            paymentClient.launchBillingFlow(activity, billingFlowParams)
        }
    }

    override suspend fun changeProduct(
        currentPurchase: Purchase,
        currentPurchaseProductId: String,
        newProduct: ProductDetails,
        newProductOfferToken: String,
        activity: Activity,
    ): BillingResult {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(newProduct)
            .setOfferToken(newProductOfferToken)
            .build()

        val updateParams = getReplacementMode(currentPurchaseProductId, newProduct.productId)
            ?.let { replacementMode ->
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(currentPurchase.purchaseToken)
                    .setSubscriptionReplacementMode(replacementMode)
                    .build()
            }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .let { builder ->
                if (updateParams != null) {
                    builder.setSubscriptionUpdateParams(updateParams)
                } else {
                    builder
                }
            }
            .build()
        return paymentClient.launchBillingFlow(activity, billingFlowParams)
    }

    override suspend fun claimWinbackOffer(
        currentPurchase: Purchase,
        winbackProduct: ProductDetails,
        winbackOfferToken: String,
        activity: Activity,
    ): BillingResult {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(winbackProduct)
            .setOfferToken(winbackOfferToken)
            .build()

        val updateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
            .setOldPurchaseToken(currentPurchase.purchaseToken)
            .setSubscriptionReplacementMode(BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .setSubscriptionUpdateParams(updateParams)
            .build()

        return paymentClient.launchBillingFlow(activity, billingFlowParams)
    }

    private suspend fun loadSubscriptionUpdateParamsMode(
        productDetails: ProductDetails,
    ): Pair<BillingResult, BillingFlowParams.SubscriptionUpdateParams?> {
        val replacementMode = settings.cachedSubscriptionStatus.value
            ?.let(::getProductId)
            ?.let { productId -> getReplacementMode(oldProductId = productId, newProductId = productDetails.productId) }

        if (replacementMode == null) {
            return okResult to null
        }

        val (result, purchases) = paymentClient.loadPurchases(purchasesParams)
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
        return cachedSubscriptionStatus
    }

    override fun clearCachedStatus() {
        cachedSubscriptionStatus = null
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
                is ProductDetailsState.Failure -> null
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
    }
}

private fun getProductId(subscriptionStatus: SubscriptionStatus): String? {
    return (subscriptionStatus as? SubscriptionStatus.Paid)
        ?.takeIf { it.platform == SubscriptionPlatform.ANDROID }
        ?.let { status ->
            when (status.tier) {
                SubscriptionTier.NONE -> null
                SubscriptionTier.PLUS -> when (status.frequency) {
                    SubscriptionFrequency.NONE -> null
                    SubscriptionFrequency.MONTHLY -> PLUS_MONTHLY_PRODUCT_ID
                    SubscriptionFrequency.YEARLY -> PLUS_YEARLY_PRODUCT_ID
                }
                SubscriptionTier.PATRON -> when (status.frequency) {
                    SubscriptionFrequency.NONE -> null
                    SubscriptionFrequency.MONTHLY -> PATRON_MONTHLY_PRODUCT_ID
                    SubscriptionFrequency.YEARLY -> PATRON_YEARLY_PRODUCT_ID
                }
            }
        }
}

/**
 * Determines the correct subscription replacement mode when a user switches subscriptions
 * from one plan to another, based on a predefined transition table.
 *
 * Transition Table:
 * ```
 * |                | Plus Monthly        | Patron Monthly        | Plus Yearly           | Patron Yearly         |
 * | Plus Monthly   | N/A                 | CHARGE_PRORATED_PRICE | CHARGE_FULL_PRICE     | CHARGE_FULL_PRICE     |
 * | Patron Monthly | WITH_TIME_PRORATION | N/A                   | CHARGE_FULL_PRICE     | CHARGE_FULL_PRICE     |
 * | Plus Yearly    | WITH_TIME_PRORATION | WITH_TIME_PRORATION   | N/A                   | CHARGE_PRORATED_PRICE |
 * | Patron Yearly  | WITH_TIME_PRORATION | WITH_TIME_PRORATION   | WITH_TIME_PRORATION   | N/A                   |
 * ```
 *
 * * `CHARGE_PRORATED_PRICE`: Charge the price difference and keep the billing cycle.
 * * `CHARGE_FULL_PRICE`: Upgrade or downgrade immediately. The remaining value is either carried over or prorated for time.
 * * `WITH_TIME_PRORATION`: Upgrade or downgrade immediately. The remaining time is adjusted based on the price difference.
 *
 * Note: We avoid using the DEFERRED replacement mode because it leaves users
 * without an active subscription until the deferred date, which complicates handling
 * further plan changes.
 *
 * See the [documentation](https://developer.android.com/google/play/billing/subscriptions#replacement-modes) for details
 * on replacement modes.
 */
private fun getReplacementMode(
    oldProductId: String,
    newProductId: String,
) = when (oldProductId) {
    PLUS_MONTHLY_PRODUCT_ID -> when (newProductId) {
        PATRON_MONTHLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE
        PLUS_YEARLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
        PATRON_YEARLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
        else -> null
    }

    PATRON_MONTHLY_PRODUCT_ID -> when (newProductId) {
        PLUS_MONTHLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
        PLUS_YEARLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
        PATRON_YEARLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE
        else -> null
    }

    PLUS_YEARLY_PRODUCT_ID -> when (newProductId) {
        PLUS_MONTHLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
        PATRON_MONTHLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
        PATRON_YEARLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE
        else -> null
    }

    PATRON_YEARLY_PRODUCT_ID -> when (newProductId) {
        PLUS_MONTHLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
        PATRON_MONTHLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
        PLUS_YEARLY_PRODUCT_ID -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
        else -> null
    }

    else -> null
}

sealed interface ProductDetailsState {
    data class Loaded(val productDetails: List<ProductDetails>) : ProductDetailsState

    data object Failure : ProductDetailsState
}

sealed interface PurchasesState {
    data class Loaded(val purchases: List<Purchase>) : PurchasesState

    data object Failure : PurchasesState
}

sealed interface PurchaseHistoryState {
    data class Loaded(val history: List<PurchaseHistoryRecord>) : PurchaseHistoryState

    data object Failure : PurchaseHistoryState
}

sealed class SubscriptionChangedEvent {
    object AccountUpgradedToPlus : SubscriptionChangedEvent()
    object AccountDowngradedToFree : SubscriptionChangedEvent()
}

data class FreeTrial(
    val subscriptionTier: SubscriptionTier,
    val exists: Boolean = false,
)
