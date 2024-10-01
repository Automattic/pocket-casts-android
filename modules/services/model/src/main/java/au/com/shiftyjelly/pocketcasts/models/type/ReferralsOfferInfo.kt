package au.com.shiftyjelly.pocketcasts.models.type

import android.content.Context

interface ReferralsOfferInfo {
    val localizedOfferDurationNoun: String
    val localizedOfferDurationAdjective: String
    val localizedPriceAfterOffer: String
}

data class ReferralsOfferInfoPlayStore(
    val subscriptionWithOffer: Subscription.WithOffer? = null,
    val context: Context,
) : ReferralsOfferInfo {
    override val localizedOfferDurationAdjective = subscriptionWithOffer?.offerPricingPhase?.periodWithDash(context.resources) ?: ""
    override val localizedOfferDurationNoun = subscriptionWithOffer?.offerPricingPhase?.periodValuePlural(context.resources) ?: ""
    override val localizedPriceAfterOffer = subscriptionWithOffer?.recurringPricingPhase?.let {
        "${it.formattedPrice} ${it.priceCurrencyCode}"
    } ?: ""
}

data object ReferralsOfferInfoMock : ReferralsOfferInfo {
    override val localizedOfferDurationNoun = "2 Months"
    override val localizedOfferDurationAdjective = "2-Month"
    override val localizedPriceAfterOffer = "$39.99 USD"
}
