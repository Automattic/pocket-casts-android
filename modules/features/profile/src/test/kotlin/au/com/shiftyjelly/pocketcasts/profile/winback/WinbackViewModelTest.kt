package au.com.shiftyjelly.pocketcasts.profile.winback

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.Tracker
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchasesState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.PricingPhases
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

class WinbackViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val subscriptionManager = mock<SubscriptionManager>()
    private val purchaseEvents = MutableSharedFlow<PurchaseEvent>()
    private val purchaseEventsFlowable = purchaseEvents.asFlowable()

    private val settings = mock<Settings>()

    private val tracker = FakeTracker()

    @Before
    fun setUp() {
        whenever(subscriptionManager.observePurchaseEvents()) doReturn purchaseEventsFlowable
        val subscriptionSettingMock = mock<UserSetting<SubscriptionStatus?>> {
            on { flow } doReturn MutableStateFlow(null)
        }
        whenever(settings.cachedSubscriptionStatus) doReturn subscriptionSettingMock
    }

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

    private val knownPlan = SubscriptionPlan(
        productId = Subscription.PLUS_YEARLY_PRODUCT_ID,
        offerToken = "token",
        title = "title",
        formattedPrice = "price",
        billingPeriod = BillingPeriod.Yearly,
    )

    @Test
    fun `subscription plans for user with active subscription`() = runTest {
        val purchases = listOf(createPurchase())

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitLoadedState()

            val plusMonthly = state[Subscription.PLUS_MONTHLY_PRODUCT_ID]
            val patronMonthly = state[Subscription.PATRON_MONTHLY_PRODUCT_ID]
            val plusYearly = state[Subscription.PLUS_YEARLY_PRODUCT_ID]
            val patronYearly = state[Subscription.PATRON_YEARLY_PRODUCT_ID]

            assertNotNull(plusMonthly?.productId)
            assertNotNull(patronMonthly?.productId)
            assertNotNull(plusYearly?.productId)
            assertNotNull(patronYearly?.productId)

            assertEquals(state.activePurchase.productId, Subscription.PLUS_MONTHLY_PRODUCT_ID)
        }
    }

    @Test
    fun `subscription plans for user with unacknowledged purchase`() = runTest {
        val purchases = listOf(createPurchase(isAcknowledged = false))

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoPurchases, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with not auto-renewing purchase`() = runTest {
        val purchases = listOf(createPurchase(isAutoRenewing = false))

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoPurchases, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with purchase without order ID`() = runTest {
        val purchases = listOf(createPurchase(orderId = null))

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoOrderId, availablePlans.reason)
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

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.TooManyPurchases, availablePlans.reason)
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

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.TooManyProducts, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with purchase with no products`() = runTest {
        val purchases = listOf(createPurchase(productIds = emptyList()))

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoProducts, availablePlans.reason)
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

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val subscriptionPlansState = awaitLoadedState()

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

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val subscriptionPlansState = awaitLoadedState()

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

    @Test
    fun `change subscription plan successfully`() = runTest {
        val purchases = listOf(createPurchase())

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)
        wheneverBlocking { subscriptionManager.changeProduct(any(), any(), any(), any(), any()) } doReturn true

        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            viewModel.changePlan(mock(), knownPlan)
            assertTrue(awaitLoadedState().isChangingPlan)

            val newPurchase = createPurchase(orderId = "new-purchase")
            wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(listOf(newPurchase))

            purchaseEvents.emit(PurchaseEvent.Success)
            val state = awaitLoadedState()
            assertFalse(state.isChangingPlan)
            assertEquals(state.activePurchase, ActivePurchase(newPurchase.orderId!!, newPurchase.products[0]))
        }
    }

    @Test
    fun `change subscription when current state is not loaded`() = runTest {
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(emptyList())

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1)

            viewModel.changePlan(mock(), knownPlan)
            expectNoEvents()
        }
    }

    @Test
    fun `change subscription when there is no matching product`() = runTest {
        val purchases = listOf(createPurchase())

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1)

            viewModel.changePlan(mock(), knownPlan.copy(productId = "unknown"))
            expectNoEvents()
        }
    }

    @Test
    fun `change subscription when there billing flow fails to start`() = runTest {
        val purchases = listOf(createPurchase())

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)
        wheneverBlocking { subscriptionManager.changeProduct(any(), any(), any(), any(), any()) } doReturn false

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1)

            viewModel.changePlan(mock(), knownPlan.copy(productId = "unknown"))
            expectNoEvents()
        }
    }

    @Test
    fun `change subscription when it is cancelled`() = runTest {
        val purchases = listOf(createPurchase())

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)
        wheneverBlocking { subscriptionManager.changeProduct(any(), any(), any(), any(), any()) } doReturn true

        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            viewModel.changePlan(mock(), knownPlan)
            assertTrue(awaitLoadedState().isChangingPlan)

            purchaseEvents.emit(PurchaseEvent.Cancelled(0))
            val state = awaitLoadedState()
            assertFalse(state.isChangingPlan)
            assertFalse(state.hasPlanChangeFailed)
        }
    }

    @Test
    fun `change subscription when purchase fails`() = runTest {
        val purchases = listOf(createPurchase())

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)
        wheneverBlocking { subscriptionManager.changeProduct(any(), any(), any(), any(), any()) } doReturn true

        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            viewModel.changePlan(mock(), knownPlan)
            assertTrue(awaitLoadedState().isChangingPlan)

            purchaseEvents.emit(PurchaseEvent.Failure("", 0))
            val state = awaitLoadedState()
            assertFalse(state.isChangingPlan)
            assertTrue(state.hasPlanChangeFailed)
        }
    }

    @Test
    fun `change subscription when new plans fail to load`() = runTest {
        val purchases = listOf(createPurchase())

        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)
        wheneverBlocking { subscriptionManager.changeProduct(any(), any(), any(), any(), any()) } doReturn true

        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            viewModel.changePlan(mock(), knownPlan)
            assertTrue(awaitLoadedState().isChangingPlan)

            wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(emptyList())
            purchaseEvents.emit(PurchaseEvent.Success)

            val state = awaitItem().subscriptionPlansState
            assertTrue(state is SubscriptionPlansState.Failure)
        }
    }

    @Test
    fun `track screen shown`() = runTest {
        val purchases = listOf(createPurchase())
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackScreenShown("screen_key")

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(
                AnalyticsEvent.WINBACK_SCREEN_SHOWN,
                mapOf("screen" to "screen_key"),
            ),
            event,
        )
    }

    @Test
    fun `track screen dismissed`() = runTest {
        val purchases = listOf(createPurchase())
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackScreenDismissed("screen_key")

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(
                AnalyticsEvent.WINBACK_SCREEN_DISMISSED,
                mapOf("screen" to "screen_key"),
            ),
            event,
        )
    }

    @Test
    fun `track continue cancellation tapped`() = runTest {
        val purchases = listOf(createPurchase())
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackContinueCancellationTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_CONTINUE_BUTTON_TAP, emptyMap()),
            event,
        )
    }

    @Test
    fun `track claim plus monthly offer tapped`() = runTest {
        val purchases = listOf(createPurchase(productIds = listOf(Subscription.PLUS_MONTHLY_PRODUCT_ID)))
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackClaimOfferTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(
                AnalyticsEvent.WINBACK_MAIN_SCREEN_ROW_TAP,
                mapOf(
                    "row" to "claim_offer",
                    "tier" to "plus",
                    "frequency" to "monthly",
                ),
            ),
            event,
        )
    }

    @Test
    fun `track claim plus yearly offer tapped`() = runTest {
        val purchases = listOf(createPurchase(productIds = listOf(Subscription.PLUS_YEARLY_PRODUCT_ID)))
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackClaimOfferTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(
                AnalyticsEvent.WINBACK_MAIN_SCREEN_ROW_TAP,
                mapOf(
                    "row" to "claim_offer",
                    "tier" to "plus",
                    "frequency" to "yearly",
                ),
            ),
            event,
        )
    }

    @Test
    fun `track claim patron monthly offer tapped`() = runTest {
        val purchases = listOf(createPurchase(productIds = listOf(Subscription.PATRON_MONTHLY_PRODUCT_ID)))
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackClaimOfferTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(
                AnalyticsEvent.WINBACK_MAIN_SCREEN_ROW_TAP,
                mapOf(
                    "row" to "claim_offer",
                    "tier" to "patron",
                    "frequency" to "monthly",
                ),
            ),
            event,
        )
    }

    @Test
    fun `track claim patron yearly offer tapped`() = runTest {
        val purchases = listOf(createPurchase(productIds = listOf(Subscription.PATRON_YEARLY_PRODUCT_ID)))
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackClaimOfferTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(
                AnalyticsEvent.WINBACK_MAIN_SCREEN_ROW_TAP,
                mapOf(
                    "row" to "claim_offer",
                    "tier" to "patron",
                    "frequency" to "yearly",
                ),
            ),
            event,
        )
    }

    @Test
    fun `track available plans tapped`() = runTest {
        val purchases = listOf(createPurchase())
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackAvailablePlansTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(
                AnalyticsEvent.WINBACK_MAIN_SCREEN_ROW_TAP,
                mapOf("row" to "available_plans"),
            ),
            event,
        )
    }

    @Test
    fun `track help and feedback tapped`() = runTest {
        val purchases = listOf(createPurchase())
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackHelpAndFeedbackTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(
                AnalyticsEvent.WINBACK_MAIN_SCREEN_ROW_TAP,
                mapOf("row" to "help_and_feedback"),
            ),
            event,
        )
    }

    @Test
    fun `track offer claimed confirmation tapped`() = runTest {
        val purchases = listOf(createPurchase())
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackOfferClaimedConfirmationTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_OFFER_CLAIMED_DONE_BUTTON_TAPPED, emptyMap()),
            event,
        )
    }

    @Test
    fun `track plans back button tapped`() = runTest {
        val purchases = listOf(createPurchase())
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackPlansBackButtonTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_AVAILABLE_PLANS_BACK_BUTTON_TAPPED, emptyMap()),
            event,
        )
    }

    @Test
    fun `track plan change`() = runTest {
        val purchases = listOf(createPurchase(productIds = listOf(Subscription.PLUS_MONTHLY_PRODUCT_ID)))
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)
        wheneverBlocking { subscriptionManager.changeProduct(any(), any(), any(), any(), any()) } doReturn true

        val viewModel = createViewModel()
        viewModel.changePlan(mock(), knownPlan)
        purchaseEvents.emit(PurchaseEvent.Success)

        assertEquals(
            listOf(
                TrackEvent(
                    AnalyticsEvent.WINBACK_AVAILABLE_PLANS_SELECT_PLAN,
                    mapOf(
                        "product" to knownPlan.productId,
                    ),
                ),
                TrackEvent(
                    AnalyticsEvent.WINBACK_AVAILABLE_PLANS_NEW_PLAN_PURCHASE_SUCCESSFUL,
                    mapOf(
                        "current_product" to Subscription.PLUS_MONTHLY_PRODUCT_ID,
                        "new_product" to knownPlan.productId,
                    ),
                ),
            ),
            tracker.events,
        )
    }

    @Test
    fun `track keep subscription tapped`() = runTest {
        val purchases = listOf(createPurchase())
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackKeepSubscriptionTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_CANCEL_CONFIRMATION_STAY_BUTTON_TAPPED, emptyMap()),
            event,
        )
    }

    @Test
    fun `track cancel subscription tapped`() = runTest {
        val purchases = listOf(createPurchase())
        wheneverBlocking { subscriptionManager.loadProducts() } doReturn ProductDetailsState.Loaded(products)
        wheneverBlocking { subscriptionManager.loadPurchases() } doReturn PurchasesState.Loaded(purchases)

        val viewModel = createViewModel()
        viewModel.trackCancelSubscriptionTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_CANCEL_CONFIRMATION_CANCEL_BUTTON_TAPPED, emptyMap()),
            event,
        )
    }

    private fun createViewModel() = WinbackViewModel(
        subscriptionManager,
        settings,
        AnalyticsTracker.test(tracker, isEnabled = true),
    )
}

private suspend fun TurbineTestContext<WinbackViewModel.UiState>.awaitLoadedState(): SubscriptionPlansState.Loaded {
    return awaitItem().subscriptionPlansState as SubscriptionPlansState.Loaded
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
    orderId: String? = "orderId",
    productIds: List<String> = listOf(Subscription.PLUS_MONTHLY_PRODUCT_ID),
    isAcknowledged: Boolean = true,
    isAutoRenewing: Boolean = true,
) = mock<Purchase> {
    on { this.orderId } doReturn orderId
    on { this.products } doReturn productIds
    on { this.isAcknowledged } doReturn isAcknowledged
    on { this.isAutoRenewing } doReturn isAutoRenewing
}

private val BillingPeriod.value
    get() = when (this) {
        BillingPeriod.Monthly -> "P1M"
        BillingPeriod.Yearly -> "P1Y"
    }

class FakeTracker : Tracker {
    private val _events = mutableListOf<TrackEvent>()

    val events get() = _events.toList()

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
        _events += TrackEvent(event, properties)
    }

    override fun refreshMetadata() = Unit

    override fun flush() = Unit

    override fun clearAllData() = Unit
}

data class TrackEvent(
    val type: AnalyticsEvent,
    val properties: Map<String, Any>,
)
