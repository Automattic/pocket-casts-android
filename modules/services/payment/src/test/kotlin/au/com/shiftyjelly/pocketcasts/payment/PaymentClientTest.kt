package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
import au.com.shiftyjelly.pocketcasts.payment.TestListener.Event
import kotlinx.coroutines.async
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
    private val listener = TestListener()

    private val client = PaymentClient(dataSource, approver, setOf(listener))

    private val planKey = SubscriptionPlan.Key(SubscriptionTier.Plus, BillingCycle.Monthly, offer = null)
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
        dataSource.loadedProductsResultCode = PaymentResultCode.Error

        val plans = client.loadSubscriptionPlans()

        assertNull(plans.getOrNull())
    }

    @Test
    fun `load acknowledged subscribtion purchases`() = runTest {
        dataSource.loadedPurchases = listOf(
            purchase.copy(
                state = PurchaseState.Purchased("order-id-1"),
                productIds = listOf(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID),
                isAcknowledged = true,
                isAutoRenewing = true,
            ),
            purchase.copy(
                state = PurchaseState.Purchased("order-id-2"),
                productIds = listOf(SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID),
                isAcknowledged = true,
                isAutoRenewing = true,
            ),
            purchase.copy(
                state = PurchaseState.Purchased("order-id-3"),
                productIds = listOf(SubscriptionPlan.PATRON_MONTHLY_PRODUCT_ID),
                isAcknowledged = true,
                isAutoRenewing = true,
            ),
            purchase.copy(
                state = PurchaseState.Purchased("order-id-4"),
                productIds = listOf(SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID),
                isAcknowledged = true,
                isAutoRenewing = false,
            ),
        )

        val subscriptions = client.loadAcknowledgedSubscriptions().getOrNull()!!

        assertEquals(
            listOf(
                AcknowledgedSubscription("order-id-1", SubscriptionTier.Plus, BillingCycle.Monthly, isAutoRenewing = true),
                AcknowledgedSubscription("order-id-2", SubscriptionTier.Plus, BillingCycle.Yearly, isAutoRenewing = true),
                AcknowledgedSubscription("order-id-3", SubscriptionTier.Patron, BillingCycle.Monthly, isAutoRenewing = true),
                AcknowledgedSubscription("order-id-4", SubscriptionTier.Patron, BillingCycle.Yearly, isAutoRenewing = false),
            ),
            subscriptions,
        )
    }

    @Test
    fun `do not load unconfirmed subscription purchases`() = runTest {
        dataSource.loadedPurchases = listOf(
            purchase.copy(
                state = PurchaseState.Pending,
                isAcknowledged = true,
            ),
            purchase.copy(
                state = PurchaseState.Unspecified,
                isAcknowledged = false,
            ),
            purchase.copy(
                state = PurchaseState.Purchased("order-id-3"),
                isAcknowledged = false,
            ),
        )

        val subscriptions = client.loadAcknowledgedSubscriptions().getOrNull()!!

        assertEquals(emptyList<AcknowledgedSubscription>(), subscriptions)
    }

    @Test
    fun `do not load acknowledged subscription purchases without any products`() = runTest {
        dataSource.loadedPurchases = listOf(
            purchase.copy(
                productIds = listOf(
                    SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID,
                    SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID,
                ),
            ),
        )

        val subscriptions = client.loadAcknowledgedSubscriptions().getOrNull()!!

        assertEquals(emptyList<AcknowledgedSubscription>(), subscriptions)
    }

    @Test
    fun `do not load acknowledged subscription purchases with unknown products`() = runTest {
        dataSource.loadedPurchases = listOf(
            purchase.copy(productIds = listOf("some-unknown-product")),
        )

        val subscriptions = client.loadAcknowledgedSubscriptions().getOrNull()!!

        assertEquals(emptyList<AcknowledgedSubscription>(), subscriptions)
    }

    @Test
    fun `do not load acknowledged subscription purchases without multiple products`() = runTest {
        dataSource.loadedPurchases = listOf(
            purchase.copy(productIds = emptyList()),
        )

        val subscriptions = client.loadAcknowledgedSubscriptions().getOrNull()!!

        assertEquals(emptyList<AcknowledgedSubscription>(), subscriptions)
    }

    @Test
    fun `ignore invalid purchases when loading acknowledged subscription purchases`() = runTest {
        dataSource.loadedPurchases = listOf(
            purchase.copy(
                state = PurchaseState.Purchased("order-id"),
                productIds = listOf(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID),
                isAcknowledged = true,
                isAutoRenewing = true,
            ),
            purchase.copy(productIds = emptyList()),
        )

        val subscriptions = client.loadAcknowledgedSubscriptions().getOrNull()!!

        assertEquals(
            listOf(
                AcknowledgedSubscription("order-id", SubscriptionTier.Plus, BillingCycle.Monthly, isAutoRenewing = true),
            ),
            subscriptions,
        )
    }

    @Test
    fun `load acknowledged subscriptions with failure`() = runTest {
        dataSource.loadedPurchasesResultCode = PaymentResultCode.Error

        val subscriptions = client.loadAcknowledgedSubscriptions()

        assertNull(subscriptions.getOrNull())
    }

    @Test
    fun `purchase subscription`() = runTest {
        val purchaseResult = client.purchaseSubscriptionPlan(planKey)

        assertEquals(PurchaseResult.Purchased, purchaseResult)
    }

    @Test
    fun `do not purchase subscription when billing result is failure`() = runTest {
        dataSource.billingFlowResultCode = PaymentResultCode.FeatureNotSupported

        val purchaseResult = client.purchaseSubscriptionPlan(planKey)

        assertEquals(PurchaseResult.Failure(PaymentResultCode.FeatureNotSupported), purchaseResult)
    }

    @Test
    fun `do not purchase subscription when purchase result is failure`() = runTest {
        dataSource.purchasedProductsResultCode = PaymentResultCode.Error

        val purchaseResult = client.purchaseSubscriptionPlan(planKey)

        assertEquals(PurchaseResult.Failure(PaymentResultCode.Error), purchaseResult)
    }

    @Test
    fun `cancel purchase subscription when purchase result is cancelled`() = runTest {
        dataSource.purchasedProductsResultCode = PaymentResultCode.UserCancelled

        val purchaseResult = client.purchaseSubscriptionPlan(planKey)

        assertEquals(PurchaseResult.Cancelled, purchaseResult)
    }

    @Test
    fun `do not purchase subscription when approving fails`() = runTest {
        approver.approveResultCode = PaymentResultCode.Error

        val purchaseResult = client.purchaseSubscriptionPlan(planKey)

        assertEquals(PurchaseResult.Failure(PaymentResultCode.Error), purchaseResult)
    }

    @Test
    fun `do not purchase subscription when acknowledging fails`() = runTest {
        dataSource.acknowledgePurchaseResultCode = PaymentResultCode.Error

        val purchaseResult = client.purchaseSubscriptionPlan(planKey)

        assertEquals(PurchaseResult.Failure(PaymentResultCode.Error), purchaseResult)
    }

    @Test
    fun `cancel purchase subscription when acknowledging is cancelled`() = runTest {
        dataSource.acknowledgePurchaseResultCode = PaymentResultCode.UserCancelled

        val purchaseResult = client.purchaseSubscriptionPlan(planKey)

        assertEquals(PurchaseResult.Cancelled, purchaseResult)
    }

    @Test
    fun `do not finalize non confirmed purchases`() = runTest {
        dataSource.purchasedProducts = listOf(
            purchase.copy(state = PurchaseState.Pending),
        )

        val purchaseResult = backgroundScope.async { client.purchaseSubscriptionPlan(planKey) }
        yield() // Yield to make sure the job didn't cancel

        assertTrue(purchaseResult.isActive)
        assertTrue(approver.receivedPurchases.isEmpty())
    }

    @Test
    fun `do not acknowledge purchases that are already acknowledged`() = runTest {
        dataSource.purchasedProducts = listOf(
            purchase.copy(isAcknowledged = true),
        )

        val purchaseResult = client.purchaseSubscriptionPlan(planKey)

        assertTrue(dataSource.receivedPurchases.isEmpty())
        assertEquals(PurchaseResult.Purchased, purchaseResult)
    }

    @Test
    fun `acknowledge lingering purchases`() = runTest {
        val purchases = listOf(
            purchase.copy(state = PurchaseState.Purchased("order-id-1")),
            purchase.copy(isAcknowledged = true),
            purchase.copy(state = PurchaseState.Pending),
            purchase.copy(state = PurchaseState.Purchased("order-id-2")),
        )
        dataSource.loadedPurchases = purchases

        client.acknowledgePendingPurchases()

        val expectedPurchases = listOf(purchases[0], purchases[3])
        assertEquals(expectedPurchases, approver.receivedPurchases)
        assertEquals(expectedPurchases, dataSource.receivedPurchases)
    }

    @Test
    fun `dispatch loading plans successfully`() = runTest {
        client.loadSubscriptionPlans()

        listener.assertEvents(
            Event.LoadSubscriptionPlans,
            Event.LoadSubscriptionPlansSuccess,
        )
    }

    @Test
    fun `dispatch loading plans with failure`() = runTest {
        dataSource.loadedProductsResultCode = PaymentResultCode.Error

        client.loadSubscriptionPlans()

        listener.assertEvents(
            Event.LoadSubscriptionPlans,
            Event.LoadSubscriptionPlansFailure,
        )
    }

    @Test
    fun `dispatch loading acknowledged subscription purchases succesfully`() = runTest {
        client.loadAcknowledgedSubscriptions()

        listener.assertEvents(
            Event.LoadAcknowledgedSubscriptions,
            Event.LoadAcknowledgedSubscriptionsSuccess,
        )
    }

    @Test
    fun `dispatch loading acknowledged subscription with failure`() = runTest {
        dataSource.loadedPurchasesResultCode = PaymentResultCode.Error

        client.loadAcknowledgedSubscriptions()

        listener.assertEvents(
            Event.LoadAcknowledgedSubscriptions,
            Event.LoadAcknowledgedSubscriptionsFailure,
        )
    }

    @Test
    fun `log issues with invalid acknowledged subscription purchases`() = runTest {
        dataSource.loadedPurchases = listOf(
            purchase.copy(
                state = PurchaseState.Pending,
                isAcknowledged = true,
            ),
            purchase.copy(
                state = PurchaseState.Purchased(orderId = "order-id-1"),
                isAcknowledged = false,
            ),
            purchase.copy(
                state = PurchaseState.Purchased(orderId = "order-id-2"),
                productIds = emptyList(),
                isAcknowledged = true,
            ),
            purchase.copy(
                state = PurchaseState.Purchased(orderId = "order-id-3"),
                productIds = listOf("product-id-1", "product-id-2"),
                isAcknowledged = true,
            ),
            purchase.copy(
                state = PurchaseState.Purchased(orderId = "order-id-4"),
                productIds = listOf("unknown-product-id"),
                isAcknowledged = true,
            ),
        )

        client.loadAcknowledgedSubscriptions()

        listener.assertMessages(
            "Skipping purchase order-id-2. No associated products.",
            "Skipping purchase order-id-3. Too many associated products: [product-id-1, product-id-2]",
            "Skipping purchase order-id-4. Couldn't find matching product key for unknown-product-id",
        )
    }

    @Test
    fun `dispatch purchase result success`() = runTest {
        client.purchaseSubscriptionPlan(planKey)

        listener.assertEvents(
            Event.PurchaseSubscriptionPlan,
            Event.ConfirmPurchase,
            Event.ConfirmPurchaseSuccess,
            Event.PurchaseSubscriptionPlanSuccess,
        )
    }

    @Test
    fun `dispatch purchase result cancellation`() = runTest {
        dataSource.purchasedProductsResultCode = PaymentResultCode.UserCancelled

        client.purchaseSubscriptionPlan(planKey)

        listener.assertEvents(
            Event.PurchaseSubscriptionPlan,
            Event.PurchaseSubscriptionPlanCancelled,
        )
    }

    @Test
    fun `dispatch purchase result failure`() = runTest {
        dataSource.purchasedProductsResultCode = PaymentResultCode.ServiceDisconnected

        client.purchaseSubscriptionPlan(planKey)

        listener.assertEvents(
            Event.PurchaseSubscriptionPlan,
            Event.PurchaseSubscriptionPlanFailure,
        )
    }

    @Test
    fun `dispatch billing result failure`() = runTest {
        dataSource.billingFlowResultCode = PaymentResultCode.DeveloperError

        client.purchaseSubscriptionPlan(planKey)

        listener.assertEvents(
            Event.PurchaseSubscriptionPlan,
            Event.PurchaseSubscriptionPlanFailure,
        )
    }

    @Test
    fun `dispatch confirm pruchase result failure`() = runTest {
        approver.approveResultCode = PaymentResultCode.DeveloperError

        client.purchaseSubscriptionPlan(planKey)

        listener.assertEvents(
            Event.PurchaseSubscriptionPlan,
            Event.ConfirmPurchase,
            Event.ConfirmPurchaseFailure,
            Event.PurchaseSubscriptionPlanFailure,
        )
    }

    private suspend fun PaymentClient.purchaseSubscriptionPlan(key: SubscriptionPlan.Key): PurchaseResult {
        return purchaseSubscriptionPlan(key, purchaseSource = "source", mock<Activity>())
    }
}
