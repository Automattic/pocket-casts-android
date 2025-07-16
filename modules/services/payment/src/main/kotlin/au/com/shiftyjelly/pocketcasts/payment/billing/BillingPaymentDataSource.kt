package au.com.shiftyjelly.pocketcasts.payment.billing

import android.app.Activity
import android.content.Context
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.PaymentResult
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.Product
import au.com.shiftyjelly.pocketcasts.payment.Purchase
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.flatMap
import au.com.shiftyjelly.pocketcasts.payment.map
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.android.billingclient.api.Purchase as GooglePurchase

internal class BillingPaymentDataSource(
    context: Context,
    private val listeners: Set<PaymentClient.Listener>,
) : PaymentDataSource {

    private val connection = ClientConnection(
        context,
        purchaseUpdateListener = PurchasesUpdatedListener { billingResult, googlePurchases ->
            val result = if (billingResult.isOk()) {
                PaymentResult.Success(googlePurchases?.map(mapper::toPurchase).orEmpty())
            } else {
                billingResult.toPaymentFailure()
            }
            _purchases.tryEmit(result)
        },
        diagnosticListeners = listeners,
    )

    private val mapper = BillingPaymentMapper(listeners)

    private val _purchases = MutableSharedFlow<PaymentResult<List<Purchase>>>(
        extraBufferCapacity = 100, // Arbitrarily large number
    )

    override val purchaseResults = _purchases.asSharedFlow()

    override suspend fun loadProducts(): PaymentResult<List<Product>> {
        return connection.withConnectedClient { client ->
            client
                .queryProductDetails(AllSubscriptionsQueryProductDetailsParams)
                .toPaymentResult()
                .map { productDetails -> productDetails.mapNotNull(mapper::toProduct) }
        }
    }

    override suspend fun loadPurchases(): PaymentResult<List<Purchase>> {
        return connection.withConnectedClient { client ->
            client
                .queryPurchasesAsync(SubscriptionsQueryPurchasesParams)
                .toPaymentResult()
                .map { googlePurchases -> googlePurchases.mapNotNull(mapper::toPurchase) }
        }
    }

    override suspend fun launchBillingFlow(
        key: SubscriptionPlan.Key,
        activity: Activity,
    ): PaymentResult<Unit> {
        return connection.withConnectedClient { client ->
            client.queryProductDetails(AllSubscriptionsQueryProductDetailsParams)
                .toPaymentResult()
                .flatMap { productDetails ->
                    client.queryPurchasesAsync(SubscriptionsQueryPurchasesParams)
                        .toPaymentResult()
                        .map { purchases -> productDetails to purchases }
                }
                .flatMap { (productDetails, purchases) ->
                    val params = mapper.toBillingFlowRequest(key, productDetails, purchases)?.toGoogleParams()
                    if (params == null) {
                        PaymentResult.Failure(PaymentResultCode.DeveloperError, "Couldn't create billing flow params")
                    } else {
                        val billingResult = client.launchBillingFlow(activity, params)
                        if (billingResult.isOk()) {
                            PaymentResult.Success(Unit)
                        } else {
                            billingResult.toPaymentFailure()
                        }
                    }
                }
        }
    }

    override suspend fun acknowledgePurchase(purchase: Purchase): PaymentResult<Purchase> {
        return connection.withConnectedClient { client ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.token)
                .build()
            val billingResult = client.acknowledgePurchase(params)
            if (billingResult.isOk()) {
                PaymentResult.Success(purchase.copy(isAcknowledged = true))
            } else {
                billingResult.toPaymentFailure()
            }
        }
    }
}

private val AllSubscriptionsQueryProductDetailsParams = QueryProductDetailsParams.newBuilder()
    .setProductList(
        listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionPlan.PATRON_MONTHLY_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        ),
    )
    .build()

private val SubscriptionsQueryPurchasesParams = QueryPurchasesParams.newBuilder()
    .setProductType(BillingClient.ProductType.SUBS)
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

private fun BillingResult.isOk() = responseCode == BillingClient.BillingResponseCode.OK
