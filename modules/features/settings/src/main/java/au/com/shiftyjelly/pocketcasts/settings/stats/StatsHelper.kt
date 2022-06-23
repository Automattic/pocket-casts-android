package au.com.shiftyjelly.pocketcasts.settings.stats

import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralDays
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralHours
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralMins
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSeconds
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralSecs

object StatsHelper {

    /**
     * Convert the listening stats from seconds to a friendly string.
     * For example: 34 seconds, 1 min 37 secs, 10 hours
     */
    fun secondsToFriendlyString(seconds: Long, resources: Resources): String {
        val days = seconds / 86400
        val hours = seconds / 3600 - days * 24
        val mins = seconds / 60 - hours * 60 - days * 24 * 60
        val secs = seconds % 60

        return buildString {
            if (days > 0) {
                append(resources.getStringPluralDays(days.toInt()))
                append(" ")
            }

            if (hours > 0) {
                append(resources.getStringPluralHours(hours.toInt()))
                append(" ")
            }

            if (mins > 0 && days < 1) {
                append(resources.getStringPluralMins(mins.toInt()))
                append(" ")
            }

            if (mins > 0 && hours < 1 && days < 1) {
                append(resources.getStringPluralSecs(secs.toInt()))
            }

            if (isEmpty()) {
                append(resources.getStringPluralSeconds(secs.toInt()))
            }
        }.trim()
    }
}
