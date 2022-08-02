package au.com.shiftyjelly.pocketcasts.settings.util

import android.content.Context
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralDays
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralMonths
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralYears
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Period
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class BillingPeriodHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun mapToBillingDetails(period: Period): BillingDetails {
        var periodUnit: Int? = null
        var periodValue: String? = null
        var renews: Int? = null
        var hint: Int? = null
        var isMonth = false
        if (period.years > 0) {
            periodUnit = LR.string.plus_year
            periodValue = context.resources.getStringPluralYears(period.years)
            hint = LR.string.plus_best_value
            renews = LR.string.plus_renews_automatically_yearly
            isMonth = false
        } else if (period.months > 0) {
            periodUnit = LR.string.plus_month
            periodValue = context.resources.getStringPluralMonths(period.months)
            renews = LR.string.plus_renews_automatically_monthly
            isMonth = true
        } else if (period.days > 0) {
            periodUnit = LR.string.plus_day
            periodValue = context.resources.getStringPluralDays(period.days)
            isMonth = false
        }
        return BillingDetails(
            periodUnit = periodUnit,
            periodValue = periodValue,
            renews = renews,
            hint = hint,
            isMonth = isMonth
        )
    }
    data class BillingDetails(
        val periodUnit: Int? = null,
        val periodValue: String? = null,
        val renews: Int? = null,
        val hint: Int? = null,
        val isMonth: Boolean = true,
    )
}
