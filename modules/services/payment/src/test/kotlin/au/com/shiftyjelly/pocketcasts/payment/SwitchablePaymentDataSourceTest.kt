package au.com.shiftyjelly.pocketcasts.payment

import au.com.shiftyjelly.pocketcasts.payment.mock.MockBillingConfig
import au.com.shiftyjelly.pocketcasts.payment.mock.MockPaymentDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.mock

class SwitchablePaymentDataSourceTest {
    private val realDataSource = FakePaymentDataSource()
    private val mockDataSource = FakePaymentDataSource().apply {
        loadedProductsResultCode = PaymentResultCode.ServiceUnavailable
    }
    private var isMockEnabled = false
    private val realDataSourceDelegate = lazy { realDataSource }

    private val dataSource = SwitchablePaymentDataSource(
        realDataSource = realDataSourceDelegate,
        mockDataSource = mockDataSource,
        isMockEnabled = { isMockEnabled },
    )

    @Test
    fun `uses the real data source when mocks are off`() = runTest {
        val result = dataSource.loadProducts()

        assertEquals(realDataSource.loadedProducts, result.getOrNull())
    }

    @Test
    fun `switches to mocks without recreating the data source`() = runTest {
        dataSource.loadProducts()
        isMockEnabled = true

        val result = dataSource.loadProducts()

        assertEquals(PaymentResultCode.ServiceUnavailable, (result as PaymentResult.Failure).code)
    }

    @Test
    fun `completes a mock purchase through the payment client`() = runTest {
        val switchableDataSource = SwitchablePaymentDataSource(
            realDataSource = realDataSourceDelegate,
            mockDataSource = MockPaymentDataSource { MockBillingConfig(isEnabled = true) },
            isMockEnabled = { true },
        )
        val client = PaymentClient.test(switchableDataSource)
        val key = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Yearly, SubscriptionOffer.IntroOffer)

        val result = client.purchaseSubscriptionPlan(key, purchaseSource = "test", activity = mock())

        assertEquals(PurchaseResult.Purchased, result)
        assertFalse(realDataSourceDelegate.isInitialized())
    }

    @Test
    fun `acknowledges real purchases with the real data source even when mocks are on`() = runTest {
        val realPurchase = FakePaymentDataSource.DefaultPurchasedProduct
        val mockPurchase = realPurchase.copy(token = MockPaymentDataSource.MOCK_PURCHASE_TOKEN)
        isMockEnabled = true

        dataSource.acknowledgePurchase(realPurchase)
        dataSource.acknowledgePurchase(mockPurchase)

        assertEquals(listOf(realPurchase), realDataSource.receivedPurchases)
        assertEquals(listOf(mockPurchase), mockDataSource.receivedPurchases)
    }

    @Test
    fun `does not create the real data source while mocks are on`() = runTest {
        isMockEnabled = true

        dataSource.loadProducts()
        dataSource.loadPurchases()

        assertFalse(realDataSourceDelegate.isInitialized())
    }
}
