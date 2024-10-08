package au.com.shiftyjelly.pocketcasts.repositories.referrals

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoPlayStore
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import com.android.billingclient.api.ProductDetails
import io.reactivex.Flowable
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ReferralOfferInfoProviderTest {
    private lateinit var referralOfferInfoProvider: ReferralOfferInfoProvider
    private val subscriptionMapper: SubscriptionMapper = mock()
    private val context: Context = mock()
    private val subscriptionManager: SubscriptionManager = mock()

    @Before
    fun setUp() {
        referralOfferInfoProvider = ReferralOfferInfoProvider(
            subscriptionManager = subscriptionManager,
            subscriptionMapper = subscriptionMapper,
            context = context,
        )
    }

    @Test
    fun `referralOfferInfo returns offer info when subscription has referral product with offer`() = runTest {
        val productDetailsState = createProductDetailsState(REFERRAL_SUBSCRIPTION_PRODUCT_ID, REFERRAL_OFFER_ID)
        whenever(subscriptionManager.observeProductDetails()).thenReturn(Flowable.just(productDetailsState))

        val result = referralOfferInfoProvider.referralOfferInfo()

        assertNotNull((result as ReferralsOfferInfoPlayStore).subscriptionWithOffer)
    }

    @Test
    fun `referralOfferInfo returns null when subscription does not have referral product`() = runTest {
        val productDetailsState = createProductDetailsState("not-a-referral-product-id", REFERRAL_OFFER_ID)
        whenever(subscriptionManager.observeProductDetails()).thenReturn(Flowable.just(productDetailsState))

        val result = referralOfferInfoProvider.referralOfferInfo()

        assertNull((result as ReferralsOfferInfoPlayStore).subscriptionWithOffer)
    }

    @Test
    fun `referralOfferInfo returns null when subscription does not have referral offer`() = runTest {
        val productDetailsState = createProductDetailsState(REFERRAL_SUBSCRIPTION_PRODUCT_ID, "not-a-referral-offer-id")
        whenever(subscriptionManager.observeProductDetails()).thenReturn(Flowable.just(productDetailsState))

        val result = referralOfferInfoProvider.referralOfferInfo()

        assertNull((result as ReferralsOfferInfoPlayStore).subscriptionWithOffer)
    }

    @Test
    fun `referralOfferInfo returns null subscription when no subscription product found`() = runTest {
        val productDetailsState = ProductDetailsState.Loaded(emptyList())
        whenever(subscriptionManager.observeProductDetails()).thenReturn(Flowable.just(productDetailsState))

        val result = referralOfferInfoProvider.referralOfferInfo()

        assertNull((result as ReferralsOfferInfoPlayStore).subscriptionWithOffer)
    }

    @Test
    fun `referralOfferInfo returns null subscription when product details state is error`() = runTest {
        val productDetailsState = ProductDetailsState.Error("error")
        whenever(subscriptionManager.observeProductDetails()).thenReturn(Flowable.just(productDetailsState))

        val result = referralOfferInfoProvider.referralOfferInfo()

        assertNull((result as ReferralsOfferInfoPlayStore).subscriptionWithOffer)
    }

    private fun createProductDetailsState(
        productId: String,
        offerId: String,
    ): ProductDetailsState.Loaded {
        val productDetails = mock<ProductDetails>()
        val subscriptionOfferDetails = mock<ProductDetails.SubscriptionOfferDetails>()
        val subscription = mock<Subscription.Trial>()
        whenever(productDetails.productId).thenReturn(productId)
        whenever(productDetails.subscriptionOfferDetails).thenReturn(listOf(subscriptionOfferDetails))
        whenever(subscription.productDetails).thenReturn(productDetails)
        whenever(subscriptionOfferDetails.offerId).thenReturn(offerId)
        whenever(subscriptionMapper.mapFromProductDetails(eq(productDetails), anyBoolean(), any())).thenReturn(subscription)

        return ProductDetailsState.Loaded(listOf(productDetails))
    }
}
