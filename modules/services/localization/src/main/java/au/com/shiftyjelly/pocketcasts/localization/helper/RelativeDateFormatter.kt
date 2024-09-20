package au.com.shiftyjelly.pocketcasts.localization.helper

import android.content.Context
import android.content.res.Resources
import android.icu.text.RelativeDateTimeFormatter
import android.text.format.DateUtils
import au.com.shiftyjelly.pocketcasts.localization.R
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Format the date in the following format
 * "Today"
 * "Yesterday"
 * "Monday" if less than six days from now
 * "23 July" in the same year
 * "1 December 2017" in a previous year
 */
class RelativeDateFormatter(val context: Context) {

    private val now: Calendar = Calendar.getInstance()
    private val sixDaysFromNow: Calendar = Calendar.getInstance().apply { add(Calendar.DATE, 6) }
    private val yesterday: Calendar = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
    private val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var relativeDateFormatter: RelativeDateTimeFormatter? = null

    fun format(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date

        // Try to add "Today" and "Yesterday"
        val alternativeString = formatCloseDays(calendar)
        if (alternativeString != null) {
            return alternativeString.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        val format = when {
            calendar.get(Calendar.YEAR) != currentYear -> DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
            calendar.after(now) && calendar.before(sixDaysFromNow) -> DateUtils.FORMAT_SHOW_WEEKDAY
            else -> DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR
        }

        return DateUtils.formatDateTime(context, calendar.timeInMillis, format)
    }

    private fun formatCloseDays(calendar: Calendar): String? {
        val relativeDateFormatter = this.relativeDateFormatter ?: RelativeDateTimeFormatter.getInstance()
        if (this.relativeDateFormatter == null) {
            this.relativeDateFormatter = relativeDateFormatter
        }
        if (DateUtils.isToday(calendar.timeInMillis)) {
            return relativeDateFormatter.format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.DAY)
        } else if (isSameDay(calendar, yesterday)) {
            return relativeDateFormatter.format(RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.AbsoluteUnit.DAY)
        }
        return null
    }

    private fun formatCloseDaysOld(calendar: Calendar, resources: Resources): String? {
        if (DateUtils.isToday(calendar.timeInMillis)) {
            return resources.getString(R.string.today)
        } else if (isSameDay(calendar, yesterday)) {
            return resources.getString(R.string.yesterday)
        }
        return null
    }

    private fun isSameDay(calendarOne: Calendar, calendarTwo: Calendar): Boolean {
        return calendarOne.get(Calendar.YEAR) == calendarTwo.get(Calendar.YEAR) && calendarOne.get(Calendar.DAY_OF_YEAR) == calendarTwo.get(Calendar.DAY_OF_YEAR)
    }
}
