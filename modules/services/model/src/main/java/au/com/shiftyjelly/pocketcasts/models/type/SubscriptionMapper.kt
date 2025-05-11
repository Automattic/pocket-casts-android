package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import jakarta.inject.Inject
import java.time.Period
import java.time.format.DateTimeParseException

class SubscriptionMapper @Inject constructor() {
    fun mapFromProductDetails(
        productDetails: ProductDetails,
        isOfferEligible: Boolean = false,
        removeWinbackOffer: Boolean = true,
    ): Subscription? {
        val matchingSubscriptionOfferDetails = if (isOfferEligible) {
            productDetails
                .subscriptionOfferDetails
                ?.filter { if (removeWinbackOffer) !it.offerTags.contains(WINBACK_OFFER_TAG) else true }
                ?.filter { it.offerSubscriptionPricingPhase != null } // get SubscriptionOfferDetails with offers
                ?.ifEmpty { productDetails.subscriptionOfferDetails } // if no special offers, return all offers available
                ?: productDetails.subscriptionOfferDetails // if null, return all offers
        } else {
            productDetails
                .subscriptionOfferDetails
                ?.filter { it.offerSubscriptionPricingPhase == null } // Take the first if there are multiple SubscriptionOfferDetails without special offers
        } ?: emptyList()

        val relevantSubscriptionOfferDetails = matchingSubscriptionOfferDetails
            .filter { !it.offerTags.contains(REFERRAL_OFFER_TAG) }
            .filter { if (removeWinbackOffer) !it.offerTags.contains(WINBACK_OFFER_TAG) else true }
            // This assumes that base plan has lowest number of pricing phases,
            // and if something else has the same number of phases,
            // base plan will be the first one
            .minByOrNull { offerDetails -> offerDetails.pricingPhases.pricingPhaseList.size }

        return relevantSubscriptionOfferDetails
            ?.recurringSubscriptionPricingPhase
            ?.let { recurringPricingPhase ->
                val offerPricingPhase = relevantSubscriptionOfferDetails.offerSubscriptionPricingPhase
                if (offerPricingPhase == null) {
                    Subscription.Simple(
                        tier = SubscriptionTier.fromProductId(productDetails.productId),
                        recurringPricingPhase = recurringPricingPhase,
                        productDetails = productDetails,
                        offerToken = relevantSubscriptionOfferDetails.offerToken,
                    )
                } else {
                    if (FeatureFlag.isEnabled(Feature.INTRO_PLUS_OFFER_ENABLED) && hasIntro(productDetails)) {
                        Subscription.Intro(
                            tier = SubscriptionTier.fromProductId(productDetails.productId),
                            recurringPricingPhase = recurringPricingPhase,
                            offerPricingPhase = offerPricingPhase,
                            productDetails = productDetails,
                            offerToken = relevantSubscriptionOfferDetails.offerToken,
                        )
                    } else if (hasTrial(productDetails)) {
                        Subscription.Trial(
                            tier = SubscriptionTier.fromProductId(productDetails.productId),
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

    private fun hasTrial(productDetails: ProductDetails): Boolean {
        return productDetails.subscriptionOfferDetails?.any {
            it.offerId in listOf(Subscription.TRIAL_OFFER_ID)
        } ?: false
    }

    private fun hasIntro(productDetails: ProductDetails): Boolean {
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

    companion object {
        private const val REFERRAL_OFFER_TAG = "referral-offer"
        private const val WINBACK_OFFER_TAG = "winback"
    }
}
