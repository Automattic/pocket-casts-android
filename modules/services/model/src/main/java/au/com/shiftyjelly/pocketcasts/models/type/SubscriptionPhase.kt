package au.com.shiftyjelly.pocketcasts.models.type

import android.content.res.Resources
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralDays
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralMonths
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralYears
import com.android.billingclient.api.ProductDetails
import java.time.Period

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

sealed class SubscriptionPhaseImpl private constructor(
    protected val pricingPhase: ProductDetails.PricingPhase,
    protected val period: Period,
    @StringRes override val periodRes: Int,
) : SubscriptionPhase {

    abstract override fun periodValue(res: Resources): String

    class Years(pricingPhase: ProductDetails.PricingPhase, period: Period) :
        SubscriptionPhaseImpl(
            pricingPhase = pricingPhase,
            period = period,
            periodRes = R.string.plus_year,
        ),
        RecurringSubscriptionPhase {

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

    class Months(pricingPhase: ProductDetails.PricingPhase, period: Period) :
        SubscriptionPhaseImpl(
            pricingPhase = pricingPhase,
            period = period,
            periodRes = R.string.plus_month,
        ),
        RecurringSubscriptionPhase {

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

    class Days(pricingPhase: ProductDetails.PricingPhase, period: Period) :
        SubscriptionPhaseImpl(
            pricingPhase = pricingPhase,
            period = period,
            periodRes = R.string.plus_day,
        ),
        TrialSubscriptionPhase {

        override fun periodValue(res: Resources): String = res.getStringPluralDays(period.days)

        override fun numFree(res: Resources): String =
            res.getString(R.string.profile_amount_free, periodValue(res))
    }
}
