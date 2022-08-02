package au.com.shiftyjelly.pocketcasts.settings.util

import android.content.Context
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralDays
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralMonths
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralYears
import au.com.shiftyjelly.pocketcasts.utils.extensions.SubscriptionBillingUnit
import au.com.shiftyjelly.pocketcasts.utils.extensions.toSubscriptionBillingUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Period
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class BillingPeriodHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun mapToBillingDetails(period: Period): BillingDetails = when (period.toSubscriptionBillingUnit()) {
        SubscriptionBillingUnit.YEARS -> BillingDetails(
            subscriptionBillingUnit = SubscriptionBillingUnit.YEARS,
            periodUnit = LR.string.plus_year,
            periodValue = context.resources.getStringPluralYears(period.years),
            renews = LR.string.plus_renews_automatically_yearly,
            hint = LR.string.plus_best_value
        )
        SubscriptionBillingUnit.MONTHS -> BillingDetails(
            subscriptionBillingUnit = SubscriptionBillingUnit.MONTHS,
            periodUnit = LR.string.plus_month,
            periodValue = context.resources.getStringPluralMonths(period.months),
            renews = LR.string.plus_renews_automatically_monthly,
            hint = null
        )
        SubscriptionBillingUnit.DAYS -> BillingDetails(
            subscriptionBillingUnit = SubscriptionBillingUnit.DAYS,
            periodUnit = LR.string.plus_day,
            periodValue = context.resources.getStringPluralDays(period.days),
            renews = null,
            hint = null
        )
        else -> BillingDetails(
            subscriptionBillingUnit = null,
            periodUnit = null,
            periodValue = null,
            renews = null,
            hint = null
        )
    }

    data class BillingDetails(
        val subscriptionBillingUnit: SubscriptionBillingUnit?,
        @StringRes val periodUnit: Int?,
        val periodValue: String?,
        @StringRes val renews: Int?,
        @StringRes val hint: Int?,
    )
}
