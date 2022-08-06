package au.com.shiftyjelly.pocketcasts.models.type

import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.utils.extensions.recurringSubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.utils.extensions.trialSubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import java.time.Period
import java.time.format.DateTimeParseException

sealed interface Subscription {
    val recurringPricingPhase: RecurringSubscriptionPricingPhase
    val trialPricingPhase: TrialSubscriptionPricingPhase?
    val productDetails: ProductDetails
    val shortTitle: String
        get() = productDetails.title.split(" (").first()

    fun numFreeThenPricePerPeriod(res: Resources): String?

    // Simple subscriptions do not have a trial phase
    class Simple(
        override val recurringPricingPhase: RecurringSubscriptionPricingPhase,
        override val productDetails: ProductDetails
    ) : Subscription {
        override val trialPricingPhase = null
        override fun numFreeThenPricePerPeriod(res: Resources): String? = null
    }

    class WithTrial(
        override val recurringPricingPhase: RecurringSubscriptionPricingPhase,
        override val trialPricingPhase: TrialSubscriptionPricingPhase, // override to not be nullable
        override val productDetails: ProductDetails
    ) : Subscription {

        override fun numFreeThenPricePerPeriod(res: Resources): String {
            val stringRes = when (recurringPricingPhase) {
                is SubscriptionPricingPhase.Years -> R.string.plus_trial_then_slash_year
                is SubscriptionPricingPhase.Months -> R.string.plus_trial_then_slash_month
            }
            return res.getString(
                stringRes,
                trialPricingPhase.periodValue(res),
                recurringPricingPhase.formattedPrice
            )
        }
    }

    companion object {
        fun fromProductDetails(productDetails: ProductDetails): Subscription? {

            val recurringPhase = productDetails.recurringSubscriptionPricingPhase?.fromPricingPhase()
            val trialPhase = productDetails.trialSubscriptionPricingPhase?.fromPricingPhase()

            return when {
                recurringPhase !is RecurringSubscriptionPricingPhase -> {
                    // This should never happen. Every subscription is expected to have a recurring phase.
                    LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "unable to convert product details to a subscription")
                    null
                }
                trialPhase is TrialSubscriptionPricingPhase -> WithTrial(
                    recurringPricingPhase = recurringPhase,
                    trialPricingPhase = trialPhase,
                    productDetails = productDetails
                )
                else -> Simple(
                    recurringPricingPhase = recurringPhase,
                    productDetails = productDetails
                )
            }
        }

        private fun PricingPhase.fromPricingPhase(): SubscriptionPricingPhase? =
            try {
                val period = Period.parse(this.billingPeriod)
                when {
                    period.years > 0 -> SubscriptionPricingPhase.Years(this, period)
                    period.months > 0 -> SubscriptionPricingPhase.Months(this, period)
                    period.days > 0 -> SubscriptionPricingPhase.Days(period)
                    else -> null
                }
            } catch (_: DateTimeParseException) {
                LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "Unable to parse billingPeriod: $billingPeriod")
                null
            }
    }
}
