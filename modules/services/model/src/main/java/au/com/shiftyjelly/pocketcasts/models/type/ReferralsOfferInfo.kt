package au.com.shiftyjelly.pocketcasts.models.type

interface ReferralsOfferInfo {
    val localizedOfferDurationNoun: String
    val localizedOfferDurationAdjective: String
    val localizedPriceAfterOffer: String
}

data class ReferralsOfferInfoPlayStore(
    val subscriptionWithOffer: Subscription.WithOffer? = null,
    override val localizedOfferDurationNoun: String,
    override val localizedOfferDurationAdjective: String,
    override val localizedPriceAfterOffer: String,
) : ReferralsOfferInfo

data object ReferralsOfferInfoMock : ReferralsOfferInfo {
    override val localizedOfferDurationNoun = "2 Months"
    override val localizedOfferDurationAdjective = "2-Month"
    override val localizedPriceAfterOffer = "$39.99 USD"
}
