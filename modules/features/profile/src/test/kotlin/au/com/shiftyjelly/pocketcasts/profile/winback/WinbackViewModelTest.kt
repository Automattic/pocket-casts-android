package au.com.shiftyjelly.pocketcasts.profile.winback

import android.app.Activity
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.Tracker
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.payment.AcknowledgedSubscription
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.FakePaymentDataSource
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentResultCode
import au.com.shiftyjelly.pocketcasts.payment.Purchase
import au.com.shiftyjelly.pocketcasts.payment.PurchaseState
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionPlan
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager.ReferralResult
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.pocketcasts.service.api.winbackResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

class WinbackViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val paymentDataSource = FakePaymentDataSource()
    private val tracker = FakeTracker()
    private val settings = mock<Settings>()
    private val referralManager = mock<ReferralManager>()

    private lateinit var viewModel: WinbackViewModel

    @Before
    fun setUp() {
        val subscriptionSettingMock = mock<UserSetting<Subscription?>> {
            on { flow } doReturn MutableStateFlow(null)
        }
        whenever(settings.cachedSubscription) doReturn subscriptionSettingMock
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(
            offerId = SubscriptionOffer.Winback.offerId(SubscriptionTier.Plus, BillingCycle.Yearly)!!,
        )

        viewModel = WinbackViewModel(
            paymentClient = PaymentClient.test(paymentDataSource),
            referralManager = referralManager,
            settings = settings,
            tracker = AnalyticsTracker.test(tracker, isFirstPartyEnabled = true),
        )
    }

    @Test
    fun `initial plans state is loading`() {
        val state = viewModel.uiState.value

        assertEquals(SubscriptionPlansState.Loading, state.subscriptionPlansState)
    }

    @Test
    fun `subscription plans for user with active subscription`() = runTest {
        viewModel.loadWinbackData()

        viewModel.uiState.test {
            val state = awaitLoadedState()
            assertEquals(state.currentSubscription.productId, SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID)
        }
    }

    @Test
    fun `subscription plans for user with unacknowledged purchase`() = runTest {
        paymentDataSource.loadedPurchases = listOf(createPurchase(isAcknowledged = false))

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoPurchases, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with not auto-renewing purchase`() = runTest {
        paymentDataSource.loadedPurchases = listOf(createPurchase(isAutoRenewing = false))

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoPurchases, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with purchase without order ID`() = runTest {
        paymentDataSource.loadedPurchases = listOf(createPurchase(orderId = null))

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoPurchases, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with multiple purchases`() = runTest {
        paymentDataSource.loadedPurchases = listOf(createPurchase(orderId = "1"), createPurchase(orderId = "2"))

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.TooManyPurchases, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with purchase with multiple products`() = runTest {
        paymentDataSource.loadedPurchases = listOf(createPurchase(productIds = listOf("id1", "id2")))

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoPurchases, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans for user with purchase with no products`() = runTest {
        paymentDataSource.loadedPurchases = emptyList()

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            val availablePlans = awaitItem().subscriptionPlansState as SubscriptionPlansState.Failure

            assertEquals(FailureReason.NoPurchases, availablePlans.reason)
        }
    }

    @Test
    fun `subscription plans are sorted`() = runTest {
        viewModel.loadWinbackData()

        viewModel.uiState.test {
            val loadedState = awaitLoadedState()

            val expected = listOf(
                SubscriptionPlan.PlusMonthlyPreview,
                SubscriptionPlan.PatronMonthlyPreview,
                SubscriptionPlan.PlusYearlyPreview,
                SubscriptionPlan.PatronYearlyPreview,
            )
            assertEquals(expected, loadedState.basePlans)
        }
    }

    @Test
    fun `change subscription plan successfully`() = runTest {
        val newPurchase = createPurchase(
            orderId = "new-order-id",
            productIds = listOf(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID),
        )
        val newSubscription = AcknowledgedSubscription("new-order-id", SubscriptionTier.Plus, BillingCycle.Monthly, isAutoRenewing = true)

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            paymentDataSource.loadedPurchases = listOf(newPurchase)

            viewModel.changePlan(SubscriptionPlan.PlusMonthlyPreview, mock<Activity>())
            assertTrue(awaitLoadedState().isChangingPlan)

            val state = awaitLoadedState()
            assertFalse(state.isChangingPlan)
            assertEquals(newSubscription, state.currentSubscription)
        }
    }

    @Test
    fun `change subscription when current state is not loaded`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.changePlan(SubscriptionPlan.PlusMonthlyPreview, mock())
            expectNoEvents()
        }
    }

    @Test
    fun `change subscription when it is cancelled`() = runTest {
        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            paymentDataSource.purchasedProductsResultCode = PaymentResultCode.UserCancelled

            viewModel.changePlan(SubscriptionPlan.PlusMonthlyPreview, mock())
            assertTrue(awaitLoadedState().isChangingPlan)

            val state = awaitLoadedState()
            assertFalse(state.isChangingPlan)
            assertFalse(state.hasPlanChangeFailed)
        }
    }

    @Test
    fun `change subscription when purchase fails`() = runTest {
        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            paymentDataSource.purchasedProductsResultCode = PaymentResultCode.BillingUnavailable

            viewModel.changePlan(SubscriptionPlan.PlusMonthlyPreview, mock())
            assertTrue(awaitLoadedState().isChangingPlan)

            val state = awaitLoadedState()
            assertFalse(state.isChangingPlan)
            assertTrue(state.hasPlanChangeFailed)
        }
    }

    @Test
    fun `change subscription when new plans fail to load`() = runTest {
        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertFalse(awaitLoadedState().isChangingPlan)

            paymentDataSource.loadedPurchasesResultCode = PaymentResultCode.BillingUnavailable

            viewModel.changePlan(SubscriptionPlan.PlusMonthlyPreview, mock())
            assertTrue(awaitLoadedState().isChangingPlan)

            val changedPlanState = awaitItem()
            assertTrue(changedPlanState.subscriptionPlansState is SubscriptionPlansState.Failure)
            assertNull(changedPlanState.winbackOfferState)
        }
    }

    @Test
    fun `plus monthly winback offer`() = runTest {
        paymentDataSource.loadedPurchases = listOf(
            createPurchase(productIds = listOf(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID)),
        )
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(
            offerId = SubscriptionOffer.Winback.offerId(SubscriptionTier.Plus, BillingCycle.Monthly)!!,
        )

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertEquals(
                WinbackOffer(
                    redeemCode = "ABC",
                    formattedPrice = "$3.99",
                    tier = SubscriptionTier.Plus,
                    billingCycle = BillingCycle.Monthly,
                ),
                awaitItem().winbackOfferState?.offer,
            )
        }
    }

    @Test
    fun `plus yearly winback offer`() = runTest {
        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertEquals(
                WinbackOffer(
                    redeemCode = "ABC",
                    formattedPrice = "$20.00",
                    tier = SubscriptionTier.Plus,
                    billingCycle = BillingCycle.Yearly,
                ),
                awaitItem().winbackOfferState?.offer,
            )
        }
    }

    @Test
    fun `patron monthly winback offer`() = runTest {
        paymentDataSource.loadedPurchases = listOf(
            createPurchase(productIds = listOf(SubscriptionPlan.PATRON_MONTHLY_PRODUCT_ID)),
        )
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(
            offerId = SubscriptionOffer.Winback.offerId(SubscriptionTier.Patron, BillingCycle.Monthly)!!,
        )

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertEquals(
                WinbackOffer(
                    redeemCode = "ABC",
                    formattedPrice = "$9.99",
                    tier = SubscriptionTier.Patron,
                    billingCycle = BillingCycle.Monthly,
                ),
                awaitItem().winbackOfferState?.offer,
            )
        }
    }

    @Test
    fun `patron yearly winback offer`() = runTest {
        paymentDataSource.loadedPurchases = listOf(
            createPurchase(productIds = listOf(SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID)),
        )
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(
            offerId = SubscriptionOffer.Winback.offerId(SubscriptionTier.Patron, BillingCycle.Yearly)!!,
        )

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertEquals(
                WinbackOffer(
                    redeemCode = "ABC",
                    formattedPrice = "$50.00",
                    tier = SubscriptionTier.Patron,
                    billingCycle = BillingCycle.Yearly,
                ),
                awaitItem().winbackOfferState?.offer,
            )
        }
    }

    @Test
    fun `no winback offer`() = runTest {
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn ReferralResult.EmptyResult()

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `change winback offer after changing subscription plan successfully`() = runTest {
        val newPurchase = createPurchase(
            orderId = "new-order-id",
            productIds = listOf(SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID),
        )
        val newSubscription = AcknowledgedSubscription("new-order-id", SubscriptionTier.Patron, BillingCycle.Yearly, isAutoRenewing = true)

        viewModel.loadWinbackData()

        paymentDataSource.loadedPurchases = listOf(newPurchase)
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(
            offerId = SubscriptionOffer.Winback.offerId(SubscriptionTier.Patron, BillingCycle.Yearly)!!,
        )

        viewModel.changePlan(SubscriptionPlan.PlusMonthlyPreview, mock<Activity>())

        assertEquals(
            WinbackOffer(
                redeemCode = "ABC",
                formattedPrice = "$50.00",
                tier = SubscriptionTier.Patron,
                billingCycle = BillingCycle.Yearly,
            ),
            viewModel.uiState.value.winbackOfferState?.offer,
        )
    }

    @Test
    fun `winback offer with blank offer ID`() = runTest {
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(offerId = "")

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `winback offer with blank redeem code`() = runTest {
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(code = "")

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `winback offer with unknown ID`() = runTest {
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(offerId = "unknown-offer")

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `winback offer with no matching product ID`() = runTest {
        paymentDataSource.loadedPurchases = listOf(
            createPurchase(productIds = listOf(SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID)),
        )
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(
            offerId = SubscriptionOffer.Winback.offerId(SubscriptionTier.Plus, BillingCycle.Monthly)!!,
        )

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertNull(awaitItem().winbackOfferState)
        }
    }

    @Test
    fun `winback offer with too few pricing phases`() = runTest {
        paymentDataSource.loadedProducts = FakePaymentDataSource.DefaultLoadedProducts.map { product ->
            val newOfferPlans = product.pricingPlans.offerPlans.map { offer ->
                offer.copy(pricingPhases = offer.pricingPhases.take(1))
            }
            val newPricingPlans = product.pricingPlans.copy(offerPlans = newOfferPlans)
            product.copy(pricingPlans = newPricingPlans)
        }
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(
            offerId = SubscriptionOffer.Winback.offerId(SubscriptionTier.Plus, BillingCycle.Monthly)!!,
        )

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.subscriptionPlansState is SubscriptionPlansState.Loaded)
            assertNull(state.winbackOfferState)
        }
    }

    @Test
    fun `winback offer with too many pricing phases`() = runTest {
        paymentDataSource.loadedProducts = FakePaymentDataSource.DefaultLoadedProducts.map { product ->
            val newOfferPlans = product.pricingPlans.offerPlans.map { offer ->
                offer.copy(pricingPhases = offer.pricingPhases + offer.pricingPhases)
            }
            val newPricingPlans = product.pricingPlans.copy(offerPlans = newOfferPlans)
            product.copy(pricingPlans = newPricingPlans)
        }
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(
            offerId = SubscriptionOffer.Winback.offerId(SubscriptionTier.Plus, BillingCycle.Monthly)!!,
        )

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.subscriptionPlansState is SubscriptionPlansState.Loaded)
            assertNull(state.winbackOfferState)
        }
    }

    @Test
    fun `claim winback offer successfully`() = runTest {
        wheneverBlocking { referralManager.getWinbackResponse() } doReturn createSuccessReferralResult(
            offerId = SubscriptionOffer.Winback.offerId(SubscriptionTier.Plus, BillingCycle.Yearly)!!,
        )

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            val initialState = awaitOfferState()
            assertFalse(initialState.isClaimingOffer)
            assertFalse(initialState.isOfferClaimed)
            assertFalse(initialState.hasOfferClaimFailed)

            viewModel.claimOffer(mock<Activity>())
            val claimingState = awaitOfferState()
            assertTrue(claimingState.isClaimingOffer)
            assertFalse(claimingState.isOfferClaimed)
            assertFalse(claimingState.hasOfferClaimFailed)

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
        paymentDataSource.loadedPurchases = emptyList()

        viewModel.loadWinbackData()

        viewModel.uiState.test {
            skipItems(1)

            viewModel.claimOffer(mock<Activity>())
            expectNoEvents()
        }
    }

    @Test
    fun `claim winback offer when it is cancelled`() = runTest {
        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertFalse(awaitOfferState().isClaimingOffer)

            paymentDataSource.purchasedProductsResultCode = PaymentResultCode.UserCancelled

            viewModel.claimOffer(mock<Activity>())
            assertTrue(awaitOfferState().isClaimingOffer)

            val claimedState = awaitOfferState()
            assertFalse(claimedState.isClaimingOffer)
            assertFalse(claimedState.isOfferClaimed)
            assertFalse(claimedState.hasOfferClaimFailed)
        }
    }

    @Test
    fun `claim winback offer when purchase fails`() = runTest {
        viewModel.loadWinbackData()

        viewModel.uiState.test {
            assertFalse(awaitOfferState().isClaimingOffer)

            paymentDataSource.purchasedProductsResultCode = PaymentResultCode.BillingUnavailable

            viewModel.claimOffer(mock<Activity>())
            assertTrue(awaitOfferState().isClaimingOffer)

            val claimedState = awaitOfferState()
            assertFalse(claimedState.isClaimingOffer)
            assertFalse(claimedState.isOfferClaimed)
            assertTrue(claimedState.hasOfferClaimFailed)
        }
    }

    @Test
    fun `track screen shown`() = runTest {
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
        viewModel.trackContinueCancellationTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_CONTINUE_BUTTON_TAP, emptyMap()),
            event,
        )
    }

    @Test
    fun `track claim plus monthly offer tapped`() = runTest {
        paymentDataSource.loadedPurchases = listOf(
            createPurchase(productIds = listOf(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID)),
        )

        viewModel.loadWinbackData()
        viewModel.claimOffer(mock<Activity>())

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
        paymentDataSource.loadedPurchases = listOf(
            createPurchase(productIds = listOf(SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID)),
        )

        viewModel.loadWinbackData()
        viewModel.claimOffer(mock<Activity>())

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
        paymentDataSource.loadedPurchases = listOf(
            createPurchase(productIds = listOf(SubscriptionPlan.PATRON_MONTHLY_PRODUCT_ID)),
        )

        viewModel.loadWinbackData()
        viewModel.claimOffer(mock<Activity>())

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
        paymentDataSource.loadedPurchases = listOf(
            createPurchase(productIds = listOf(SubscriptionPlan.PATRON_YEARLY_PRODUCT_ID)),
        )

        viewModel.loadWinbackData()
        viewModel.claimOffer(mock<Activity>())

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
        viewModel.trackOfferClaimedConfirmationTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_OFFER_CLAIMED_DONE_BUTTON_TAPPED, emptyMap()),
            event,
        )
    }

    @Test
    fun `track plans back button tapped`() = runTest {
        viewModel.trackPlansBackButtonTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_AVAILABLE_PLANS_BACK_BUTTON_TAPPED, emptyMap()),
            event,
        )
    }

    @Test
    fun `track plan change`() = runTest {
        val newPurchase = createPurchase(
            orderId = "new-order-id",
            productIds = listOf(SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID),
        )

        viewModel.loadWinbackData()
        paymentDataSource.loadedPurchases = listOf(newPurchase)
        viewModel.changePlan(SubscriptionPlan.PlusMonthlyPreview, mock<Activity>())

        assertEquals(
            listOf(
                TrackEvent(
                    AnalyticsEvent.WINBACK_AVAILABLE_PLANS_SELECT_PLAN,
                    mapOf(
                        "product" to SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID,
                    ),
                ),
                TrackEvent(
                    AnalyticsEvent.WINBACK_AVAILABLE_PLANS_NEW_PLAN_PURCHASE_SUCCESSFUL,
                    mapOf(
                        "current_product" to SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID,
                        "new_product" to SubscriptionPlan.PLUS_MONTHLY_PRODUCT_ID,
                    ),
                ),
            ),
            tracker.events,
        )
    }

    @Test
    fun `track keep subscription tapped`() = runTest {
        viewModel.trackKeepSubscriptionTapped()

        val event = tracker.events.single()
        assertEquals(
            TrackEvent(AnalyticsEvent.WINBACK_CANCEL_CONFIRMATION_STAY_BUTTON_TAPPED, emptyMap()),
            event,
        )
    }

    @Test
    fun `track cancel subscription tapped`() = runTest {
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

private fun createPurchase(
    orderId: String? = "order-id",
    productIds: List<String> = listOf(SubscriptionPlan.PLUS_YEARLY_PRODUCT_ID),
    isAcknowledged: Boolean = true,
    isAutoRenewing: Boolean = true,
) = Purchase(
    state = orderId?.let(PurchaseState::Purchased) ?: PurchaseState.Pending,
    token = "token",
    productIds = productIds,
    isAcknowledged = isAcknowledged,
    isAutoRenewing = isAutoRenewing,
)

private fun createSuccessReferralResult(
    offerId: String = SubscriptionOffer.Winback.offerId(SubscriptionTier.Plus, BillingCycle.Yearly)!!,
    code: String = "ABC",
) = ReferralResult.SuccessResult(
    winbackResponse {
        this.code = code
        this.offer = offerId
    },
)

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
