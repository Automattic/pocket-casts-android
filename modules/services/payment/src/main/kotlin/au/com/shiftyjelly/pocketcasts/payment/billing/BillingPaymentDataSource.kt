package au.com.shiftyjelly.pocketcasts.payment.billing

import android.app.Activity
import android.content.Context
import au.com.shiftyjelly.pocketcasts.payment.Logger
import au.com.shiftyjelly.pocketcasts.payment.PaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.Product
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionBillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.map
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
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

internal class BillingPaymentDataSource(
    context: Context,
    private val logger: Logger,
) : PaymentDataSource {
    private val _purchaseUpdates = MutableSharedFlow<Pair<BillingResult, List<Purchase>>>(
        extraBufferCapacity = 100, // Arbitrarily large number
    )
    override val purchaseUpdates = _purchaseUpdates.asSharedFlow()

    private val connection = ClientConnection(
        context,
        listener = PurchasesUpdatedListener { billingResult, purchases ->
            logger.info("Purchase results updated")
            _purchaseUpdates.tryEmit(billingResult to purchases.orEmpty())
        },
        logger = logger,
    )

    override suspend fun loadProducts(
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

    override suspend fun loadPurchaseHistory(
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

    override suspend fun loadPurchases(
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

    override suspend fun acknowledgePurchase(
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

    override suspend fun launchBillingFlow(
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

    // <editor-fold desc="PaymentDataSource implementation in progress">
    private val mapper = BillingPaymentMapper(logger)

    override suspend fun loadProducts(): PaymentResult<List<Product>> {
        return connection.withConnectedClient { client ->
            client
                .queryProductDetails(AllSubscriptionsQueryProductDetailsParams)
                .toPaymentResult()
                .map { productDetails -> productDetails.mapNotNull(mapper::toProduct) }
        }
    }
    // </editor-fold>
}

private val AllSubscriptionsQueryProductDetailsParams = QueryProductDetailsParams.newBuilder()
    .setProductList(
        listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionPlan.productId(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly))
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionPlan.productId(SubscriptionTier.Plus, SubscriptionBillingCycle.Yearly))
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionPlan.productId(SubscriptionTier.Patron, SubscriptionBillingCycle.Monthly))
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionPlan.productId(SubscriptionTier.Patron, SubscriptionBillingCycle.Yearly))
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        ),
    )
    .build()

private fun ProductDetailsResult.toPaymentResult(): PaymentResult<List<ProductDetails>> {
    return if (billingResult.isOk()) {
        PaymentResult.Success(productDetailsList.orEmpty())
    } else {
        billingResult.toPaymentFailure()
    }
}

private fun PurchasesResult.toPaymentResult(): PaymentResult<List<GooglePurchase>> {
    return if (billingResult.isOk()) {
        PaymentResult.Success(purchasesList.orEmpty())
    } else {
        billingResult.toPaymentFailure()
    }
}

private fun BillingResult.toPaymentFailure() = PaymentResult.Failure(
    when (responseCode) {
        -2 -> PaymentResultCode.FeatureNotSupported
        -1 -> PaymentResultCode.ServiceDisconnected
        0 -> PaymentResultCode.Ok
        1 -> PaymentResultCode.UserCancelled
        2, -3 -> PaymentResultCode.ServiceUnavailable
        3 -> PaymentResultCode.BillingUnavailable
        4 -> PaymentResultCode.ItemUnavailable
        5 -> PaymentResultCode.DeveloperError
        6 -> PaymentResultCode.Error
        7 -> PaymentResultCode.ItemAlreadyOwned
        8 -> PaymentResultCode.ItemNotOwned
        12 -> PaymentResultCode.NetworkError
        else -> PaymentResultCode.Unknown(responseCode)
    },
    debugMessage,
)
