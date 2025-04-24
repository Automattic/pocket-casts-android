package au.com.shiftyjelly.pocketcasts.payment.billing

import android.app.Activity
import android.content.Context
import au.com.shiftyjelly.pocketcasts.payment.Logger
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchaseHistory
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PaymentDataSource(
    context: Context,
    private val logger: Logger,
) {
    private val _purchaseUpdates = MutableSharedFlow<Pair<BillingResult, List<Purchase>>>(
        extraBufferCapacity = 100, // Arbitrarily large number
    )
    val purchaseUpdates = _purchaseUpdates.asSharedFlow()

    private val connection = ClientConnection(
        context,
        listener = PurchasesUpdatedListener { billingResult, purchases ->
            logger.info("Purchase results updated")
            _purchaseUpdates.tryEmit(billingResult to purchases.orEmpty())
        },
        logger = logger,
    )

    suspend fun loadProducts(
        params: QueryProductDetailsParams,
    ): Pair<BillingResult, List<ProductDetails>> {
        logger.info("Loading products")
        return connection.withConnectedClient { client ->
            val result = client.queryProductDetails(params)
            if (result.billingResult.isOk()) {
                logger.info("Products loaded")
            } else {
                logger.warning("Failed to load products: ${result.billingResult.debugMessage}")
            }
            result.billingResult to result.productDetailsList.orEmpty()
        }
    }

    suspend fun loadPurchaseHistory(
        params: QueryPurchaseHistoryParams,
    ): Pair<BillingResult, List<PurchaseHistoryRecord>> {
        logger.info("Loading purchase history")
        return connection.withConnectedClient { client ->
            val result = client.queryPurchaseHistory(params)
            if (result.billingResult.isOk()) {
                logger.info("Purchase history loaded")
            } else {
                logger.warning("Failed to load purchase history: ${result.billingResult.debugMessage}")
            }
            result.billingResult to result.purchaseHistoryRecordList.orEmpty()
        }
    }

    suspend fun loadPurchases(
        params: QueryPurchasesParams,
    ): Pair<BillingResult, List<Purchase>> {
        logger.info("Loading purchases")
        return connection.withConnectedClient { client ->
            val result = client.queryPurchasesAsync(params)
            if (result.billingResult.isOk()) {
                logger.info("Purchases loaded")
            } else {
                logger.warning("Failed to load purchases: ${result.billingResult.debugMessage}")
            }
            result.billingResult to result.purchasesList
        }
    }

    suspend fun acknowledgePurchase(
        params: AcknowledgePurchaseParams,
    ): BillingResult {
        logger.info("Acknowledging purchase: ${params.purchaseToken}")
        return connection.withConnectedClient { client ->
            val result = client.acknowledgePurchase(params)
            if (result.isOk()) {
                logger.info("Purchase acknowledge: ${params.purchaseToken}")
            } else {
                logger.warning("Failed to acknowledge purchase: ${params.purchaseToken}, ${result.debugMessage}")
            }
            result
        }
    }

    suspend fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams,
    ): BillingResult {
        logger.info("Launching billing flow")
        return connection.withConnectedClient { client ->
            val result = client.launchBillingFlow(activity, params)
            if (result.isOk()) {
                logger.info("Launched billing flow")
            } else {
                logger.warning("Failed to launch billing flow: ${result.debugMessage}")
            }
            result
        }
    }
}
