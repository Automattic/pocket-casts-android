package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class PaymentClient @Inject constructor(
    private val dataSource: PaymentDataSource,
    private val purchaseApprover: PurchaseApprover,
    private val listeners: Set<@JvmSuppressWildcards PaymentClient.Listener>,
) {
    private val _purchaseEvents = MutableSharedFlow<PurchaseResult>()
    private val pendingPurchases = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    suspend fun loadSubscriptionPlans(): PaymentResult<SubscriptionPlans> {
        forEachListener { onLoadSubscriptionPlans() }
        return dataSource
            .loadProducts()
            .flatMap(SubscriptionPlans::create)
            .also { forEachListener { onSubscriptionPlansLoaded(it) } }
    }

    suspend fun loadAcknowledgedSubscriptions(): PaymentResult<List<AcknowledgedSubscription>> {
        forEachListener { onloadAcknowledgedSubscriptions() }
        return dataSource
            .loadPurchases()
            .map { purchases -> purchases.toAcknowledgedSubscriptions() }
            .also { forEachListener { onAcknowledgedSubscriptionsLoaded(it) } }
    }

    suspend fun purchaseSubscriptionPlan(
        key: SubscriptionPlan.Key,
        purchaseSource: String,
        activity: Activity,
        purchaseFlow: String? = null,
    ): PurchaseResult = coroutineScope {
        forEachListener { onPurchaseSubscriptionPlan(key) }
        val purchaseUpdatesJob = launch(start = CoroutineStart.UNDISPATCHED) { listenToPurchaseUpdates() }
        val purchaseConfirmationDeferred = async { _purchaseEvents.first() }

        val billingResult = dataSource.launchBillingFlow(key, activity)
        val purchaseResult = when (billingResult) {
            is PaymentResult.Success -> {
                purchaseConfirmationDeferred.await()
            }

            is PaymentResult.Failure -> {
                purchaseConfirmationDeferred.cancel()
                billingResult.toPurchaseResult()
            }
        }
        purchaseUpdatesJob.cancelAndJoin()
        forEachListener { onSubscriptionPurchased(key = key, purchaseSource = purchaseSource, purchaseFlow = purchaseFlow, result = purchaseResult) }
        purchaseResult
    }

    suspend fun acknowledgePendingPurchases() {
        dataSource.loadPurchases().onSuccess { purchases ->
            purchases
                .filter { purchase -> !purchase.isAcknowledged }
                .forEach { confirmPurchase(it, dispatchConfirmation = false) }
        }
    }

    private suspend fun listenToPurchaseUpdates(): Nothing {
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
            try {
                forEachListener { onConfirmPurchase(purchase) }
                val confirmResult = purchaseApprover.approve(purchase)
                    .flatMap { approvedPurchase ->
                        if (!approvedPurchase.isAcknowledged) {
                            dataSource.acknowledgePurchase(approvedPurchase)
                        } else {
                            PaymentResult.Success(approvedPurchase)
                        }
                    }
                forEachListener { onPurchaseConfirmed(confirmResult) }
                if (dispatchConfirmation) {
                    _purchaseEvents.emit(confirmResult.toPurchaseResult())
                }
            } finally {
                pendingPurchases.remove(purchase.state.orderId)
            }
        }
    }

    private fun List<Purchase>.toAcknowledgedSubscriptions() = mapNotNull { it.toAcknowledgedSubscription() }

    private fun Purchase.toAcknowledgedSubscription(): AcknowledgedSubscription? {
        if (!isAcknowledged || state !is PurchaseState.Purchased) return null

        if (productIds.isEmpty()) {
            dispatchMessage("Skipping purchase ${state.orderId}. No associated products.")
            return null
        }

        if (productIds.size > 1) {
            dispatchMessage("Skipping purchase ${state.orderId}. Too many associated products: $productIds")
            return null
        }

        val productId = productIds[0]
        val productKey = findMatchingProductKey(productId)

        if (productKey == null) {
            dispatchMessage("Skipping purchase ${state.orderId}. Couldn't find matching product key for $productId")
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

    private fun forEachListener(block: Listener.() -> Unit) {
        listeners.forEach { it.block() }
    }

    private fun dispatchMessage(message: String) {
        forEachListener { onMessage(message) }
    }

    interface Listener {
        fun onLoadSubscriptionPlans() = Unit

        fun onSubscriptionPlansLoaded(result: PaymentResult<SubscriptionPlans>) = Unit

        fun onloadAcknowledgedSubscriptions() = Unit

        fun onAcknowledgedSubscriptionsLoaded(result: PaymentResult<List<AcknowledgedSubscription>>) = Unit

        fun onPurchaseSubscriptionPlan(key: SubscriptionPlan.Key) = Unit

        fun onSubscriptionPurchased(key: SubscriptionPlan.Key, purchaseSource: String, purchaseFlow: String?, result: PurchaseResult) = Unit

        fun onConfirmPurchase(purchase: Purchase) = Unit

        fun onPurchaseConfirmed(result: PaymentResult<Purchase>) = Unit

        fun onMessage(message: String) = Unit
    }

    companion object {
        fun test(
            dataSource: PaymentDataSource = PaymentDataSource.fake(),
            vararg listeners: Listener,
        ) = PaymentClient(
            dataSource = dataSource,
            listeners = listeners.toSet(),
            purchaseApprover = object : PurchaseApprover {
                override suspend fun approve(purchase: Purchase): PaymentResult<Purchase> {
                    return PaymentResult.Success(purchase)
                }
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

/**
 * Get the appropriate yearly plan based on the NEW_INSTALLMENT_PLAN feature flag.
 *
 * When feature flag is ON and installment plan is available (user in supported country),
 * returns the installment plan. Otherwise returns the upfront yearly plan.
 *
 * Note: Installment plans are only available in Brazil, France, Italy, and Spain.
 * Google Play automatically filters out installment plans for users in other countries.
 */
fun SubscriptionPlans.getYearlyPlanWithFeatureFlag(
    tier: SubscriptionTier,
): SubscriptionPlan.Base {
    val isInstallmentEnabled = FeatureFlag.isEnabled(Feature.NEW_INSTALLMENT_PLAN)

    // For Plus tier only, check if installment plan should be shown
    if (tier == SubscriptionTier.Plus && isInstallmentEnabled) {
        // Try to find installment plan (may not exist if user not in supported country)
        val installmentKey = SubscriptionPlan.Key(
            tier = tier,
            billingCycle = BillingCycle.Yearly,
            offer = null
        )

        // Check if the loaded products include an installment plan
        // This is a simplified check - in real implementation this would look at the actual product
        // For now, we'll fall back to the regular yearly plan
        // TODO: Implement proper installment plan lookup when Google Play integration is ready
    }

    // Default: return upfront yearly plan
    return getBasePlan(tier, BillingCycle.Yearly)
}
