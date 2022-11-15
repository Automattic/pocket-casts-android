package au.com.shiftyjelly.pocketcasts.utils

import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import kotlin.math.roundToInt

object DateUtil {

    const val MILLISECS_PER_DAY = (24 * 60 * 60 * 1000).toLong()

    private val dateTimeFormatterLong = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    fun daysBetweenTwoDates(dateOne: Date, dateTwo: Date): Int {
        val timeDifference = (dateTwo.time - dateOne.time).toDouble()
        val result = timeDifference / MILLISECS_PER_DAY
        return result.roundToInt()
    }

    fun toLocalizedFormatLongStyle(date: Date): String {
        return date
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateTimeFormatterLong)
    }

    fun toLocalizedFormatPattern(date: Date, pattern: String): String {
        return date
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern(pattern))
    }

    fun toEpochMillis(localDateTime: LocalDateTime) = try {
        localDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (e: Exception) {
        Timber.e("Conversion to epoch millis failed [${e.message}]")
        null
    }
}

fun Date.timeIntervalSinceNow(): Long {
    return Date().time - this.time
}
