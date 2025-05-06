package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class PaymentClient @Inject constructor(
    private val dataSource: PaymentDataSource,
    private val purchaseApprover: PurchaseApprover,
    private val logger: Logger,
) {
    private val purchaseEvents = MutableSharedFlow<PurchaseResult>()
    private val pendingPurchases = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    suspend fun monitorPurchaseUpdates(): Nothing = coroutineScope {
        launch { loadAndAcknowledgePurchases() }
        listenToPurchaseUpdates()
    }

    suspend fun loadSubscriptionPlans(): PaymentResult<SubscriptionPlans> {
        logger.info("Load subscription plans")
        return dataSource
            .loadProducts()
            .flatMap(SubscriptionPlans::create)
            .onSuccess { plans -> logger.info("Subscription plans loaded: $plans") }
            .onFailure { code, message -> logger.warning("Failed to load subscription plans. $code, $message") }
    }

    suspend fun purchaseSubscriptionPlan(
        key: SubscriptionPlan.Key,
        activity: Activity,
    ): PurchaseResult = coroutineScope {
        val purchaseConfirmationDeferred = async { purchaseEvents.first() }
        val billingResult = dataSource.launchBillingFlow(key, activity)
        when (billingResult) {
            is PaymentResult.Success -> {
                purchaseConfirmationDeferred.await()
            }

            is PaymentResult.Failure -> {
                logger.warning("Launching billing flow failed: ${billingResult.code} ${billingResult.message}")
                purchaseConfirmationDeferred.cancel()
                billingResult.toPurchaseResult()
            }
        }
    }

    private suspend fun loadAndAcknowledgePurchases() {
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
                    logger.warning("Purchase failure: ${recoveredResult.code} ${recoveredResult.message}")
                    purchaseEvents.emit(recoveredResult.toPurchaseResult())
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
                .onFailure { code, message -> logger.warning("Failed to confirm purchase: $purchase. $code $message") }
            if (dispatchConfirmation) {
                purchaseEvents.emit(confirmResult.toPurchaseResult())
            }
            pendingPurchases.remove(purchase.state.orderId)
        }
    }
}

private fun PaymentResult<*>.toPurchaseResult() = when (this) {
    is PaymentResult.Success -> PurchaseResult.Purchased
    is PaymentResult.Failure -> when (code) {
        PaymentResultCode.UserCancelled -> PurchaseResult.Cancelled
        else -> PurchaseResult.Failure(code)
    }
}
