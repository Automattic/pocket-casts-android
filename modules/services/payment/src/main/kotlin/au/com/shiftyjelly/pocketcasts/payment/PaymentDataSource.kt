package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
import android.content.Context
import au.com.shiftyjelly.pocketcasts.payment.billing.BillingPaymentDataSource
import kotlinx.coroutines.flow.SharedFlow

interface PaymentDataSource {
    val purchaseResults: SharedFlow<PaymentResult<List<Purchase>>>

    suspend fun loadProducts(): PaymentResult<List<Product>>

    suspend fun loadPurchases(): PaymentResult<List<Purchase>>

    suspend fun launchBillingFlow(key: SubscriptionPlan.Key, activity: Activity): PaymentResult<Unit>

    suspend fun acknowledgePurchase(purchase: Purchase): PaymentResult<Purchase>

    companion object {
        fun billing(
            context: Context,
            listeners: Set<PaymentClient.Listener>,
        ): PaymentDataSource = BillingPaymentDataSource(context, listeners)

        fun fake() = FakePaymentDataSource()
    }
}
