package au.com.shiftyjelly.pocketcasts.utils.extensions

import au.com.shiftyjelly.pocketcasts.utils.DateUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Date.toIsoString(): String {
    val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    isoDateFormat.timeZone = TimeZone.getTimeZone("UTC")
    return isoDateFormat.format(this)
}

// Sending an invalid date to the server can cause json parsing to fail
// For example Date(2841695909714000).switchInvalidForNow() will switch the invalid date "92019-10-04T22:55:14Z" with the current date
fun Date.switchInvalidForNow(): Date {
    val cal = Calendar.getInstance()
    cal.time = this
    val year = cal.get(Calendar.YEAR)
    return if (year < 1970 || year > 9999) Calendar.getInstance().time else this
}

/**
 * Convert date from UTC to the device's local time. Output string in the format 23 April 2020.
 */
fun Date.toLocalizedFormatLongStyle(): String {
    return DateUtil.toLocalizedFormatLongStyle(this)
}

/**
 * Convert date from UTC to the device's local time. Output string in the pattern e.g. EEEE = Monday
 */
fun Date.toLocalizedFormatPattern(pattern: String): String {
    return DateUtil.toLocalizedFormatPattern(this, pattern)
}
