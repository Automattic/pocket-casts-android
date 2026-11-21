package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

internal class SwitchablePaymentDataSource(
    private val realDataSource: Lazy<PaymentDataSource>,
    private val mockDataSource: PaymentDataSource,
    private val isMockEnabled: () -> Boolean,
) : PaymentDataSource {
    private val activeDataSource get() = if (isMockEnabled()) mockDataSource else realDataSource.value

    // Picks the source when collection starts, which is just before each purchase is launched
    override val purchaseResults: Flow<PaymentResult<List<Purchase>>> = flow {
        emitAll(activeDataSource.purchaseResults)
    }

    override suspend fun loadProducts() = activeDataSource.loadProducts()

    override suspend fun loadPurchases() = activeDataSource.loadPurchases()

    override suspend fun launchBillingFlow(key: SubscriptionPlan.Key, activity: Activity) = activeDataSource.launchBillingFlow(key, activity)

    override suspend fun acknowledgePurchase(purchase: Purchase) = activeDataSource.acknowledgePurchase(purchase)

    override suspend fun showInAppMessages(activity: Activity) = activeDataSource.showInAppMessages(activity)
}
