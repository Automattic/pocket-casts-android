package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
import android.content.Context
import au.com.shiftyjelly.pocketcasts.payment.billing.BillingPaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.mock.MockBillingSettings
import au.com.shiftyjelly.pocketcasts.payment.mock.MockPaymentDataSource
import kotlinx.coroutines.flow.Flow

interface PaymentDataSource {
    val purchaseResults: Flow<PaymentResult<List<Purchase>>>

    suspend fun loadProducts(): PaymentResult<List<Product>>

    suspend fun loadPurchases(): PaymentResult<List<Purchase>>

    suspend fun launchBillingFlow(key: SubscriptionPlan.Key, activity: Activity): PaymentResult<Unit>

    suspend fun acknowledgePurchase(purchase: Purchase): PaymentResult<Purchase>

    suspend fun showInAppMessages(activity: Activity): PaymentResult<InAppMessageResult>

    companion object {
        fun billing(
            context: Context,
            listeners: Set<PaymentClient.Listener>,
        ): PaymentDataSource = BillingPaymentDataSource(context, listeners)

        fun switchable(
            context: Context,
            listeners: Set<PaymentClient.Listener>,
            mockBillingSettings: MockBillingSettings,
        ): PaymentDataSource = SwitchablePaymentDataSource(
            realDataSource = lazy { billing(context, listeners) },
            mockDataSource = MockPaymentDataSource { mockBillingSettings.config.value },
            isMockEnabled = { mockBillingSettings.isMockEnabled },
        )

        fun fake() = FakePaymentDataSource()
    }
}
