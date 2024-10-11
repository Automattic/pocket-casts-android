package au.com.shiftyjelly.pocketcasts.models.type

import android.content.res.Resources
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.android.billingclient.api.ProductDetails
import java.time.Period
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

sealed interface OfferSubscriptionPricingPhase : SubscriptionPricingPhase {
    val chronoUnit: ChronoUnit

    // i.e., 14 offer
    fun numPeriodFree(res: Resources): String =
        res.getString(R.string.profile_amount_free, periodValuePlural(res))

    // i.e., 14 day offer
    fun numPeriodOffer(res: Resources, isTrial: Boolean): String =
        if (isTrial) {
            res.getString(R.string.plus_trial_duration_free_trial, periodValueSingular(res))
        } else {
            res.getString(R.string.plus_offer_duration, periodValueSingular(res))
        }

    fun offerEnd(): String {
        val end = chronoUnit.addTo(ZonedDateTime.now(), periodValue.toLong())
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).format(end)
    }

    override fun priceSlashPeriod(res: Resources): String {
        return when (this) {
            is SubscriptionPricingPhase.Years -> res.getString(R.string.plus_slash_year, this.formattedPrice)
            is SubscriptionPricingPhase.Months -> res.getString(R.string.plus_slash_month, this.formattedPrice)
            else -> { "" }
        }
    }
}

sealed interface RecurringSubscriptionPricingPhase : SubscriptionPricingPhase {
    val priceCurrencyCode: String
        get() = pricingPhase.priceCurrencyCode
    val formattedPrice: String
        get() = pricingPhase.formattedPrice
    val perPeriod: Int
    val renews: Int
    val hint: Int?
    fun pricePerPeriod(res: Resources): String
    override fun priceSlashPeriod(res: Resources): String
    fun thenPriceSlashPeriod(res: Resources): String

    fun toSubscriptionFrequency() = when (this) {
        is SubscriptionPricingPhase.Months -> SubscriptionFrequency.MONTHLY
        is SubscriptionPricingPhase.Years -> SubscriptionFrequency.YEARLY
    }
}

sealed interface SubscriptionPricingPhase {
    val pricingPhase: ProductDetails.PricingPhase

    @get:StringRes val periodResSingular: Int

    @get:StringRes val periodResPlural: Int
    val periodValue: Int
    fun periodValuePlural(res: Resources): String =
        res.getStringPlural(periodValue, periodResSingular, periodResPlural)
    fun periodValueSingular(res: Resources): String =
        "$periodValue ${res.getString(periodResSingular)}"
    fun periodWithDash(res: Resources): String =
        "$periodValue-${res.getString(periodResSingular).replaceFirstChar { it.titlecase(Locale.getDefault()) }}"
    fun slashPeriod(res: Resources): String =
        "/ ${res.getString(periodResSingular)}"
    fun phaseType(): Type = pricingPhase.subscriptionPricingPhaseType

    fun priceSlashPeriod(res: Resources): String

    enum class Type { OFFER, RECURRING, UNKNOWN }

    private val ProductDetails.PricingPhase.subscriptionPricingPhaseType: Type
        get() = when (recurrenceMode) {
            ProductDetails.RecurrenceMode.FINITE_RECURRING -> Type.OFFER
            ProductDetails.RecurrenceMode.INFINITE_RECURRING -> Type.RECURRING
            else -> {
                LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "Unable to determine SubscriptionPricingPhase.Type")
                Type.UNKNOWN
            }
        }

    class Years(
        override val pricingPhase: ProductDetails.PricingPhase,
        private val period: Period,
    ) : RecurringSubscriptionPricingPhase, OfferSubscriptionPricingPhase {
        override val periodValue = period.years
        override val chronoUnit = ChronoUnit.YEARS
        override val periodResSingular = R.string.plus_year
        override val periodResPlural = R.string.years_plural
        override val perPeriod = R.string.profile_per_year
        override val renews = R.string.plus_renews_automatically_yearly
        override val hint = R.string.plus_best_value

        override fun pricePerPeriod(res: Resources): String =
            res.getString(R.string.plus_per_year, pricingPhase.formattedPrice)

        override fun priceSlashPeriod(res: Resources): String =
            res.getString(R.string.plus_slash_year, pricingPhase.formattedPrice)

        override fun thenPriceSlashPeriod(res: Resources): String =
            res.getString(R.string.plus_then_slash_year, pricingPhase.formattedPrice)
    }

    class Months(
        override val pricingPhase: ProductDetails.PricingPhase,
        private val period: Period,
    ) : RecurringSubscriptionPricingPhase, OfferSubscriptionPricingPhase {

        override val periodResSingular = R.string.plus_month
        override val periodResPlural = R.string.months_plural
        override val periodValue = period.months
        override val chronoUnit = ChronoUnit.MONTHS
        override val perPeriod = R.string.profile_per_month
        override val renews = R.string.plus_renews_automatically_monthly
        override val hint = null

        override fun pricePerPeriod(res: Resources): String =
            res.getString(R.string.plus_per_month, pricingPhase.formattedPrice)

        override fun priceSlashPeriod(res: Resources): String =
            res.getString(R.string.plus_slash_month, pricingPhase.formattedPrice)

        override fun thenPriceSlashPeriod(res: Resources): String =
            res.getString(R.string.plus_then_slash_month, pricingPhase.formattedPrice)
    }

    class Days(
        override val pricingPhase: ProductDetails.PricingPhase,
        private val period: Period,
    ) : OfferSubscriptionPricingPhase {
        override val periodResSingular = R.string.plus_day
        override val periodResPlural = R.string.days_plural
        override val periodValue = period.days

        override val chronoUnit = ChronoUnit.DAYS

        init {
            if (phaseType() != Type.OFFER) {
                LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, "Got a phase type of ${phaseType()} for a Days phase, which only extends OfferSubscriptionPricingPhase")
            }
        }
    }
}
