package au.com.shiftyjelly.pocketcasts.models.type

import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralDays
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralMonths
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralYears
import com.android.billingclient.api.ProductDetails
import java.time.Period

sealed interface SubscriptionPhase {
    val periodRes: Int
    fun periodValue(res: Resources): String

    class Years(
        private val pricingPhase: ProductDetails.PricingPhase,
        private val period: Period
    ) : RecurringSubscriptionPhase {

        override val periodRes = R.string.plus_year
        override val formattedPrice = pricingPhase.formattedPrice
        override val numFreeThenPricePerPeriodRes = R.string.plus_trial_then_slash_year
        override val renews = R.string.plus_renews_automatically_yearly
        override val hint = R.string.plus_best_value

        override fun periodValue(res: Resources): String =
            res.getStringPluralYears(period.years)

        override fun pricePerPeriod(res: Resources): String =
            res.getString(R.string.plus_per_year, pricingPhase.formattedPrice)

        override fun priceSlashPeriod(res: Resources): String =
            res.getString(R.string.plus_slash_year, pricingPhase.formattedPrice)

        override fun thenPriceSlashPeriod(res: Resources): String =
            res.getString(R.string.plus_then_slash_year, pricingPhase.formattedPrice)
    }

    class Months(
        private val pricingPhase: ProductDetails.PricingPhase,
        private val period: Period
    ) : RecurringSubscriptionPhase {

        override val periodRes = R.string.plus_month
        override val formattedPrice = pricingPhase.formattedPrice
        override val numFreeThenPricePerPeriodRes = R.string.plus_trial_then_slash_month
        override val renews = R.string.plus_renews_automatically_monthly
        override val hint = null

        override fun periodValue(res: Resources): String =
            res.getStringPluralMonths(period.months)

        override fun pricePerPeriod(res: Resources): String =
            res.getString(R.string.plus_per_month, pricingPhase.formattedPrice)

        override fun priceSlashPeriod(res: Resources): String =
            res.getString(R.string.plus_slash_month, pricingPhase.formattedPrice)

        override fun thenPriceSlashPeriod(res: Resources): String =
            res.getString(R.string.plus_then_slash_month, pricingPhase.formattedPrice)
    }

    class Days(private val period: Period) : TrialSubscriptionPhase {

        override val periodRes = R.string.plus_day

        override fun periodValue(res: Resources): String = res.getStringPluralDays(period.days)
    }
}

interface TrialSubscriptionPhase : SubscriptionPhase {
    fun numFree(res: Resources): String =
        res.getString(R.string.profile_amount_free, periodValue(res))
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
