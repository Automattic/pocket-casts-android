package au.com.shiftyjelly.pocketcasts.payment

import android.app.Activity
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
    private val logger = TestLogger()

    private val client = PaymentClient(dataSource, approver, logger)

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
                productIds = listOf(SubscriptionPlan.PlusMonthlyProductId),
                isAcknowledged = true,
                isAutoRenewing = true,
            ),
            purchase.copy(
                state = PurchaseState.Purchased("order-id-2"),
                productIds = listOf(SubscriptionPlan.PlusYearlyProductId),
                isAcknowledged = true,
                isAutoRenewing = true,
            ),
            purchase.copy(
                state = PurchaseState.Purchased("order-id-3"),
                productIds = listOf(SubscriptionPlan.PatronMonthlyProductId),
                isAcknowledged = true,
                isAutoRenewing = true,
            ),
            purchase.copy(
                state = PurchaseState.Purchased("order-id-4"),
                productIds = listOf(SubscriptionPlan.PatronYearlyProductId),
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
                    SubscriptionPlan.PlusMonthlyProductId,
                    SubscriptionPlan.PlusYearlyProductId,
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
                productIds = listOf(SubscriptionPlan.PlusMonthlyProductId),
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
        val purchaseResult = client.purchaseSubscriptionPlan(planKey, mock<Activity>())

        assertEquals(PurchaseResult.Purchased, purchaseResult)
    }

    @Test
    fun `do not purchase subscription when billing result is failure`() = runTest {
        dataSource.billingFlowResultCode = PaymentResultCode.FeatureNotSupported

        val purchaseResult = client.purchaseSubscriptionPlan(planKey, mock<Activity>())

        assertEquals(PurchaseResult.Failure(PaymentResultCode.FeatureNotSupported), purchaseResult)
    }

    @Test
    fun `do not purchase subscription when purchase result is failure`() = runTest {
        dataSource.purchasedProductsResultCode = PaymentResultCode.Error

        val purchaseResult = client.purchaseSubscriptionPlan(planKey, mock<Activity>())

        assertEquals(PurchaseResult.Failure(PaymentResultCode.Error), purchaseResult)
    }

    @Test
    fun `cancel purchase subscription when purchase result is cancelled`() = runTest {
        dataSource.purchasedProductsResultCode = PaymentResultCode.UserCancelled

        val purchaseResult = client.purchaseSubscriptionPlan(planKey, mock<Activity>())

        assertEquals(PurchaseResult.Cancelled, purchaseResult)
    }

    @Test
    fun `do not purchase subscription when approving fails`() = runTest {
        approver.approveResultCode = PaymentResultCode.Error

        val purchaseResult = client.purchaseSubscriptionPlan(planKey, mock<Activity>())

        assertEquals(PurchaseResult.Failure(PaymentResultCode.Error), purchaseResult)
    }

    @Test
    fun `do not purchase subscription when acknowledging fails`() = runTest {
        dataSource.acknowledgePurchaseResultCode = PaymentResultCode.Error

        val purchaseResult = client.purchaseSubscriptionPlan(planKey, mock<Activity>())

        assertEquals(PurchaseResult.Failure(PaymentResultCode.Error), purchaseResult)
    }

    @Test
    fun `cancel purchase subscription when acknowledging is cancelled`() = runTest {
        dataSource.acknowledgePurchaseResultCode = PaymentResultCode.UserCancelled

        val purchaseResult = client.purchaseSubscriptionPlan(planKey, mock<Activity>())

        assertEquals(PurchaseResult.Cancelled, purchaseResult)
    }

    @Test
    fun `do not finalize non confirmed purchases`() = runTest {
        dataSource.purchasedProducts = listOf(
            purchase.copy(state = PurchaseState.Pending),
        )

        val purchaseResult = backgroundScope.async { client.purchaseSubscriptionPlan(planKey, mock<Activity>()) }
        yield() // Yield to make sure the job didn't cancel

        assertTrue(purchaseResult.isActive)
        assertTrue(approver.receivedPurchases.isEmpty())
    }

    @Test
    fun `do not acknowledge purchases that are already acknowledged`() = runTest {
        dataSource.purchasedProducts = listOf(
            purchase.copy(isAcknowledged = true),
        )

        val purchaseResult = client.purchaseSubscriptionPlan(planKey, mock<Activity>())

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
    fun `log loading plans successfully`() = runTest {
        val plans = client.loadSubscriptionPlans()

        logger.assertInfos(
            "Load subscription plans",
            "Subscription plans loaded: ${plans.getOrNull()}",
        )
    }

    @Test
    fun `log loading plans with failure`() = runTest {
        dataSource.loadedProductsResultCode = PaymentResultCode.Error

        client.loadSubscriptionPlans()

        logger.assertInfos("Load subscription plans")
        logger.assertWarnings("Failed to load subscription plans. Error, Load products error")
    }

    @Test
    fun `log loading acknowledged subscription purchases succesfully`() = runTest {
        val subscriptions = client.loadAcknowledgedSubscriptions().getOrNull()!!

        logger.assertInfos(
            "Loading acknowledged subscriptions",
            "Acknowledged subscriptions loaded: $subscriptions",
        )
    }

    @Test
    fun `log loading acknowledged subscription with failure`() = runTest {
        dataSource.loadedPurchasesResultCode = PaymentResultCode.Error

        client.loadAcknowledgedSubscriptions()

        logger.assertInfos("Loading acknowledged subscriptions")
        logger.assertWarnings("Failed to load acknowledged subscriptions. Error, Load purchases error")
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

        logger.assertWarnings(
            "Skipping purchase order-id-2. No associated products.",
            "Skipping purchase order-id-3. Too many associated products: [product-id-1, product-id-2]",
            "Skipping purchase order-id-4. Couldn't find matching product key for unknown-product-id",
        )
    }

    @Test
    fun `log confirming purchase`() = runTest {
        dataSource.purchasedProducts = listOf(purchase)

        client.purchaseSubscriptionPlan(planKey, mock<Activity>())

        logger.assertInfos(
            "Confirm purchase: $purchase",
            "Purchase confirmed: ${purchase.copy(isAcknowledged = true)}",
        )
    }

    @Test
    fun `log confirming purchase failure`() = runTest {
        dataSource.purchasedProducts = listOf(purchase)
        approver.approveResultCode = PaymentResultCode.DeveloperError

        client.purchaseSubscriptionPlan(planKey, mock<Activity>())

        logger.assertInfos(
            "Confirm purchase: $purchase",
        )
        logger.assertWarnings(
            "Failed to confirm purchase: $purchase. DeveloperError, Error message",
        )
    }

    @Test
    fun `log purchase result failure`() = runTest {
        dataSource.purchasedProductsResultCode = PaymentResultCode.ServiceDisconnected

        client.purchaseSubscriptionPlan(planKey, mock<Activity>())

        logger.assertWarnings(
            "Purchase failure: ServiceDisconnected, Purchase product error",
        )
    }

    @Test
    fun `log billing result failure`() = runTest {
        dataSource.billingFlowResultCode = PaymentResultCode.DeveloperError

        client.purchaseSubscriptionPlan(planKey, mock<Activity>())

        logger.assertWarnings(
            "Launching billing flow failed: DeveloperError, Launch billing error",
        )
    }
}
