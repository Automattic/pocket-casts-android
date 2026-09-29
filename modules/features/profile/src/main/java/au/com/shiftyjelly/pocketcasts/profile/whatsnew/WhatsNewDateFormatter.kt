package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import android.content.Context
import android.text.format.DateFormat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal class WhatsNewDateFormatter(
    private val todayLabel: String,
    private val monthDay: DateTimeFormatter,
    private val monthDayYear: DateTimeFormatter,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun format(instant: Instant): String {
        val today = LocalDate.now(clock)
        val date = instant.atZone(clock.zone).toLocalDate()
        return when {
            date == today -> todayLabel
            date.year == today.year -> monthDay.format(date)
            else -> monthDayYear.format(date)
        }
    }

    companion object {
        fun create(context: Context, locale: Locale = Locale.getDefault()) = WhatsNewDateFormatter(
            todayLabel = context.getString(LR.string.today),
            monthDay = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale),
            monthDayYear = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMdyyyy"), locale),
        )
    }
}
