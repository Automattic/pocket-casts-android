package au.com.shiftyjelly.pocketcasts.wear.ui

import android.content.Context
import android.text.format.DateUtils
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * Formats a refresh timestamp for display in WearOS settings.
 *
 * Uses absolute time formatting (e.g., "3:45 PM") rather than relative time (e.g., "2 hours ago")
 * because absolute times are faster to read at a glance on small WearOS screens.
 *
 * Display modes:
 * - **Very recent** (< 2 minutes): "Just now"
 * - **Same day**: Time only (e.g., "3:45 PM" or "15:45")
 * - **Different day**: Abbreviated date and time (e.g., "Feb 8, 3:45 PM")
 *
 * Time format (12/24-hour) and date formatting follow device locale settings.
 *
 * @param date The timestamp of the last refresh
 * @param context Android context for accessing resources and system settings
 * @return A localized, formatted string representing the refresh time
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
