package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class PaymentClient @Inject constructor(
    private val dataSource: PaymentDataSource,
    private val purchaseApprover: PurchaseApprover,
    private val logger: Logger,
) {
    private val _purchaseEvents = MutableSharedFlow<PurchaseResult>()
    private val pendingPurchases = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    suspend fun loadSubscriptionPlans(): PaymentResult<SubscriptionPlans> {
        logger.info("Load subscription plans")
        return dataSource
            .loadProducts()
            .flatMap(SubscriptionPlans::create)
            .onSuccess { plans -> logger.info("Subscription plans loaded: $plans") }
            .onFailure { code, message -> logger.warning("Failed to load subscription plans. $code, $message") }
    }

    suspend fun loadAcknowledgedSubscriptions(): PaymentResult<List<AcknowledgedSubscription>> {
        logger.info("Loading acknowledged subscriptions")
        return dataSource
            .loadPurchases()
            .map { purchases -> purchases.toAcknowledgedSubscriptions() }
            .onSuccess { subscriptions -> logger.info("Acknowledged subscriptions loaded: $subscriptions") }
            .onFailure { code, message -> logger.warning("Failed to load acknowledged subscriptions. $code, $message") }
    }

    suspend fun purchaseSubscriptionPlan(
        key: SubscriptionPlan.Key,
        activity: Activity,
    ): PurchaseResult = coroutineScope {
        val purchaseUpdatesJob = launch(start = CoroutineStart.UNDISPATCHED) { listenToPurchaseUpdates() }
        val purchaseConfirmationDeferred = async { _purchaseEvents.first() }

        val billingResult = dataSource.launchBillingFlow(key, activity)
        val purchaseResult = when (billingResult) {
            is PaymentResult.Success -> {
                purchaseConfirmationDeferred.await()
            }

            is PaymentResult.Failure -> {
                logger.warning("Launching billing flow failed: ${billingResult.code}, ${billingResult.message}")
                purchaseConfirmationDeferred.cancel()
                billingResult.toPurchaseResult()
            }
        }
        purchaseUpdatesJob.cancelAndJoin()
        purchaseResult
    }

    suspend fun acknowledgePendingPurchases() {
        dataSource.loadPurchases().onSuccess { purchases ->
            purchases
                .filter { purchase -> !purchase.isAcknowledged }
                .forEach { confirmPurchase(it, dispatchConfirmation = false) }
        }
    }

    suspend fun listenToPurchaseUpdates(): Nothing {
        dataSource.purchaseResults.collect { result ->
            val recoveredResult = result.recover { code, message ->
                when (code) {
                    PaymentResultCode.ItemAlreadyOwned -> dataSource.loadPurchases()
                    else -> PaymentResult.Failure(code, message)
                }
            }
            when (recoveredResult) {
                is PaymentResult.Success -> {
                    val purchases = recoveredResult.value
                    purchases.forEachIndexed { index, purchase ->
                        confirmPurchase(purchase, dispatchConfirmation = index == purchases.lastIndex)
                    }
                }

                is PaymentResult.Failure -> {
                    logger.warning("Purchase failure: ${recoveredResult.code}, ${recoveredResult.message}")
                    _purchaseEvents.emit(recoveredResult.toPurchaseResult())
                }
            }
        }
    }

    private suspend fun confirmPurchase(
        purchase: Purchase,
        dispatchConfirmation: Boolean,
    ) {
        if (purchase.state is PurchaseState.Purchased && pendingPurchases.add(purchase.state.orderId)) {
            logger.info("Confirm purchase: $purchase")

            val confirmResult = purchaseApprover.approve(purchase)
                .flatMap { approvedPurchase ->
                    if (!approvedPurchase.isAcknowledged) {
                        dataSource.acknowledgePurchase(approvedPurchase)
                    } else {
                        PaymentResult.Success(approvedPurchase)
                    }
                }
                .onSuccess { logger.info("Purchase confirmed: $it") }
                .onFailure { code, message -> logger.warning("Failed to confirm purchase: $purchase. $code, $message") }
            if (dispatchConfirmation) {
                _purchaseEvents.emit(confirmResult.toPurchaseResult())
            }
            pendingPurchases.remove(purchase.state.orderId)
        }
    }

    private fun List<Purchase>.toAcknowledgedSubscriptions() = mapNotNull { it.toAcknowledgedSubscription() }

    private fun Purchase.toAcknowledgedSubscription(): AcknowledgedSubscription? {
        if (!isAcknowledged || state !is PurchaseState.Purchased) return null

        if (productIds.isEmpty()) {
            logger.warning("Skipping purchase ${state.orderId}. No associated products.")
            return null
        }

        if (productIds.size > 1) {
            logger.warning("Skipping purchase ${state.orderId}. Too many associated products: $productIds")
            return null
        }

        val productId = productIds[0]
        val productKey = findMatchingProductKey(productId)

        if (productKey == null) {
            logger.warning("Skipping purchase ${state.orderId}. Couldn't find matching product key for $productId")
            return null
        }

        return AcknowledgedSubscription(state.orderId, productKey.tier, productKey.billingCycle, isAutoRenewing)
    }

    private fun findMatchingProductKey(productId: String): SubscriptionPlan.Key? {
        val keys = SubscriptionTier.entries.flatMap { tier ->
            BillingCycle.entries.map { cycle ->
                SubscriptionPlan.Key(tier, cycle, offer = null)
            }
        }
        return keys.firstOrNull { it.productId == productId }
    }

    // <editor-fold desc="Temporarily extracted old interface">
    val purchaseEvents = _purchaseEvents.asSharedFlow()

    suspend fun loadProducts(
        params: QueryProductDetailsParams,
    ): Pair<BillingResult, List<ProductDetails>> {
        return dataSource.loadProducts(params)
    }

    suspend fun loadPurchaseHistory(
        params: QueryPurchaseHistoryParams,
    ): Pair<BillingResult, List<PurchaseHistoryRecord>> {
        return dataSource.loadPurchaseHistory(params)
    }

    suspend fun loadPurchases(
        params: QueryPurchasesParams,
    ): Pair<BillingResult, List<com.android.billingclient.api.Purchase>> {
        return dataSource.loadPurchases(params)
    }

    suspend fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams,
    ): BillingResult {
        return dataSource.launchBillingFlow(activity, params)
    }
    // </editor-fold>

    companion object {
        fun test(dataSource: PaymentDataSource = PaymentDataSource.fake()) = PaymentClient(
            dataSource,
            purchaseApprover = object : PurchaseApprover {
                override suspend fun approve(purchase: Purchase): PaymentResult<Purchase> {
                    return PaymentResult.Success(purchase)
                }
            },
            logger = object : Logger {
                override fun info(message: String) = Unit
                override fun warning(message: String) = Unit
                override fun error(message: String, exception: Throwable) = Unit
            },
        )
    }
}

private fun PaymentResult<*>.toPurchaseResult() = when (this) {
    is PaymentResult.Success -> PurchaseResult.Purchased
    is PaymentResult.Failure -> when (code) {
        PaymentResultCode.UserCancelled -> PurchaseResult.Cancelled
        else -> PurchaseResult.Failure(code)
    }
}
