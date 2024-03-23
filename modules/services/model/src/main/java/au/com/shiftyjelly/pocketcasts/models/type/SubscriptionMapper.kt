package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PATRON_MONTHLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PATRON_YEARLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_MONTHLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_YEARLY_PRODUCT_ID
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import java.time.Period
import java.time.format.DateTimeParseException

object SubscriptionMapper {
    fun map(productDetails: ProductDetails, isOfferEligible: Boolean): Subscription? {
        val matchingSubscriptionOfferDetails = if (isOfferEligible) {
            productDetails
                .subscriptionOfferDetails
                ?.filter { it.offerSubscriptionPricingPhase != null } // get SubscriptionOfferDetails with offers
                ?.ifEmpty { productDetails.subscriptionOfferDetails } // if no special offers, return all offers available
                ?: productDetails.subscriptionOfferDetails // if null, return all offers
        } else {
            productDetails
                .subscriptionOfferDetails
                ?.filter { it.offerSubscriptionPricingPhase == null } // Take the first if there are multiple SubscriptionOfferDetails without special offers
        } ?: emptyList()

        // TODO handle multiple matching SubscriptionOfferDetails
        if (matchingSubscriptionOfferDetails.size > 1) {
            LogBuffer.w(LogBuffer.TAG_SUBSCRIPTIONS, "Multiple matching SubscriptionOfferDetails found. Only using the first.")
        }
        val relevantSubscriptionOfferDetails = matchingSubscriptionOfferDetails.firstOrNull()

        return relevantSubscriptionOfferDetails
            ?.recurringSubscriptionPricingPhase
            ?.let { recurringPricingPhase ->
                val offerPricingPhase = relevantSubscriptionOfferDetails.offerSubscriptionPricingPhase
                if (offerPricingPhase == null) {
                    Subscription.Simple(
                        tier = mapProductIdToTier(productDetails.productId),
                        recurringPricingPhase = recurringPricingPhase,
                        productDetails = productDetails,
                        offerToken = relevantSubscriptionOfferDetails.offerToken,
                    )
                } else {
                    if (isTrial(productDetails)) {
                        Subscription.Trial(
                            tier = mapProductIdToTier(productDetails.productId),
                            recurringPricingPhase = recurringPricingPhase,
                            offerPricingPhase = offerPricingPhase,
                            productDetails = productDetails,
                            offerToken = relevantSubscriptionOfferDetails.offerToken,
                        )
                    } else if (isIntro(productDetails)) {
                        Subscription.Intro(
                            tier = mapProductIdToTier(productDetails.productId),
                            recurringPricingPhase = recurringPricingPhase,
                            offerPricingPhase = offerPricingPhase,
                            productDetails = productDetails,
                            offerToken = relevantSubscriptionOfferDetails.offerToken,
                        )
                    } else {
                        null
                    }
                }
            }
    }
    private fun isTrial(productDetails: ProductDetails): Boolean {
        return productDetails.subscriptionOfferDetails?.any {
            it.offerId == Subscription.TRIAL_OFFER_ID
        } ?: false
    }
    private fun isIntro(productDetails: ProductDetails): Boolean {
        return productDetails.subscriptionOfferDetails?.any {
            it.offerId == Subscription.INTRO_OFFER_ID
        } ?: false
    }

    private val ProductDetails.SubscriptionOfferDetails.recurringSubscriptionPricingPhase: RecurringSubscriptionPricingPhase?
        get() = recurringSubscriptionPricingPhases().run {
            when (size) {
                0 -> {
                    LogBuffer.e(
                        LogBuffer.TAG_SUBSCRIPTIONS,
                        "ProductDetails did not have any infinite recurring pricing phases. Current implementation expects 1.",
                    )
                    null
                }
                1 -> first()
                else -> {
                    LogBuffer.e(
                        LogBuffer.TAG_SUBSCRIPTIONS,
                        "ProductDetails had $size infinite recurring pricing phases. Current implementation only properly handles 1.",
                    )
                    first()
                }
            }
        }

    private val ProductDetails.SubscriptionOfferDetails.offerSubscriptionPricingPhase: OfferSubscriptionPricingPhase?
        get() = offerSubscriptionPricingPhases().run {
            when (size) {
                0 -> null
                1 -> first()
                else -> {
                    LogBuffer.e(
                        LogBuffer.TAG_SUBSCRIPTIONS,
                        "ProductDetails had $size finite recurring pricing phase. Current implementation only properly handles 1.",
                    )
                    first()
                }
            }
        }

    private fun ProductDetails.SubscriptionOfferDetails.recurringSubscriptionPricingPhases() =
        subscriptionPricingPhases<RecurringSubscriptionPricingPhase>(SubscriptionPricingPhase.Type.RECURRING)

    private fun ProductDetails.SubscriptionOfferDetails.offerSubscriptionPricingPhases() =
        subscriptionPricingPhases<OfferSubscriptionPricingPhase>(SubscriptionPricingPhase.Type.OFFER)

    private inline fun <reified T : SubscriptionPricingPhase> ProductDetails.SubscriptionOfferDetails.subscriptionPricingPhases(
        phaseType: SubscriptionPricingPhase.Type,
    ) =
        pricingPhases
            .pricingPhaseList
            .map { it.fromPricingPhase() }
            .filterIsInstance<T>()
            .filter { it.phaseType() == phaseType } // Must check the phaseType because a SubscriptionPricingPhase class can implement both Offer and Recurring

    private fun ProductDetails.PricingPhase.fromPricingPhase(): SubscriptionPricingPhase? =
        try {
            val period = Period.parse(this.billingPeriod)
            when {
                period.years > 0 -> SubscriptionPricingPhase.Years(this, period)
                period.months > 0 -> SubscriptionPricingPhase.Months(this, period)
                period.days > 0 -> SubscriptionPricingPhase.Days(this, period)
                else -> null
            }
        } catch (_: DateTimeParseException) {
            LogBuffer.e(
                LogBuffer.TAG_SUBSCRIPTIONS,
                "Unable to parse billingPeriod: $billingPeriod",
            )
            null
        }

    fun mapProductIdToTier(productId: String) = when (productId) {
        in listOf(PLUS_MONTHLY_PRODUCT_ID, PLUS_YEARLY_PRODUCT_ID) -> SubscriptionTier.PLUS
        in listOf(PATRON_MONTHLY_PRODUCT_ID, PATRON_YEARLY_PRODUCT_ID) -> SubscriptionTier.PATRON
        else -> SubscriptionTier.UNKNOWN
    }
}
