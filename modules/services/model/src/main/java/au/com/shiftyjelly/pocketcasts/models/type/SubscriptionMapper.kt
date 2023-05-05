package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PATRON_PRODUCT_BASE
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.Companion.PLUS_PRODUCT_BASE
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import java.time.Period
import java.time.format.DateTimeParseException

object SubscriptionMapper {
    fun map(productDetails: ProductDetails, isFreeTrialEligible: Boolean): Subscription? {

        val matchingSubscriptionOfferDetails = if (isFreeTrialEligible) {
            productDetails
                .subscriptionOfferDetails
                ?.filter { it.trialSubscriptionPricingPhase != null } // get SubscriptionOfferDetails with trial offers
                ?.ifEmpty { productDetails.subscriptionOfferDetails } // if no trial offers, return all offers
                ?: productDetails.subscriptionOfferDetails // if null, return all offers
        } else {
            productDetails
                .subscriptionOfferDetails
                ?.filter { it.trialSubscriptionPricingPhase == null } // Take the first if there are multiple SubscriptionOfferDetails without trial offers
        } ?: emptyList()

        // TODO handle multiple matching SubscriptionOfferDetails
        if (matchingSubscriptionOfferDetails.size > 1) {
            LogBuffer.w(LogBuffer.TAG_SUBSCRIPTIONS, "Multiple matching SubscriptionOfferDetails found. Only using the first.")
        }
        val relevantSubscriptionOfferDetails = matchingSubscriptionOfferDetails.firstOrNull()

        return relevantSubscriptionOfferDetails
            ?.recurringSubscriptionPricingPhase
            ?.let { recurringPricingPhase ->
                val trialPricingPhase = relevantSubscriptionOfferDetails.trialSubscriptionPricingPhase
                if (trialPricingPhase == null) {
                    Subscription.Simple(
                        tier = mapProductIdToTier(productDetails.productId),
                        recurringPricingPhase = recurringPricingPhase,
                        productDetails = productDetails,
                        offerToken = relevantSubscriptionOfferDetails.offerToken
                    )
                } else {
                    Subscription.WithTrial(
                        tier = mapProductIdToTier(productDetails.productId),
                        recurringPricingPhase = recurringPricingPhase,
                        trialPricingPhase = trialPricingPhase,
                        productDetails = productDetails,
                        offerToken = relevantSubscriptionOfferDetails.offerToken
                    )
                }
            }
    }

    private val ProductDetails.SubscriptionOfferDetails.recurringSubscriptionPricingPhase: RecurringSubscriptionPricingPhase?
        get() = recurringSubscriptionPricingPhases().run {
            when (size) {
                0 -> {
                    LogBuffer.e(
                        LogBuffer.TAG_SUBSCRIPTIONS,
                        "ProductDetails did not have any infinite recurring pricing phases. Current implementation expects 1."
                    )
                    null
                }
                1 -> first()
                else -> {
                    LogBuffer.e(
                        LogBuffer.TAG_SUBSCRIPTIONS,
                        "ProductDetails had $size infinite recurring pricing phases. Current implementation only properly handles 1."
                    )
                    first()
                }
            }
        }

    private val ProductDetails.SubscriptionOfferDetails.trialSubscriptionPricingPhase: TrialSubscriptionPricingPhase?
        get() = trialSubscriptionPricingPhases().run {
            when (size) {
                0 -> null
                1 -> first()
                else -> {
                    LogBuffer.e(
                        LogBuffer.TAG_SUBSCRIPTIONS,
                        "ProductDetails had $size finite recurring pricing phase. Current implementation only properly handles 1."
                    )
                    first()
                }
            }
        }

    private fun ProductDetails.SubscriptionOfferDetails.recurringSubscriptionPricingPhases() =
        subscriptionPricingPhases<RecurringSubscriptionPricingPhase>(SubscriptionPricingPhase.Type.RECURRING)

    private fun ProductDetails.SubscriptionOfferDetails.trialSubscriptionPricingPhases() =
        subscriptionPricingPhases<TrialSubscriptionPricingPhase>(SubscriptionPricingPhase.Type.TRIAL)

    private inline fun <reified T : SubscriptionPricingPhase> ProductDetails.SubscriptionOfferDetails.subscriptionPricingPhases(
        phaseType: SubscriptionPricingPhase.Type
    ) =
        pricingPhases
            .pricingPhaseList
            .map { it.fromPricingPhase() }
            .filterIsInstance<T>()
            .filter { it.phaseType() == phaseType } // Must check the phaseType because a SubscriptionPricingPhase class can implement both Trial and Recurring

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
                "Unable to parse billingPeriod: $billingPeriod"
            )
            null
        }

    fun mapProductIdToTier(productId: String) = when {
        productId.startsWith(PLUS_PRODUCT_BASE) -> SubscriptionTier.PLUS
        productId.startsWith(PATRON_PRODUCT_BASE) -> SubscriptionTier.PATRON
        else -> SubscriptionTier.UNKNOWN
    }
}
