package au.com.shiftyjelly.pocketcasts.profile.winback

import android.app.Activity
import app.cash.turbine.Turbine
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.Tracker
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.BillingPeriod
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.WinbackOfferDetails
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchaseEvent
import au.com.shiftyjelly.pocketcasts.repositories.subscription.PurchasesState
import au.com.shiftyjelly.pocketcasts.repositories.winback.WinbackManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.PricingPhases
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.android.billingclient.api.Purchase
import com.pocketcasts.service.api.WinbackResponse
import com.pocketcasts.service.api.winbackResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WinbackViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val winbackManager = FakeWinbackManager()
    private val tracker = FakeTracker()
    private val settings = mock<Settings>()

    private val products = WinbackOfferDetails.entries.map { it.toProductDetails() }

    private val purchase = createPurchase()

    private val knownPlan = SubscriptionPlan(
        productId = Subscription.PLUS_YEARLY_PRODUCT_ID,
        offerToken = "token",
        title = "title",
        formattedPrice = "price",
        billingPeriod = BillingPeriod.Yearly,
    )

    private val winbackResponse = winbackResponse {
        offer = WinbackOfferDetails.PlusMonthly.offerId
        code = "ABC"
    }

    private val winbackOffer = WinbackOffer(
        details = WinbackOfferDetails.PlusMonthly,
        offerToken = "offer-token-${WinbackOfferDetails.PlusMonthly.productId}",
        redeemCode = "ABC",
        formattedPrice = "formated-price",
    )

    private lateinit var viewModel: WinbackViewModel

    @Before
    fun setUp() {
        val subscriptionSettingMock = mock<UserSetting<SubscriptionStatus?>> {
            on { flow } doReturn MutableStateFlow(null)
        }
        whenever(settings.cachedSubscriptionStatus) doReturn subscriptionSettingMock

        viewModel = WinbackViewModel(
            winbackManager,
            settings,
            AnalyticsTracker.test(tracker, isEnabled = true),
        )
    }

    @Test
    fun `initial plans state is loading`() {
        val state = viewModel.uiState.value

        assertEquals(SubscriptionPlansState.Loading, state.subscriptionPlansState)
    }

    @Test
    fun `subscription plans for user with active subscription`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

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
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(createPurchase(isAcknowledged = false))
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoPurchases, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with not auto-renewing purchase`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(createPurchase(isAutoRenewing = false))
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoPurchases, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with purchase without order ID`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(createPurchase(orderId = null))
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoOrderId, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with multiple purchases`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchases(listOf(createPurchase(orderId = "1"), createPurchase(orderId = "2")))
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.TooManyPurchases, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with purchase with multiple products`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(createPurchase(productIds = listOf("id1", "id2")))
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.TooManyProducts, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with purchase with no products`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(createPurchase(productIds = emptyList()))
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoProducts, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans use only base offer from products`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            val subscriptionPlansState = awaitLoadedState()

            val plan = subscriptionPlansState[Subscription.PLUS_MONTHLY_PRODUCT_ID]
            assertEquals("base-token-${Subscription.PLUS_MONTHLY_PRODUCT_ID}", plan?.offerToken)
        }
    }

    @Test
    fun `subscription plans are sorted`() = runTest {
        winbackManager.addProductDetails(products.reversed())
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

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
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            viewModel.changePlan(knownPlan, mock())
            assertTrue(awaitLoadedState().isChangingPlan)

            val newPurchase = createPurchase(orderId = "new-purchase")
            winbackManager.addPurchases(listOf(newPurchase))
            winbackManager.addPurchaseEvent(PurchaseEvent.Success)
            val state = awaitLoadedState()

            assertFalse(state.isChangingPlan)
            assertEquals(state.activePurchase, ActivePurchase(newPurchase.orderId!!, newPurchase.products[0]))
        }
    }

    @Test
    fun `change subscription when current state is not loaded`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchases(emptyList())
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            skipItems(1)

            viewModel.changePlan(knownPlan, mock())
            expectNoEvents()
        }
    }

    @Test
    fun `change subscription when there is no matching product`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            skipItems(1)

            viewModel.changePlan(knownPlan.copy(productId = "unknown"), mock())
            expectNoEvents()
        }
    }

    @Test
    fun `change subscription when it is cancelled`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            viewModel.changePlan(knownPlan, mock())
            assertTrue(awaitLoadedState().isChangingPlan)

            winbackManager.addPurchaseEvent(PurchaseEvent.Cancelled(0))
            val state = awaitLoadedState()

            assertFalse(state.isChangingPlan)
            assertFalse(state.hasPlanChangeFailed)
        }
    }

    @Test
    fun `change subscription when purchase fails`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            viewModel.changePlan(knownPlan, mock())
            assertTrue(awaitLoadedState().isChangingPlan)

            winbackManager.addPurchaseEvent(PurchaseEvent.Failure("", 0))
            val state = awaitLoadedState()

            assertFalse(state.isChangingPlan)
            assertTrue(state.hasPlanChangeFailed)
        }
    }

    @Test
    fun `change subscription when new plans fail to load`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            viewModel.changePlan(knownPlan, mock())
            assertTrue(awaitLoadedState().isChangingPlan)

            winbackManager.addPurchases(emptyList())
            winbackManager.addPurchaseEvent(PurchaseEvent.Success)

            val state = awaitItem().subscriptionPlansState
            assertTrue(state is SubscriptionPlansState.Failure)
        }
    }

    @Test
    fun `plus monthly winback offer`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(
            winbackResponse {
                offer = WinbackOfferDetails.PlusMonthly.offerId
                code = "ABC"
            },
        )

        viewModel.uiState.test {
            assertEquals(
                WinbackOffer(
                    redeemCode = "ABC",
                    details = WinbackOfferDetails.PlusMonthly,
                    offerToken = "offer-token-${Subscription.PLUS_MONTHLY_PRODUCT_ID}",
                    formattedPrice = "base-price-${Subscription.PLUS_MONTHLY_PRODUCT_ID}",
                ),
                awaitItem().winbackOfferState?.offer,
            )
        }
    }

    @Test
    fun `plus yearly winback offer`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(
            winbackResponse {
                offer = WinbackOfferDetails.PlusYearly.offerId
                code = "ABC"
            },
        )

        viewModel.uiState.test {
            assertEquals(
                WinbackOffer(
                    redeemCode = "ABC",
                    details = WinbackOfferDetails.PlusYearly,
                    offerToken = "offer-token-${Subscription.PLUS_YEARLY_PRODUCT_ID}",
                    formattedPrice = "offer-price-${Subscription.PLUS_YEARLY_PRODUCT_ID}",
                ),
                awaitItem().winbackOfferState?.offer,
            )
        }
    }

    @Test
    fun `patron monthly winback offer`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(
            winbackResponse {
                offer = WinbackOfferDetails.PatronMonthly.offerId
                code = "ABC"
            },
        )

        viewModel.uiState.test {
            assertEquals(
                WinbackOffer(
                    redeemCode = "ABC",
                    details = WinbackOfferDetails.PatronMonthly,
                    offerToken = "offer-token-${Subscription.PATRON_MONTHLY_PRODUCT_ID}",
                    formattedPrice = "base-price-${Subscription.PATRON_MONTHLY_PRODUCT_ID}",
                ),
                awaitItem().winbackOfferState?.offer,
            )
        }
    }

    @Test
    fun `patron yearly winback offer`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(
            winbackResponse {
                offer = WinbackOfferDetails.PatronYearly.offerId
                code = "ABC"
            },
        )

        viewModel.uiState.test {
            assertEquals(
                WinbackOffer(
                    redeemCode = "ABC",
                    details = WinbackOfferDetails.PatronYearly,
                    offerToken = "offer-token-${Subscription.PATRON_YEARLY_PRODUCT_ID}",
                    formattedPrice = "offer-price-${Subscription.PATRON_YEARLY_PRODUCT_ID}",
                ),
                awaitItem().winbackOfferState?.offer,
            )
        }
    }

    @Test
    fun `no winback offer`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `winback offer with blank offer ID`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(
            winbackResponse {
                offer = " "
                code = "ABC"
            },
        )

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `winback offer with blank redeem code`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(
            winbackResponse {
                offer = WinbackOfferDetails.PlusMonthly.offerId
                code = " "
            },
        )

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `winback offer with unknown ID`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(
            winbackResponse {
                offer = "unknown"
                code = "ABC"
            },
        )

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `winback offer with no matching product ID`() = runTest {
        winbackManager.addProductDetails(products.filter { it.productId != Subscription.PLUS_MONTHLY_PRODUCT_ID })
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(
            winbackResponse {
                offer = WinbackOfferDetails.PlusMonthly.offerId
                code = "ABC"
            },
        )

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `winback offer with no matching product offer ID`() = runTest {
        winbackManager.addProductDetails(
            WinbackOfferDetails.PlusMonthly.toProductDetails(bonusOfferId = "offer-id"),
        )
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(
            winbackResponse {
                offer = WinbackOfferDetails.PlusMonthly.offerId
                code = "ABC"
            },
        )

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `winback offer with too few pricing phases`() = runTest {
        winbackManager.addProductDetails(
            WinbackOfferDetails.PlusMonthly.toProductDetails(
                customPricingPhases = List(1) {
                    mock<PricingPhase>() {
                        on { formattedPrice } doReturn "price"
                        on { billingPeriod } doReturn BillingPeriod.Monthly.value
                        on { recurrenceMode } doReturn RecurrenceMode.INFINITE_RECURRING
                    }
                },
            ),
        )
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(
            winbackResponse {
                offer = WinbackOfferDetails.PlusMonthly.offerId
                code = "ABC"
            },
        )

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `winback offer with too many pricing phases`() = runTest {
        winbackManager.addProductDetails(
            WinbackOfferDetails.PlusMonthly.toProductDetails(
                customPricingPhases = List(3) {
                    mock<PricingPhase>() {
                        on { formattedPrice } doReturn "price"
                        on { billingPeriod } doReturn BillingPeriod.Monthly.value
                        on { recurrenceMode } doReturn RecurrenceMode.INFINITE_RECURRING
                    }
                },
            ),
        )
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(
            winbackResponse {
                offer = WinbackOfferDetails.PlusMonthly.offerId
                code = "ABC"
            },
        )

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `claim winback offer successfully`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(winbackResponse)

        viewModel.uiState.test {
            val initialState = awaitOfferState()
            assertFalse(initialState.isClaimingOffer)
            assertFalse(initialState.isOfferClaimed)
            assertFalse(initialState.hasOfferClaimFailed)

            viewModel.claimOffer(winbackOffer, mock())
            val claimingState = awaitOfferState()
            assertTrue(claimingState.isClaimingOffer)
            assertFalse(claimingState.isOfferClaimed)
            assertFalse(claimingState.hasOfferClaimFailed)

            winbackManager.addPurchaseEvent(PurchaseEvent.Success)
            val claimedState = awaitOfferState()
            assertFalse(claimedState.isClaimingOffer)
            assertTrue(claimedState.isOfferClaimed)
            assertFalse(claimedState.hasOfferClaimFailed)

            viewModel.consumeClaimedOffer()
            assertFalse(awaitOfferState().isOfferClaimed)
        }
    }

    @Test
    fun `claim winback offer when current state is not loaded`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchases(emptyList())
        winbackManager.addWinbackResponse(winbackResponse)

        viewModel.uiState.test {
            skipItems(1)

            viewModel.claimOffer(winbackOffer, mock())
            expectNoEvents()
        }
    }

    @Test
    fun `claim winback offer when there is no matching product`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(winbackResponse)

        viewModel.uiState.test {
            skipItems(1)

            viewModel.claimOffer(winbackOffer.copy(offerToken = "unknown"), mock())
            expectNoEvents()
        }
    }

    @Test
    fun `claim winback offer when it is cancelled`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(winbackResponse)

        viewModel.uiState.test {
            assertFalse(awaitOfferState().isClaimingOffer)

            viewModel.claimOffer(winbackOffer, mock())
            assertTrue(awaitOfferState().isClaimingOffer)

            winbackManager.addPurchaseEvent(PurchaseEvent.Cancelled(responseCode = 1))
            val claimedState = awaitOfferState()
            assertFalse(claimedState.isClaimingOffer)
            assertFalse(claimedState.isOfferClaimed)
            assertFalse(claimedState.hasOfferClaimFailed)
        }
    }

    @Test
    fun `claim winback offer when purchase fails`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(winbackResponse)

        viewModel.uiState.test {
            skipItems(1)

            viewModel.claimOffer(winbackOffer, mock())
            skipItems(1)

            winbackManager.addPurchaseEvent(PurchaseEvent.Failure("error", responseCode = 1))
            val claimedState = awaitOfferState()
            assertFalse(claimedState.isClaimingOffer)
            assertFalse(claimedState.isOfferClaimed)
            assertTrue(claimedState.hasOfferClaimFailed)
        }
    }

    @Test
    fun `track screen shown`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

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
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

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
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.trackContinueCancellationTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_CONTINUE_BUTTON_TAP, emptyMap()),
            event,
        )
    }

    @Test
    fun `track claim plus monthly offer tapped`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(createPurchase(productIds = listOf(Subscription.PLUS_MONTHLY_PRODUCT_ID)))
        winbackManager.addWinbackResponse(null)

        viewModel.claimOffer(winbackOffer, mock())
        winbackManager.addPurchaseEvent(PurchaseEvent.Success)

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
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(createPurchase(productIds = listOf(Subscription.PLUS_YEARLY_PRODUCT_ID)))
        winbackManager.addWinbackResponse(null)

        viewModel.claimOffer(winbackOffer, mock())
        winbackManager.addPurchaseEvent(PurchaseEvent.Success)

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
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(createPurchase(productIds = listOf(Subscription.PATRON_MONTHLY_PRODUCT_ID)))
        winbackManager.addWinbackResponse(null)

        viewModel.claimOffer(winbackOffer, mock())
        winbackManager.addPurchaseEvent(PurchaseEvent.Success)

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
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(createPurchase(productIds = listOf(Subscription.PATRON_YEARLY_PRODUCT_ID)))
        winbackManager.addWinbackResponse(null)

        viewModel.claimOffer(winbackOffer, mock())
        winbackManager.addPurchaseEvent(PurchaseEvent.Success)

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
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

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
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

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
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.trackOfferClaimedConfirmationTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_OFFER_CLAIMED_DONE_BUTTON_TAPPED, emptyMap()),
            event,
        )
    }

    @Test
    fun `track plans back button tapped`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.trackPlansBackButtonTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_AVAILABLE_PLANS_BACK_BUTTON_TAPPED, emptyMap()),
            event,
        )
    }

    @Test
    fun `track plan change`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.changePlan(knownPlan, mock())

        winbackManager.addPurchase(createPurchase(productIds = listOf(Subscription.PLUS_MONTHLY_PRODUCT_ID)))
        winbackManager.addPurchaseEvent(PurchaseEvent.Success)

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
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.trackKeepSubscriptionTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_CANCEL_CONFIRMATION_STAY_BUTTON_TAPPED, emptyMap()),
            event,
        )
    }

    @Test
    fun `track cancel subscription tapped`() = runTest {
        winbackManager.addProductDetails(products)
        winbackManager.addPurchase(purchase)
        winbackManager.addWinbackResponse(null)

        viewModel.trackCancelSubscriptionTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_CANCEL_CONFIRMATION_CANCEL_BUTTON_TAPPED, emptyMap()),
            event,
        )
    }
}

private suspend fun TurbineTestContext<WinbackViewModel.UiState>.awaitLoadedState(): SubscriptionPlansState.Loaded {
    return awaitItem().subscriptionPlansState as SubscriptionPlansState.Loaded
}

private suspend fun TurbineTestContext<WinbackViewModel.UiState>.awaitOfferState(): WinbackOfferState {
    return awaitItem().winbackOfferState!!
}

private operator fun SubscriptionPlansState.Loaded.get(productId: String) =
    plans.singleOrNull { it.productId == productId }

private fun WinbackOfferDetails.toProductDetails(
    bonusOfferId: String? = this.offerId,
    customPricingPhases: List<PricingPhase>? = null,
) = createProductDetails(
    id = productId,
    bonusOfferId = bonusOfferId,
    period = billingPeriod,
    customPricingPhases = customPricingPhases,
)

private fun createProductDetails(
    id: String,
    bonusOfferId: String?,
    period: BillingPeriod,
    customPricingPhases: List<PricingPhase>?,
) = mock<ProductDetails> {
    check(id != bonusOfferId) { "ID and offer ID must be different" }

    val basePricingPhase = mock<PricingPhase> {
        on { formattedPrice } doReturn "base-price-$id"
        on { billingPeriod } doReturn period.value
        on { recurrenceMode } doReturn RecurrenceMode.INFINITE_RECURRING
    }
    val offerPricingPhase = bonusOfferId?.let {
        mock<PricingPhase> {
            on { formattedPrice } doReturn "offer-price-$id"
            on { billingPeriod } doReturn period.value
            on { recurrenceMode } doReturn RecurrenceMode.INFINITE_RECURRING
        }
    }
    val basePricingPhases = mock<PricingPhases> {
        on { pricingPhaseList } doReturn listOf(basePricingPhase)
    }
    val offerPricingPhases = offerPricingPhase?.let {
        mock<PricingPhases> {
            on { pricingPhaseList } doReturn (customPricingPhases ?: listOf(offerPricingPhase, basePricingPhase))
        }
    }
    val offerDetails = buildList {
        if (offerPricingPhases != null) {
            add(
                mock<SubscriptionOfferDetails> {
                    on { offerId } doReturn bonusOfferId
                    on { offerToken } doReturn "offer-token-$id"
                    on { pricingPhases } doReturn offerPricingPhases
                },
            )
        }
        add(
            mock<SubscriptionOfferDetails> {
                on { offerToken } doReturn "base-token-$id"
                on { pricingPhases } doReturn basePricingPhases
            },
        )
    }

    on { this.productId } doReturn id
    on { this.title } doReturn "title-$id"
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

class FakeWinbackManager : WinbackManager {
    private val productDetailsTurbine = Turbine<ProductDetailsState>()

    suspend fun addProductDetails(productDetails: ProductDetails) = addProductDetails(listOf(productDetails))

    suspend fun addProductDetails(productDetails: List<ProductDetails>) = productDetailsTurbine.add(ProductDetailsState.Loaded(productDetails))

    private val purchasesTurbine = Turbine<PurchasesState>()

    suspend fun addPurchase(purchase: Purchase) = addPurchases(listOf(purchase))

    suspend fun addPurchases(purchases: List<Purchase>) = purchasesTurbine.add(PurchasesState.Loaded(purchases))

    private val purchaseEventTurbine = Turbine<PurchaseEvent>()

    suspend fun addPurchaseEvent(purchaseEvent: PurchaseEvent) = purchaseEventTurbine.add(purchaseEvent)

    private val winbackResponseTurbine = Turbine<WinbackResponse?>()

    suspend fun addWinbackResponse(response: WinbackResponse?) = winbackResponseTurbine.add(response)

    override suspend fun loadProducts() = productDetailsTurbine.awaitItem()

    override suspend fun loadPurchases() = purchasesTurbine.awaitItem()

    override suspend fun changeProduct(
        currentPurchase: Purchase,
        currentPurchaseProductId: String,
        newProduct: ProductDetails,
        newProductOfferToken: String,
        activity: Activity,
    ) = purchaseEventTurbine.awaitItem()

    override suspend fun getWinbackOffer() = winbackResponseTurbine.awaitItem()

    override suspend fun claimWinbackOffer(
        currentPurchase: Purchase,
        winbackProduct: ProductDetails,
        winbackOfferToken: String,
        winbackClaimCode: String,
        activity: Activity,
    ) = purchaseEventTurbine.awaitItem()
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
