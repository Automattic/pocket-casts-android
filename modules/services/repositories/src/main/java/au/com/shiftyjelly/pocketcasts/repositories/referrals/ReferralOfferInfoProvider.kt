package au.com.shiftyjelly.pocketcasts.repositories.referrals

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.type.ReferralProductDetails
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfo
import au.com.shiftyjelly.pocketcasts.models.type.ReferralsOfferInfoPlayStore
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.reactive.asFlow

const val REFERRAL_SUBSCRIPTION_PRODUCT_ID = Subscription.PLUS_YEARLY_PRODUCT_ID
const val REFERRAL_OFFER_ID = Subscription.REFERRAL_OFFER_ID

class ReferralOfferInfoProvider @Inject constructor(
    private val subscriptionManager: SubscriptionManager,
    private val subscriptionMapper: SubscriptionMapper,
    @ApplicationContext private val context: Context,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun referralOfferInfo(
        referralProductDetails: ReferralProductDetails = ReferralProductDetails(REFERRAL_SUBSCRIPTION_PRODUCT_ID, REFERRAL_OFFER_ID),
    ): ReferralsOfferInfo? =
        subscriptionManager
            .observeProductDetails()
            .asFlow()
            .transformLatest<ProductDetailsState, ReferralsOfferInfoPlayStore> { productDetailsState ->
                val subscriptionProductDetails = (productDetailsState as? ProductDetailsState.Loaded)?.productDetails?.mapNotNull { productDetails ->
                    subscriptionMapper.mapFromProductDetails(
                        productDetails = productDetails,
                        referralProductDetails = referralProductDetails,
                    )
                } ?: emptyList()

                val isReferralOffer: (Subscription.WithOffer) -> Boolean = { subscription ->
                    subscription.productDetails.productId == referralProductDetails.productId &&
                        subscription.productDetails.subscriptionOfferDetails?.any { it.offerId == referralProductDetails.offerId } == true
                }

                val referralSubscriptionWithOffer = subscriptionProductDetails
                    .filterIsInstance<Subscription.WithOffer>()
                    .find(isReferralOffer)

                emit(
                    ReferralsOfferInfoPlayStore(
                        subscriptionWithOffer = referralSubscriptionWithOffer,
                        localizedOfferDurationAdjective = referralSubscriptionWithOffer?.offerPricingPhase?.periodWithDash(context.resources) ?: "",
                        localizedOfferDurationNoun = referralSubscriptionWithOffer?.offerPricingPhase?.periodValuePlural(context.resources) ?: "",
                        localizedPriceAfterOffer = referralSubscriptionWithOffer?.recurringPricingPhase?.let {
                            "${it.formattedPrice} ${it.priceCurrencyCode}"
                        } ?: "",
                    ),
                )
            }.firstOrNull()
}
