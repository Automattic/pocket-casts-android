package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhases
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SubscriptionMapperTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private lateinit var subscriptionMapper: SubscriptionMapper

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.REFERRALS_CLAIM, true)
        FeatureFlag.setEnabled(Feature.REFERRALS_SEND, true)
        subscriptionMapper = SubscriptionMapper()
    }

    @Test
    fun `returns simple subscription when no offer exists`() {
        val productDetails = createProductDetails(
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )
        FeatureFlag.setEnabled(Feature.INTRO_PLUS_OFFER_ENABLED, true)

        val result = subscriptionMapper.mapFromProductDetails(productDetails, isOfferEligible = true)

        assertTrue(result is Subscription.Simple)
    }

    @Test
    fun `returns null when no matching offer details`() {
        val productDetails = createProductDetails(
            offerID = "non_matching_offer_id",
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )
        FeatureFlag.setEnabled(Feature.INTRO_PLUS_OFFER_ENABLED, true)

        val result = subscriptionMapper.mapFromProductDetails(productDetails, isOfferEligible = true)

        assertNull(result)
    }

    @Test
    fun `returns subscription with intro offer when intro offer exists and intro offer feature enabled`() {
        val productDetails = createProductDetails(
            offerID = Subscription.INTRO_OFFER_ID,
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )
        FeatureFlag.setEnabled(Feature.INTRO_PLUS_OFFER_ENABLED, true)

        val result = subscriptionMapper.mapFromProductDetails(productDetails, isOfferEligible = true)

        assertTrue(result is Subscription.Intro)
    }

    @Test
    fun `returns null when intro offer exists but not eligible`() {
        val productDetails = createProductDetails(
            offerID = Subscription.INTRO_OFFER_ID,
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )
        FeatureFlag.setEnabled(Feature.INTRO_PLUS_OFFER_ENABLED, true)

        val result = subscriptionMapper.mapFromProductDetails(productDetails, isOfferEligible = false)

        assertNull(result)
    }

    @Test
    fun `returns null when intro offer exists but intro offer feature not enabled`() {
        val productDetails = createProductDetails(
            offerID = Subscription.INTRO_OFFER_ID,
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )
        FeatureFlag.setEnabled(Feature.INTRO_PLUS_OFFER_ENABLED, false)

        val result = subscriptionMapper.mapFromProductDetails(productDetails, isOfferEligible = true)

        assertNull(result)
    }

    @Test
    fun `returns subscription with trial offer when trial offer exists`() {
        val productDetails = createProductDetails(
            offerID = Subscription.TRIAL_OFFER_ID,
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )

        val result = subscriptionMapper.mapFromProductDetails(productDetails, isOfferEligible = true)

        assertTrue(result is Subscription.Trial)
    }

    @Test
    fun `returns null when trial offer not eligible`() {
        val productDetails = createProductDetails(
            offerID = Subscription.TRIAL_OFFER_ID,
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )

        val result = subscriptionMapper.mapFromProductDetails(productDetails, isOfferEligible = false)

        assertNull(result)
    }

    @Test
    fun `returns subscription with referral offer when referral offer exists`() {
        val referralProductDetails = mock<ReferralProductDetails> {
            on { offerId } doReturn Subscription.REFERRAL_OFFER_ID
        }
        val productDetails = createProductDetails(
            offerID = Subscription.REFERRAL_OFFER_ID,
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )

        val result = subscriptionMapper.mapFromProductDetails(productDetails, referralProductDetails = referralProductDetails)

        assertTrue(result is Subscription.Trial)
    }

    @Test
    fun `returns null when referral offer not matched`() {
        val referralProductDetails = mock<ReferralProductDetails> {
            on { offerId } doReturn "non_matching_offer_id"
        }
        val productDetails = createProductDetails(
            offerID = Subscription.REFERRAL_OFFER_ID,
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )

        val result = subscriptionMapper.mapFromProductDetails(productDetails, referralProductDetails = referralProductDetails)

        assertNull(result)
    }

    @Test
    fun `returns null when both referral claim and send feature not enabled`() {
        val referralProductDetails = mock<ReferralProductDetails> {
            on { offerId } doReturn Subscription.REFERRAL_OFFER_ID
        }
        val productDetails = createProductDetails(
            offerID = Subscription.REFERRAL_OFFER_ID,
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )
        FeatureFlag.setEnabled(Feature.REFERRALS_CLAIM, false)
        FeatureFlag.setEnabled(Feature.REFERRALS_SEND, false)

        val result = subscriptionMapper.mapFromProductDetails(productDetails, referralProductDetails = referralProductDetails)

        assertNull(result)
    }

    @Test
    fun `returns subscription with referral when referral send feature enabled`() {
        val referralProductDetails = mock<ReferralProductDetails> {
            on { offerId } doReturn Subscription.REFERRAL_OFFER_ID
        }
        val productDetails = createProductDetails(
            offerID = Subscription.REFERRAL_OFFER_ID,
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )
        FeatureFlag.setEnabled(Feature.REFERRALS_CLAIM, false)
        FeatureFlag.setEnabled(Feature.REFERRALS_SEND, true)

        val result = subscriptionMapper.mapFromProductDetails(productDetails, referralProductDetails = referralProductDetails)

        assertTrue(result is Subscription.Trial)
    }

    @Test
    fun `returns subscription with referral when referral claim feature enabled`() {
        val referralProductDetails = mock<ReferralProductDetails> {
            on { offerId } doReturn Subscription.REFERRAL_OFFER_ID
        }
        val productDetails = createProductDetails(
            offerID = Subscription.REFERRAL_OFFER_ID,
            productID = Subscription.PLUS_YEARLY_PRODUCT_ID,
        )
        FeatureFlag.setEnabled(Feature.REFERRALS_CLAIM, true)
        FeatureFlag.setEnabled(Feature.REFERRALS_SEND, false)

        val result = subscriptionMapper.mapFromProductDetails(productDetails, referralProductDetails = referralProductDetails)

        assertTrue(result is Subscription.Trial)
    }

    private fun createProductDetails(
        offerID: String? = null,
        productID: String,
    ): ProductDetails {
        val pricingPhaseBasePlanMock = mock<ProductDetails.PricingPhase> {
            on { billingPeriod } doReturn "P1Y"
            on { recurrenceMode } doReturn ProductDetails.RecurrenceMode.INFINITE_RECURRING
        }
        val pricingPhaseOfferMock = mock<ProductDetails.PricingPhase> {
            on { billingPeriod } doReturn "P1M"
            on { recurrenceMode } doReturn ProductDetails.RecurrenceMode.FINITE_RECURRING
        }
        val pricingPhasesMock = mock<PricingPhases> {
            on { pricingPhaseList } doReturn buildList {
                if (offerID != null) add(pricingPhaseOfferMock)
                add(pricingPhaseBasePlanMock)
            }
        }
        val subscriptionOfferDetailsMock = mock<SubscriptionOfferDetails> {
            on { offerId } doReturn offerID
            on { pricingPhases } doReturn pricingPhasesMock
            on { offerToken } doReturn "offer-token"
        }
        val productDetails = mock<ProductDetails> {
            on { productId } doReturn productID
            on { subscriptionOfferDetails } doReturn listOf(subscriptionOfferDetailsMock)
        }
        return productDetails
    }
}
