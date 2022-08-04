package au.com.shiftyjelly.pocketcasts.models.type

import android.content.res.Resources
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralDays
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralMonths
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralYears
import au.com.shiftyjelly.pocketcasts.utils.extensions.recurringSubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.utils.extensions.trialSubscriptionPricingPhase
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import java.time.Period
import java.time.format.DateTimeParseException
import au.com.shiftyjelly.pocketcasts.localization.R as LR

data class Subscription(
    val recurringSubscriptionPhase: RecurringSubscriptionPhase,
    val trialSubscriptionPhase: TrialSubscriptionPhase?,
    val productDetails: ProductDetails
) {

    val shortTitle = productDetails.title.split(" (").first()

    fun numFreeThenPricePerPeriod(res: Resources): String? =
        if (trialSubscriptionPhase == null) {
            null
        } else {
            res.getString(
                recurringSubscriptionPhase.numFreeThenPricePerPeriodRes,
                trialSubscriptionPhase.periodValue(res),
                recurringSubscriptionPhase.formattedPrice
            )
        }

    companion object {
        fun fromProductDetails(productDetails: ProductDetails): Subscription? {
            val recurringPhase = productDetails.recurringSubscriptionPricingPhase?.let {
                SubscriptionPricingPhase.fromPricingPhase(it)
            }
            val trialPhase = productDetails.trialSubscriptionPricingPhase?.let {
                SubscriptionPricingPhase.fromPricingPhase(it)
            }

            return if (recurringPhase is RecurringSubscriptionPhase &&
                (trialPhase == null || trialPhase is TrialSubscriptionPhase)
            ) {
                Subscription(
                    recurringSubscriptionPhase = recurringPhase,
                    trialSubscriptionPhase = trialPhase as TrialSubscriptionPhase?,
                    productDetails = productDetails
                )
            } else {
                LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "product details did not have valid pricing phases")
                null
            }
        }
    }
}

interface SubscriptionPhase {
    val periodRes: Int
    fun periodValue(res: Resources): String
}

interface TrialSubscriptionPhase : SubscriptionPhase {
    fun numFree(res: Resources): String
}

interface RecurringSubscriptionPhase : SubscriptionPhase {
    val formattedPrice: String
    val numFreeThenPricePerPeriodRes: Int
    val renews: Int
    val hint: Int?
    fun pricePerPeriod(res: Resources): String
    fun priceSlashPeriod(res: Resources): String
    fun thenPriceSlashPeriod(res: Resources): String
}

sealed class SubscriptionPricingPhase(
    protected val pricingPhase: ProductDetails.PricingPhase,
    protected val period: Period,
    @StringRes override val periodRes: Int,
) : SubscriptionPhase {

    abstract override fun periodValue(res: Resources): String

    class Years(pricingPhase: PricingPhase, period: Period) :
        SubscriptionPricingPhase(
            pricingPhase = pricingPhase,
            period = period,
            periodRes = LR.string.plus_year,
        ),
        RecurringSubscriptionPhase {

        override val formattedPrice = pricingPhase.formattedPrice
        override val numFreeThenPricePerPeriodRes = LR.string.plus_trial_then_slash_year
        override val renews = LR.string.plus_renews_automatically_yearly
        override val hint = LR.string.plus_best_value

        override fun periodValue(res: Resources): String =
            res.getStringPluralYears(period.years)

        override fun pricePerPeriod(res: Resources): String =
            res.getString(LR.string.plus_per_year, pricingPhase.formattedPrice)

        override fun priceSlashPeriod(res: Resources): String =
            res.getString(LR.string.plus_slash_year, pricingPhase.formattedPrice)

        override fun thenPriceSlashPeriod(res: Resources): String =
            res.getString(LR.string.plus_then_slash_year, pricingPhase.formattedPrice)
    }

    class Months(pricingPhase: PricingPhase, period: Period) :
        SubscriptionPricingPhase(
            pricingPhase = pricingPhase,
            period = period,
            periodRes = LR.string.plus_month,
        ),
        RecurringSubscriptionPhase {

        override val formattedPrice = pricingPhase.formattedPrice
        override val numFreeThenPricePerPeriodRes = LR.string.plus_trial_then_slash_month
        override val renews = LR.string.plus_renews_automatically_monthly
        override val hint = null

        override fun periodValue(res: Resources): String =
            res.getStringPluralMonths(period.months)

        override fun pricePerPeriod(res: Resources): String =
            res.getString(LR.string.plus_per_month, pricingPhase.formattedPrice)

        override fun priceSlashPeriod(res: Resources): String =
            res.getString(LR.string.plus_slash_month, pricingPhase.formattedPrice)

        override fun thenPriceSlashPeriod(res: Resources): String =
            res.getString(LR.string.plus_then_slash_month, pricingPhase.formattedPrice)
    }

    class Days(pricingPhase: PricingPhase, period: Period) :
        SubscriptionPricingPhase(
            pricingPhase = pricingPhase,
            period = period,
            periodRes = LR.string.plus_day,
        ),
        TrialSubscriptionPhase {

        override fun periodValue(res: Resources): String = res.getStringPluralDays(period.days)

        override fun numFree(res: Resources): String =
            res.getString(LR.string.profile_amount_free, periodValue(res))
    }

    companion object {

        fun fromPricingPhase(pricingPhase: PricingPhase): SubscriptionPricingPhase? =
            pricingPhase.period()?.let { period ->
                when {
                    period.years > 0 -> Years(pricingPhase, period)
                    period.months > 0 -> Months(pricingPhase, period)
                    period.days > 0 -> Days(pricingPhase, period)
                    else -> null
                }
            }

        private fun PricingPhase.period(): Period? =
            try {
                Period.parse(this.billingPeriod)
            } catch (_: DateTimeParseException) {
                LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "Unable to parse billingPeriod: $billingPeriod")
                null
            }
    }
}
