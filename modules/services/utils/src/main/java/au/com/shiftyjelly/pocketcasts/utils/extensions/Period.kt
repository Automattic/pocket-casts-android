package au.com.shiftyjelly.pocketcasts.utils.extensions

import java.time.Period

fun Period.toSubscriptionBillingUnit(): SubscriptionBillingUnit? = when {
    years > 0 -> SubscriptionBillingUnit.YEARS
    months > 0 -> SubscriptionBillingUnit.MONTHS
    days > 0 -> SubscriptionBillingUnit.DAYS
    else -> null
}

enum class SubscriptionBillingUnit { YEARS, MONTHS, DAYS }
