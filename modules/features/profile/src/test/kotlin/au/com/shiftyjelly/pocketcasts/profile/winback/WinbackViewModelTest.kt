package au.com.shiftyjelly.pocketcasts.profile.winback

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.PricingPhases
import com.android.billingclient.api.ProductDetails.RecurrenceMode
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import java.util.Date
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class WinbackViewModelTest {
    private val signInStateFlow = MutableSharedFlow<SignInState>()
    private val productDetailsFlow = MutableSharedFlow<ProductDetailsState>()

    private lateinit var viewModel: WinbackViewModel

    @Before
    fun setUp() {
        val userManager = mock<UserManager> {
            on { getSignInState() } doReturn signInStateFlow.asFlowable()
        }
        val subscriptionManager = mock<SubscriptionManager> {
            on { observeProductDetails() } doReturn productDetailsFlow.asFlowable()
        }
        viewModel = WinbackViewModel(userManager, subscriptionManager)
    }

    @Test
    fun `initial state`() = runTest {
        viewModel.uiState.test {
            assertEquals(WinbackViewModel.UiState.Empty, awaitItem())
        }
    }

    @Test
    fun `available plans for paid user with known subscription`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            signInStateFlow.emit(createPaidUser(SubscriptionView(plan = Subscription.PLUS_MONTHLY_PRODUCT_ID, isPrimary = true)))
            productDetailsFlow.emit(ProductDetailsState.Loaded(availableSubscriptions))
            val state = awaitItem()
            val plans = state.availablePlans as AvailablePlans.Loaded

            val plusMonthly = plans[Subscription.PLUS_MONTHLY_PRODUCT_ID]
            val patronMonthly = plans[Subscription.PATRON_MONTHLY_PRODUCT_ID]
            val plusYearly = plans[Subscription.PLUS_YEARLY_PRODUCT_ID]
            val patronYearly = plans[Subscription.PATRON_YEARLY_PRODUCT_ID]

            assertNotNull(plusMonthly?.productId)
            assertNotNull(patronMonthly?.productId)
            assertNotNull(plusYearly?.productId)
            assertNotNull(patronYearly?.productId)

            assertEquals(state.userSubscriptionId, Subscription.PLUS_MONTHLY_PRODUCT_ID)
        }
    }

    @Test
    fun `available plans for paid user with unknown subscription`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            signInStateFlow.emit(createPaidUser(SubscriptionView(plan = "unknown.plan", isPrimary = true)))
            productDetailsFlow.emit(ProductDetailsState.Loaded(availableSubscriptions))
            val availablePlans = awaitItem().availablePlans

            assertTrue(availablePlans is AvailablePlans.Loaded)
        }
    }

    @Test
    fun `available plans for paid user without any plan`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            signInStateFlow.emit(createPaidUser(SubscriptionView(plan = null, isPrimary = true)))
            productDetailsFlow.emit(ProductDetailsState.Loaded(availableSubscriptions))
            val availablePlans = awaitItem().availablePlans

            assertTrue(availablePlans is AvailablePlans.Loaded)
        }
    }

    @Test
    fun `available plans for signed out user`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            signInStateFlow.emit(SignInState.SignedOut)
            productDetailsFlow.emit(ProductDetailsState.Loaded(availableSubscriptions))
            val availablePlans = awaitItem().availablePlans

            assertTrue(availablePlans is AvailablePlans.Failure)
        }
    }

    @Test
    fun `available plans for free user`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            signInStateFlow.emit(SignInState.SignedOut)
            productDetailsFlow.emit(ProductDetailsState.Loaded(availableSubscriptions))
            val availablePlans = awaitItem().availablePlans

            assertTrue(availablePlans is AvailablePlans.Failure)
        }
    }

    @Test
    fun `available plans for paid user without primary subscription`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            signInStateFlow.emit(createPaidUser(SubscriptionView(plan = "no.plan", isPrimary = false)))
            productDetailsFlow.emit(ProductDetailsState.Loaded(availableSubscriptions))
            val availablePlans = awaitItem().availablePlans

            assertTrue(availablePlans is AvailablePlans.Failure)
        }
    }

    @Test
    fun `available plans ignore subscription offers`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            signInStateFlow.emit(createPaidUser(SubscriptionView(plan = "", isPrimary = true)))
            productDetailsFlow.emit(
                ProductDetailsState.Loaded(
                    listOf(
                        createProductDetails(
                            id = Subscription.PLUS_MONTHLY_PRODUCT_ID,
                            period = BillingPeriod.Monthly,
                            offer = Offer(
                                id = "offer-id",
                                billingPeriod = BillingPeriod.Monthly,
                            ),
                        ),
                    ),
                ),
            )
            val availablePlans = awaitItem().availablePlans as AvailablePlans.Loaded

            val plan = availablePlans[Subscription.PLUS_MONTHLY_PRODUCT_ID]
            assertEquals(Subscription.PLUS_MONTHLY_PRODUCT_ID, plan?.offerToken)
        }
    }

    @Test
    fun `available plans are sorted`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            signInStateFlow.emit(createPaidUser(SubscriptionView(plan = "", isPrimary = true)))
            productDetailsFlow.emit(ProductDetailsState.Loaded(availableSubscriptions.reversed()))
            val availablePlans = awaitItem().availablePlans as AvailablePlans.Loaded

            val tokens = availablePlans.plans.map(SubscriptionPlan::productId)
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

private operator fun AvailablePlans.Loaded.get(productId: String) = plans.singleOrNull { it.productId == productId }

private class SubscriptionView(
    val plan: String?,
    val isPrimary: Boolean,
)

private class Offer(
    val id: String,
    val billingPeriod: BillingPeriod,
)

private val availableSubscriptions = listOf(
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

private fun createPaidUser(
    vararg subscriptions: SubscriptionView,
) = SignInState.SignedIn(
    email = "noreplay@pocketcasts.com",
    subscriptionStatus = createPaidSubscritpion(*subscriptions),
)

private fun createPaidSubscritpion(
    vararg subscriptions: SubscriptionView,
) = SubscriptionStatus.Paid(
    expiryDate = Date(),
    autoRenew = true,
    giftDays = 0,
    frequency = SubscriptionFrequency.NONE,
    platform = SubscriptionPlatform.ANDROID,
    tier = SubscriptionTier.PLUS,
    index = 0,
    subscriptions = subscriptions.map {
        SubscriptionStatus.Subscription(
            plan = it.plan,
            isPrimarySubscription = it.isPrimary,
            tier = SubscriptionTier.PLUS,
            frequency = SubscriptionFrequency.YEARLY,
            autoRenewing = true,
            expiryDate = Date(),
            updateUrl = "",
        )
    },
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

    on { productId } doReturn id
    on { title } doReturn "title: $id"
    on { subscriptionOfferDetails } doReturn offerDetails
}

private val BillingPeriod.value get() = when (this) {
    BillingPeriod.Monthly -> "P1M"
    BillingPeriod.Yearly -> "P1Y"
}
