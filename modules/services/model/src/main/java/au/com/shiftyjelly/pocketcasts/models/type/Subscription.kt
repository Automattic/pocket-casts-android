package au.com.shiftyjelly.pocketcasts.models.type

import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.utils.extensions.recurringSubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.utils.extensions.trialSubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import java.time.Period
import java.time.format.DateTimeParseException

sealed class Subscription private constructor(
    val recurringSubscriptionPhase: RecurringSubscriptionPhase,
    open val trialSubscriptionPhase: TrialSubscriptionPhase?,
    val productDetails: ProductDetails
) {

    val shortTitle = productDetails.title.split(" (").first()

    open fun numFreeThenPricePerPeriod(res: Resources): String? =
        trialSubscriptionPhase?.let { numFreeThenPricePerPeriod(res, it) }

    protected fun numFreeThenPricePerPeriod(
        res: Resources,
        trialSubscriptionPhase: TrialSubscriptionPhase
    ): String =
        res.getString(
            recurringSubscriptionPhase.numFreeThenPricePerPeriodRes,
            trialSubscriptionPhase.periodValue(res),
            recurringSubscriptionPhase.formattedPrice
        )

    class Simple(
        recurringSubscriptionPhase: RecurringSubscriptionPhase,
        productDetails: ProductDetails
    ) : Subscription(recurringSubscriptionPhase, null, productDetails)

    class WithTrial(
        recurringSubscriptionPhase: RecurringSubscriptionPhase,
        override val trialSubscriptionPhase: TrialSubscriptionPhase, // override to not be nullable
        productDetails: ProductDetails
    ) : Subscription(recurringSubscriptionPhase, trialSubscriptionPhase, productDetails) {

        override fun numFreeThenPricePerPeriod(res: Resources): String =
            numFreeThenPricePerPeriod(res, trialSubscriptionPhase)
    }

    companion object {
        fun fromProductDetails(productDetails: ProductDetails): Subscription? {
            val recurringPhase = productDetails.recurringSubscriptionPricingPhase?.fromPricingPhase()
            val trialPhase = productDetails.trialSubscriptionPricingPhase?.fromPricingPhase()
            return when {
                recurringPhase !is RecurringSubscriptionPhase -> null
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
            period()?.let { period ->
                when {
                    period.years > 0 -> SubscriptionPhase.Years(this, period)
                    period.months > 0 -> SubscriptionPhase.Months(this, period)
                    period.days > 0 -> SubscriptionPhase.Days(period)
                    else -> null
                }
            }

        private fun PricingPhase.period(): Period? =
            try {
                Period.parse(this.billingPeriod)
            } catch (_: DateTimeParseException) {
                LogBuffer.e(
                    LogBuffer.TAG_SUBSCRIPTIONS,
                    "Unable to parse billingPeriod: $billingPeriod"
                )
                null
            }
    }
}
