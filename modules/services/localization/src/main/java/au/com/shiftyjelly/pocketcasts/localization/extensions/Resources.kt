package au.com.shiftyjelly.pocketcasts.localization.extensions

import android.content.res.Resources
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import kotlin.math.roundToInt

fun Resources.getStringPlural(count: Int, @StringRes singular: Int, @StringRes plural: Int): String {
    return if (count == 1) getString(singular) else getString(plural, count)
}

fun Resources.getStringPluralPodcasts(count: Int): String {
    return getStringPlural(count, R.string.podcasts_singular, R.string.podcasts_plural)
}

fun Resources.getStringPluralPodcastsSelected(count: Int): String {
    return getStringPlural(count, R.string.podcasts_chosen_singular, R.string.podcasts_chosen_plural)
}

fun Resources.getStringPluralEpisodes(count: Int): String {
    return getStringPlural(count, R.string.episodes_singular, R.string.episodes_plural)
}

fun Resources.getStringPluralSeconds(count: Int): String {
    return getStringPlural(count, R.string.seconds_singular, R.string.seconds_plural)
}

fun Resources.getStringPluralMinutes(count: Int): String {
    return getStringPlural(count, R.string.minutes_singular, R.string.minutes_plural)
}

fun Resources.getStringPluralMins(count: Int): String {
    return getStringPlural(count, R.string.minutes_short_singular, R.string.minutes_short_plural)
}

fun Resources.getStringPluralSecs(count: Int): String {
    return getStringPlural(count, R.string.seconds_short_singular, R.string.seconds_short_plural)
}

fun Resources.getStringPluralHours(count: Int): String {
    return getStringPlural(count, R.string.hours_singular, R.string.hours_plural)
}

fun Resources.getStringPluralDays(count: Int): String {
    return getStringPlural(count, R.string.days_singular, R.string.days_plural)
}

fun Resources.getStringPluralMonths(count: Int): String {
    return getStringPlural(count, R.string.months_singular, R.string.months_plural)
}

fun Resources.getStringPluralYears(count: Int): String {
    return getStringPlural(count, R.string.years_singular, R.string.years_plural)
}

fun Resources.getStringPluralDaysMonthsOrYears(days: Int): String {
    return when {
        days <= 30 -> getStringPluralDays(days)
        days < 365 -> {
            val months = (days / 30.0).roundToInt()
            getStringPluralMonths(months)
        }
        else -> {
            val years = (days / 365.0).roundToInt()
            getStringPluralYears(years)
        }
    }
}

fun Resources.getStringPluralSecondsMinutesHoursDaysOrYears(timeLeftMs: Long): String {
    val seconds = timeLeftMs / 1000L
    // less than 1 minute
    if (seconds <= 60L) {
        return getStringPluralSeconds(seconds.toInt())
    }
    // less than 1 hour
    val minutes = seconds / 60L
    if (minutes <= 60L) {
        val minutesRounded = (seconds / 60.0).toInt()
        return getStringPluralMinutes(minutesRounded)
    }
    // less than 1 day
    val hours = minutes / 60L
    if (hours <= 24L) {
        val hoursRounded = (minutes / 60.0).toInt()
        return getStringPluralHours(hoursRounded)
    }
    // less than 1 year
    val days = hours / 24L
    if (days <= 365L) {
        val daysRounded = (hours / 24.0).toInt()
        return getStringPluralDays(daysRounded)
    }

    val years = (days / 365.0).toInt()
    return getStringPluralYears(years)
}
