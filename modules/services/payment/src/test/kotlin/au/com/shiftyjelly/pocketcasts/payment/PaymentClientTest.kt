package au.com.shiftyjelly.pocketcasts.payment

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentClientTest {
    private val dataSource = PaymentDataSource.fake()
    private val logger = TestLogger()
    private val client = PaymentClient(dataSource, logger)

    @Test
    fun `load plans successfully`() = runTest {
        val plans = client.loadSubscriptionPlans()

        assertNotNull(plans.getOrNull())
    }

    @Test
    fun `load plans with failure`() = runTest {
        dataSource.customProductsResult = PaymentResult.Failure(PaymentResultCode.Error, "Test failure")

        val plans = client.loadSubscriptionPlans()

        assertNull(plans.getOrNull())
    }

    @Test
    fun `log loading plans successfully`() = runTest {
        val plans = client.loadSubscriptionPlans()

        logger.assertInfos(
            "Load subscription plans",
            "Subscription plans loaded: ${plans.getOrNull()}",
        )
    }

    @Test
    fun `log loading plans with failure`() = runTest {
        dataSource.customProductsResult = PaymentResult.Failure(PaymentResultCode.Error, "Test failure")

        client.loadSubscriptionPlans()

        logger.assertInfos("Load subscription plans")
        logger.assertWarnings("Failed to load subscription plans: Test failure")
    }
}
