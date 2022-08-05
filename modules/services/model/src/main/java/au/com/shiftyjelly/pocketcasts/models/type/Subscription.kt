package au.com.shiftyjelly.pocketcasts.models.type

import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.utils.extensions.recurringSubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.utils.extensions.trialSubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import java.time.Period
import java.time.format.DateTimeParseException

sealed interface Subscription {
    val recurringSubscriptionPhase: RecurringSubscriptionPhase
    val trialSubscriptionPhase: TrialSubscriptionPhase?
    val productDetails: ProductDetails
    val shortTitle: String
        get() = productDetails.title.split(" (").first()

    fun numFreeThenPricePerPeriod(res: Resources): String?

    // Simple subscriptions do not have a trial phase
    class Simple(
        override val recurringSubscriptionPhase: RecurringSubscriptionPhase,
        override val productDetails: ProductDetails
    ) : Subscription {
        override val trialSubscriptionPhase = null
        override fun numFreeThenPricePerPeriod(res: Resources): String? = null
    }

    class WithTrial(
        override val recurringSubscriptionPhase: RecurringSubscriptionPhase,
        override val trialSubscriptionPhase: TrialSubscriptionPhase, // override to not be nullable
        override val productDetails: ProductDetails
    ) : Subscription {

        override fun numFreeThenPricePerPeriod(res: Resources): String =
            res.getString(
                recurringSubscriptionPhase.numFreeThenPricePerPeriodRes,
                trialSubscriptionPhase.periodValue(res),
                recurringSubscriptionPhase.formattedPrice
            )
    }

    companion object {
        fun fromProductDetails(productDetails: ProductDetails): Subscription? {

            val recurringPhase = productDetails.recurringSubscriptionPricingPhase?.fromPricingPhase()
            val trialPhase = productDetails.trialSubscriptionPricingPhase?.fromPricingPhase()

            return when {
                recurringPhase !is RecurringSubscriptionPhase -> {
                    // This should never happen. Every subscription is expected to have a recurring phase.
                    LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "unable to convert product details to a subscription")
                    null
                }
                trialPhase is TrialSubscriptionPhase -> WithTrial(
                    recurringSubscriptionPhase = recurringPhase,
                    trialSubscriptionPhase = trialPhase,
                    productDetails = productDetails
                )
                else -> Simple(
                    recurringSubscriptionPhase = recurringPhase,
                    productDetails = productDetails
                )
            }
        }

        private fun PricingPhase.fromPricingPhase(): SubscriptionPhase? =
            try {
                val period = Period.parse(this.billingPeriod)
                when {
                    period.years > 0 -> SubscriptionPhase.Years(this, period)
                    period.months > 0 -> SubscriptionPhase.Months(this, period)
                    period.days > 0 -> SubscriptionPhase.Days(period)
                    else -> null
                }
            } catch (_: DateTimeParseException) {
                LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "Unable to parse billingPeriod: $billingPeriod")
                null
            }
    }
}
