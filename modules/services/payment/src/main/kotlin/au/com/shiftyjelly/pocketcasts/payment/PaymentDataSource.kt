package au.com.shiftyjelly.pocketcasts.payment

import android.content.Context
import au.com.shiftyjelly.pocketcasts.payment.billing.BillingPaymentDataSource

interface PaymentDataSource {
    suspend fun loadProducts(): PaymentResult<List<Product>>

    companion object {
        fun billing(
            context: Context,
            logger: Logger,
        ): PaymentDataSource = BillingPaymentDataSource(context, logger)

        fun fake() = FakePaymentDataSource()
    }
}
