package au.com.shiftyjelly.pocketcasts.localization.helper

import android.content.Context
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralHours
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralMins
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralMinutes
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSeconds
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSecs

object TimeHelper {

    /**
     * Displays the duration with the unit shortened.
     * For example: 6h
     * 1h 12m
     * 45s
     */
    fun getTimeDurationShortString(timeMs: Long, context: Context?, emptyString: String = "-"): String {
        context ?: return ""

        val seconds = timeMs / 1000
        val hours = seconds / 60 / 60
        val mins = seconds / 60 - (hours * 60)
        val secs = seconds % 60

        var output = ""
        if (hours > 0 && mins == 0L) {
            output = context.getString(R.string.time_short_hours, hours) // "${hours}h"
        } else if (hours > 0) {
            output = context.getString(R.string.time_short_hours_minutes, hours, mins) // "${hours}h ${mins}m"
        } else if (mins > 0) {
            output = context.getString(R.string.time_short_minutes, mins) // "${mins}m"
        } else if (secs > 0 && mins == 0L) {
            output = context.getString(R.string.time_short_seconds, secs) // "${secs}s"
        }
        return output.ifEmpty { emptyString }
    }

    /**
     * Displays the duration with the long units such as minutes shortened to mins.
     * For example: 6 hours
     * 1 hours 12 mins
     * 45 mins
     * 45 secs
     */
    fun getTimeDurationMediumString(timeMs: Int, context: Context?, emptyString: String = "-"): String {
        context ?: return ""

        val seconds = timeMs / 1000
        val hours = seconds.toLong() / 60 / 60
        val mins = seconds.toLong() / 60 - (hours * 60)
        val secs = seconds.toLong() % 60

        val resources = context.resources

        var output = ""
        if (hours > 0 && mins == 0L) {
            output = resources.getStringPluralHours(hours.toInt())
        } else if (hours > 0) {
            output = resources.getStringPluralHours(hours.toInt()) + " " + resources.getStringPluralMins(mins.toInt())
        } else if (mins > 0) {
            output = resources.getStringPluralMins(mins.toInt())
        } else if (secs > 0 && mins == 0L) {
            output = resources.getStringPluralSecs(secs.toInt())
        }
        return if (output.isEmpty()) emptyString else output
    }

    /**
     * Displays the duration in the largest unit.
     * For example:
     * 6 hours
     * 1 hour 12 minutes
     * 45 seconds
     */
    fun getTimeDurationString(timeMs: Long, context: Context?, emptyString: String = "-"): String {
        context ?: return ""

        val seconds = timeMs / 1000
        val hours = seconds / 60 / 60
        val mins = seconds / 60 - (hours * 60)
        val secs = seconds % 60

        val resources = context.resources

        var output = ""
        if (hours > 0 && mins == 0L) {
            output = resources.getStringPluralHours(hours.toInt())
        } else if (hours > 0) {
            output = resources.getStringPluralHours(hours.toInt()) + " " + resources.getStringPluralMinutes(mins.toInt())
        } else if (mins > 0) {
            output = resources.getStringPluralMinutes(mins.toInt())
        } else if (secs > 0 && mins == 0L) {
            output = resources.getStringPluralSeconds(secs.toInt())
        }
        return if (output.isEmpty()) emptyString else output
    }

    fun getTimeLeft(currentTimeMs: Int, durationMs: Long, inProgress: Boolean, context: Context): TimeLeft {
        if (durationMs == 0L) {
            return TimeLeft(text = "-", description = "")
        }
        if (!inProgress || currentTimeMs <= 0) {
            return TimeLeft(
                text = getTimeDurationShortString(durationMs, context),
                description = getTimeDurationString(durationMs, context)
            )
        }
        val remaining = durationMs - currentTimeMs
        return TimeLeft(
            text = context.getString(R.string.time_left, getTimeDurationShortString(remaining, context, emptyString = "0")),
            description = context.getString(R.string.time_left, getTimeDurationString(remaining, context, emptyString = "0"))
        )
    }

    /**
     * Milliseconds to string e.g. 11:43 or 2:18:90
     */
    fun getTimeLeftOnlyNumbers(currentTimeMs: Int, durationMs: Int): String {
        val timeLeftMs: Int = when {
            durationMs <= 0 -> 0
            currentTimeMs <= 0 -> durationMs
            else -> durationMs - currentTimeMs
        }
        return if (timeLeftMs <= 0) formattedMs(0) else "-${formattedMs(timeLeftMs)}"
    }

    /**
     * Milliseconds to string e.g. 11:43 or 2:18:90
     */
    fun formattedMs(ms: Int): String {
        return formattedSeconds(ms.toDouble() / 1000.0)
    }

    fun formattedSeconds(seconds: Double, hoursFormat: String = "%d:%02d:%02d", noHoursFormat: String = "%02d:%02d"): String {
        var secs = seconds
        if (secs < 0) {
            secs = 0.0
        }

        val hours = secs.toLong() / 3600
        val min = secs.toLong() / 60
        val sec = secs.toLong() % 60

        return if (hours > 0) String.format(hoursFormat, hours, min - hours * 60, sec) else String.format(noHoursFormat, min, sec)
    }
}
