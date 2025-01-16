package au.com.shiftyjelly.pocketcasts.profile.winback

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchasesState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.PricingPhases
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.wheneverBlocking

class WinbackViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val subscriptionManager = mock<SubscriptionManager>()

    private val products = listOf(
        createProductDetails(
            id = Subscription.PLUS_MONTHLY_PRODUCT_ID,
            period = BillingPeriod.Monthly,
        ),
        createProductDetails(
            id = Subscription.PATRON_MONTHLY_PRODUCT_ID,
            period = BillingPeriod.Monthly,
        ),
        createProductDetails(
            id = Subscription.PLUS_YEARLY_PRODUCT_ID,
            period = BillingPeriod.Yearly,
        ),
        createProductDetails(
            id = Subscription.PATRON_YEARLY_PRODUCT_ID,
            period = BillingPeriod.Yearly,
        ),
    )

    @Test
    fun `subscription plans for user with active subscription`() = runTest {
        val purchases = listOf(createPurchase())

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = WinbackViewModel(subscriptionManager)

        viewModel.uiState.test {
            val state = awaitItem()
            val plans = state.subscriptionPlansState as SubscriptionPlansState.Loaded

            val plusMonthly = plans[Subscription.PLUS_MONTHLY_PRODUCT_ID]
            val patronMonthly = plans[Subscription.PATRON_MONTHLY_PRODUCT_ID]
            val plusYearly = plans[Subscription.PLUS_YEARLY_PRODUCT_ID]
            val patronYearly = plans[Subscription.PATRON_YEARLY_PRODUCT_ID]

            assertNotNull(plusMonthly?.productId)
            assertNotNull(patronMonthly?.productId)
            assertNotNull(plusYearly?.productId)
            assertNotNull(patronYearly?.productId)

            assertEquals(plans.activePurchase.productId, Subscription.PLUS_MONTHLY_PRODUCT_ID)
        }
    }

    @Test
    fun `subscription plans for user with unacknowledged purchase`() = runTest {
        val purchases = listOf(createPurchase(isAcknowledged = false))

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = WinbackViewModel(subscriptionManager)

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState

            assertTrue(availablePlans is SubscriptionPlansState.Failure)
        }
    }

    @Test
    fun `subscription plans for user with purchase without order ID`() = runTest {
        val purchases = listOf(createPurchase(orderId = null))

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = WinbackViewModel(subscriptionManager)

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState

            assertTrue(availablePlans is SubscriptionPlansState.Failure)
        }
    }

    @Test
    fun `subscription plans for user with multiple purchases`() = runTest {
        val purchases = listOf(
            createPurchase(orderId = "1"),
            createPurchase(orderId = "2"),
        )

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = WinbackViewModel(subscriptionManager)

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState

            assertTrue(availablePlans is SubscriptionPlansState.Failure)
        }
    }

    @Test
    fun `subscription plans for user with purchase with multiple products`() = runTest {
        val purchases = listOf(
            createPurchase(
                productIds = listOf(
                    Subscription.PLUS_MONTHLY_PRODUCT_ID,
                    Subscription.PLUS_YEARLY_PRODUCT_ID,
                ),
            ),
        )

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = WinbackViewModel(subscriptionManager)

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState

            assertTrue(availablePlans is SubscriptionPlansState.Failure)
        }
    }

    @Test
    fun `subscription plans use only base offer from products`() = runTest {
        val purchases = listOf(createPurchase())
        val products = listOf(
            createProductDetails(
                id = Subscription.PLUS_MONTHLY_PRODUCT_ID,
                period = BillingPeriod.Monthly,
                offer = Offer(
                    id = "offer-id",
                    billingPeriod = BillingPeriod.Monthly,
                ),
            ),
        )

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = WinbackViewModel(subscriptionManager)

        viewModel.uiState.test {
            val subscriptionPlansState = awaitItem().subscriptionPlansState as SubscriptionPlansState.Loaded

            val plan = subscriptionPlansState[Subscription.PLUS_MONTHLY_PRODUCT_ID]
            assertEquals(Subscription.PLUS_MONTHLY_PRODUCT_ID, plan?.offerToken)
        }
    }

    @Test
    fun `subscription plans are sorted`() = runTest {
        val purchases = listOf(createPurchase())
        val products = products.reversed()

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = WinbackViewModel(subscriptionManager)

        viewModel.uiState.test {
            val subscriptionPlansState = awaitItem().subscriptionPlansState as SubscriptionPlansState.Loaded

            val tokens = subscriptionPlansState.plans.map(SubscriptionPlan::productId)
            val expected = listOf(
                Subscription.PLUS_MONTHLY_PRODUCT_ID,
                Subscription.PATRON_MONTHLY_PRODUCT_ID,
                Subscription.PLUS_YEARLY_PRODUCT_ID,
                Subscription.PATRON_YEARLY_PRODUCT_ID,
            )
            assertEquals(expected, tokens)
        }
    }
}

private operator fun SubscriptionPlansState.Loaded.get(productId: String) =
    plans.singleOrNull { it.productId == productId }

private class Offer(
    val id: String,
    val billingPeriod: BillingPeriod,
)

private fun createProductDetails(
    id: String,
    period: BillingPeriod,
    offer: Offer? = null,
) = mock<ProductDetails> {
    check(id != offer?.id) { "ID and offer ID must be different" }

    val basePricingPhase = mock<PricingPhase> {
        on { formattedPrice } doReturn "price: $id"
        on { billingPeriod } doReturn period.value
        on { recurrenceMode } doReturn RecurrenceMode.INFINITE_RECURRING
    }
    val offerPricingPhase = offer?.let {
        mock<PricingPhase> {
            on { billingPeriod } doReturn offer.billingPeriod.value
            on { recurrenceMode } doReturn RecurrenceMode.INFINITE_RECURRING
        }
    }
    val basePricingPhases = mock<PricingPhases> {
        on { pricingPhaseList } doReturn listOf(basePricingPhase)
    }
    val offserPricingPhases = offerPricingPhase?.let {
        mock<PricingPhases> {
            on { pricingPhaseList } doReturn listOf(
                offerPricingPhase,
                basePricingPhase,
            )
        }
    }
    val offerDetails = buildList {
        if (offserPricingPhases != null) {
            add(
                mock<SubscriptionOfferDetails> {
                    on { offerId } doReturn offer.id
                    on { offerToken } doReturn offer.id
                    on { pricingPhases } doReturn offserPricingPhases
                },
            )
        }
        add(
            mock<SubscriptionOfferDetails> {
                on { offerToken } doReturn id
                on { pricingPhases } doReturn basePricingPhases
            },
        )
    }

    on { this.productId } doReturn id
    on { this.title } doReturn "title: $id"
    on { subscriptionOfferDetails } doReturn offerDetails
}

private fun createPurchase(
    isAcknowledged: Boolean = true,
    productIds: List<String> = listOf(Subscription.PLUS_MONTHLY_PRODUCT_ID),
    orderId: String? = "orderId",
) = mock<Purchase> {
    on { this.isAcknowledged } doReturn isAcknowledged
    on { this.orderId } doReturn orderId
    on { this.products } doReturn productIds
}

private val BillingPeriod.value
    get() = when (this) {
        BillingPeriod.Monthly -> "P1M"
        BillingPeriod.Yearly -> "P1Y"
    }
