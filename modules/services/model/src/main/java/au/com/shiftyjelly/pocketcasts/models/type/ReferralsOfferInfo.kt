package au.com.shiftyjelly.pocketcasts.models.type

interface ReferralsOfferInfo {
    val localizedOfferDurationNoun: String
    val localizedOfferDurationAdjective: String
    val localizedPriceAfterOffer: String
}

data object ReferralsOfferInfoMock : ReferralsOfferInfo {
    override val localizedOfferDurationNoun = "2 Months"
    override val localizedOfferDurationAdjective = "2-Month"
    override val localizedPriceAfterOffer = "$39.99 USD"
}
