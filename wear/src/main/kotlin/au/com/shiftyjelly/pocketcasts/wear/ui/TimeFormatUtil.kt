package au.com.shiftyjelly.pocketcasts.wear.ui

import android.content.Context
import android.text.format.DateUtils
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * Formats a refresh timestamp for display in WearOS settings using absolute time.
 *
 * This function uses absolute time formatting (e.g., "3:45 PM") rather than relative time
 * (e.g., "2 hours ago") because WearOS displays prioritize glanceability on small screens.
 * Absolute times are faster to read and understand at a glance compared to calculating
 * relative durations.
 *
 * Display modes:
 * - **Very recent** (< 2 minutes): Shows "Just now" for freshly completed refreshes
 * - **Same day**: Shows time only (e.g., "3:45 PM" or "15:45") for refreshes earlier today
 * - **Different day**: Shows abbreviated date and time (e.g., "Feb 8, 3:45 PM")
 *
 * Localization:
 * - Uses Android's [DateUtils.formatDateTime] which automatically respects device locale settings
 * - Time format (12/24-hour) follows the device's system preference
 * - Date formatting follows locale conventions (MM/DD vs DD/MM, month names in local language)
 * - RTL languages are handled correctly by the Android framework
 * - The wrapper string ("Last refresh: %s") is defined in string resources and will be
 *   translated via GlotPress for all supported languages
 *
 * @param date The timestamp of the last refresh
 * @param context Android context for accessing resources and system settings
 * @return A localized, formatted string representing the refresh time
 *
 * @see android.text.format.DateUtils.formatDateTime
 * @see au.com.shiftyjelly.pocketcasts.localization.R.string.profile_last_refresh_at
 * @see au.com.shiftyjelly.pocketcasts.localization.R.string.profile_just_now
 */
fun formatRefreshTime(date: Date, context: Context): String {
    val now = System.currentTimeMillis()
    val timeDiff = now - date.time

    if (timeDiff < 2 * DateUtils.MINUTE_IN_MILLIS) {
        return context.getString(LR.string.profile_just_now)
    }

    if (DateUtils.isToday(date.time)) {
        return DateUtils.formatDateTime(
            context,
            date.time,
            DateUtils.FORMAT_SHOW_TIME,
        )
    }

    return DateUtils.formatDateTime(
        context,
        date.time,
        DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_ABBREV_MONTH or
            DateUtils.FORMAT_SHOW_TIME or
            DateUtils.FORMAT_NO_YEAR,
    )
}
