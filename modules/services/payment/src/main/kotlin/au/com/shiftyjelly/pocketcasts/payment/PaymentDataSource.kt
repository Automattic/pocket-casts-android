package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
import android.content.Context
import au.com.shiftyjelly.pocketcasts.payment.billing.BillingPaymentDataSource
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.SharedFlow
import com.android.billingclient.api.Purchase as GooglePurchase

interface PaymentDataSource {
    val purchaseResults: SharedFlow<PaymentResult<List<Purchase>>>

    suspend fun loadProducts(): PaymentResult<List<Product>>

    suspend fun loadPurchases(): PaymentResult<List<Purchase>>

    suspend fun acknowledgePurchase(purchase: Purchase): PaymentResult<Purchase>

    suspend fun launchBillingFlow(key: SubscriptionPlan.Key, activity: Activity): PaymentResult<Unit>

    companion object {
        fun billing(
            context: Context,
            logger: Logger,
        ): PaymentDataSource = BillingPaymentDataSource(context, logger)

        fun fake() = FakePaymentDataSource()
    }

    // <editor-fold desc="Temporarily extracted old interface">
    val purchaseUpdates: SharedFlow<Pair<BillingResult, List<GooglePurchase>>>

    suspend fun loadProducts(
        params: QueryProductDetailsParams,
    ): Pair<BillingResult, List<ProductDetails>>

    suspend fun loadPurchaseHistory(
        params: QueryPurchaseHistoryParams,
    ): Pair<BillingResult, List<PurchaseHistoryRecord>>

    suspend fun loadPurchases(
        params: QueryPurchasesParams,
    ): Pair<BillingResult, List<GooglePurchase>>

    suspend fun acknowledgePurchase(
        params: AcknowledgePurchaseParams,
    ): BillingResult

    suspend fun launchBillingFlow(
        activity: Activity,
        params: BillingFlowParams,
    ): BillingResult
    // </editor-fold>
}
