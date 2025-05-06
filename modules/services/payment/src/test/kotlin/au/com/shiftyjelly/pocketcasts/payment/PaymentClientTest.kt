package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class PaymentClientTest {
    private val dataSource = PaymentDataSource.fake()
    private val approver = TestPurchaseApprover()
    private val logger = TestLogger()

    private val client = PaymentClient(dataSource, approver, logger)

    private val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, SubscriptionBillingCycle.Monthly, offer = null)
    private val purchase = Purchase(
        state = PurchaseState.Purchased("orderId"),
        token = "token",
        productIds = listOf(planKey.productId),
        isAcknowledged = false,
        isAutoRenewing = false,
    )

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
    fun `purchase subscription`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Success(listOf(purchase)))
        approver.emitApproveResponse(PaymentResultCode.Ok)
        dataSource.emitAcknowledgeResponse(PaymentResultCode.Ok)

        assertEquals(PurchaseResult.Purchased, purchaseResult.await())
    }

    @Test
    fun `do not purchase subscription when billing result is failure`() = monitoredTest {
        dataSource.launchBillingFlowResultCode = PaymentResultCode.FeatureNotSupported

        val purchaseResult = purchaseSubscription()

        assertEquals(PurchaseResult.Failure(PaymentResultCode.FeatureNotSupported), purchaseResult.await())
    }

    @Test
    fun `do not purchase subscription when purchase result is failure`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Failure(PaymentResultCode.BillingUnavailable, "Error"))

        assertEquals(PurchaseResult.Failure(PaymentResultCode.BillingUnavailable), purchaseResult.await())
    }

    @Test
    fun `cancel purchase subscription when purchase result is cancelled`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Failure(PaymentResultCode.UserCancelled, "Error"))

        assertEquals(PurchaseResult.Cancelled, purchaseResult.await())
    }

    @Test
    fun `do not purchase subscription when approving fails`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Success(listOf(purchase)))
        approver.emitApproveResponse(PaymentResultCode.Error)

        assertEquals(PurchaseResult.Failure(PaymentResultCode.Error), purchaseResult.await())
    }

    @Test
    fun `do not purchase subscription when acknowledging fails`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Success(listOf(purchase)))
        approver.emitApproveResponse(PaymentResultCode.Ok)
        dataSource.emitAcknowledgeResponse(PaymentResultCode.Error)

        assertEquals(PurchaseResult.Failure(PaymentResultCode.Error), purchaseResult.await())
    }

    @Test
    fun `cancel purchase subscription when acknowledging is cancelled`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Success(listOf(purchase)))
        approver.emitApproveResponse(PaymentResultCode.Ok)
        dataSource.emitAcknowledgeResponse(PaymentResultCode.UserCancelled)

        assertEquals(PurchaseResult.Cancelled, purchaseResult.await())
    }

    @Test
    fun `wait for all purchases to be confirmed before finalizing purchase`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Success(listOf(purchase, purchase)))
        approver.emitApproveResponse(PaymentResultCode.Ok)
        dataSource.emitAcknowledgeResponse(PaymentResultCode.Ok)

        yield() // Yield to make sure the job didn't cancel
        assertTrue(purchaseResult.isActive)

        approver.emitApproveResponse(PaymentResultCode.Ok)
        dataSource.emitAcknowledgeResponse(PaymentResultCode.Ok)

        assertEquals(PurchaseResult.Purchased, purchaseResult.await())
    }

    @Test
    fun `do not finalize non confirmed purchases`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Success(listOf(purchase.copy(state = PurchaseState.Pending))))
        approver.emitApproveResponse(PaymentResultCode.Ok)
        dataSource.emitAcknowledgeResponse(PaymentResultCode.Ok)

        yield() // Yield to make sure the job didn't cancel
        assertTrue(purchaseResult.isActive)
        assertTrue(approver.receivedPurchases.isEmpty())
    }

    @Test
    fun `do not acknowledge purchases that are already acknowledged`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Success(listOf(purchase.copy(isAcknowledged = true))))
        approver.emitApproveResponse(PaymentResultCode.Ok)

        assertTrue(dataSource.receivedPurchases.isEmpty())
        assertEquals(PurchaseResult.Purchased, purchaseResult.await())
    }

    @Test
    fun `acknowledge lingering purchases when monitoring starts`() = runTest {
        val purchases = listOf(
            purchase.copy(state = PurchaseState.Purchased("order-id-1")),
            purchase.copy(isAcknowledged = true),
            purchase.copy(state = PurchaseState.Pending),
            purchase.copy(state = PurchaseState.Purchased("order-id-2")),
        )
        dataSource.customPurchases = PaymentResult.Success(purchases)

        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { client.monitorPurchaseUpdates() }
        yield() // Yield due to starting internal job inside monitorPurchaseUpdates()

        approver.emitApproveResponse(PaymentResultCode.Ok)
        dataSource.emitAcknowledgeResponse(PaymentResultCode.Ok)
        approver.emitApproveResponse(PaymentResultCode.Ok)
        dataSource.emitAcknowledgeResponse(PaymentResultCode.Ok)

        val expectedPurchases = listOf(purchases[0], purchases[3])
        assertEquals(expectedPurchases, approver.receivedPurchases)
        assertEquals(expectedPurchases, dataSource.receivedPurchases)
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
        logger.assertWarnings("Failed to load subscription plans. Error, Test failure")
    }

    @Test
    fun `log confirming purchase`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Success(listOf(purchase)))
        approver.emitApproveResponse(PaymentResultCode.Ok)
        dataSource.emitAcknowledgeResponse(PaymentResultCode.Ok)
        purchaseResult.await()

        logger.assertInfos(
            "Confirm purchase: $purchase",
            "Purchase confirmed: ${purchase.copy(isAcknowledged = true)}",
        )
    }

    @Test
    fun `log confirming purchase failure`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Success(listOf(purchase)))
        approver.emitApproveResponse(PaymentResultCode.DeveloperError)
        purchaseResult.await()

        logger.assertInfos(
            "Confirm purchase: $purchase",
        )
        logger.assertWarnings(
            "Failed to confirm purchase: $purchase. DeveloperError Error",
        )
    }

    @Test
    fun `log purchase result failure`() = monitoredTest {
        val purchaseResult = purchaseSubscription()

        dataSource.purchaseResults.emit(PaymentResult.Failure(PaymentResultCode.ServiceDisconnected, "Whoops!"))
        purchaseResult.await()

        logger.assertWarnings(
            "Purchase failure: ServiceDisconnected Whoops!",
        )
    }

    @Test
    fun `log billing result failure`() = monitoredTest {
        dataSource.launchBillingFlowResultCode = PaymentResultCode.DeveloperError

        purchaseSubscription()

        logger.assertWarnings(
            "Launching billing flow failed: DeveloperError Error",
        )
    }

    private fun monitoredTest(testBody: suspend TestScope.() -> Unit) = runTest {
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { client.monitorPurchaseUpdates() }
        testBody()
    }

    private fun TestScope.purchaseSubscription(
        key: SubscriptionPlan.Key = planKey,
    ) = backgroundScope.async(start = CoroutineStart.UNDISPATCHED) { client.purchaseSubscriptionPlan(key, mock<Activity>()) }
}
