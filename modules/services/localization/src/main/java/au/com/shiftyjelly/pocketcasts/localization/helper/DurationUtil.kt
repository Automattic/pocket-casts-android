package au.com.shiftyjelly.pocketcasts.localization.helper

import android.content.res.Resources
import androidx.annotation.PluralsRes
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * Converts the duration to a readable, user-friendly string.
 *
 * Examples of formatted output include:
 * - "34 seconds"
 * - "1 minute 37 seconds"
 * - "10 hours"
 * - "10 days 14 hours 50 minutes 10 seconds"
 *
 * If the duration is negative or resulting string would be empty, the duration will be displayed in [minUnit].
 *
 * @param resources the [Resources] instance used to fetch pluralized strings.
 * @param maxPartCount the maximum number of consecutive time parts to include in the result.
 * For example, with `maxPartsCount = 2`, "1 day 3 hours 45 minutes" would be shortened to "1 day 3 hours".
 * A time part is defined as a unit of time with a value, such as "1 day" or "3 hours".
 * @param minUnit the smallest unit of time allowed in the formatted output.
 * Units smaller than this value are converted to the next allowed unit.
 * For example, with `minUnit = FriendlyDurationUnit.Minute`, "1 minute 36 seconds" would become "1 minute".
 * @param maxUnit the largest unit of time allowed in the formatted output.
 * Units larger than this value are converted to the next allowed unit.
 * For example, with `maxUnit = FriendlyDurationUnit.Minute`, "2 hours" would become "120 minutes".
 * @param pluralResourceId a function that provides the plural resource ID for a given [FriendlyDurationUnit].
 * This function is used to retrieve the correct pluralized string resource when formatting the duration.
 * By default, it uses basic time scale units such as "days", "hours", "minutes", and "seconds".
 * @return A formatted string representing the duration, with units and values up to the specified
 * constraints.
 */
fun Duration.toFriendlyString(
    resources: Resources,
    maxPartCount: Int = 2,
    minUnit: FriendlyDurationUnit = FriendlyDurationUnit.Second,
    maxUnit: FriendlyDurationUnit = FriendlyDurationUnit.Day,
    pluralResourceId: (FriendlyDurationUnit) -> Int = { it.resourceId },
): String {
    val builder = StringBuilder()
    var usedParts = 0
    var timeLeft = coerceAtLeast(Duration.ZERO)

    val units = FriendlyDurationUnit.reversedEntries.filter { it in minUnit..maxUnit }
    for (unit in units) {
        val wholeUnitDuration = unit.inWholeUnits(timeLeft)
        if (usedParts < maxPartCount && wholeUnitDuration > Duration.ZERO) {
            unit.append(builder, resources, timeLeft, pluralResourceId)
            timeLeft -= wholeUnitDuration
            usedParts++
        }
    }
    if (builder.isEmpty()) {
        minUnit.append(builder, resources, coerceAtLeast(Duration.ZERO), pluralResourceId)
    }
    return builder.toString().trimEnd()
}

enum class FriendlyDurationUnit(
    private val durationUnit: DurationUnit,
    @PluralsRes val resourceId: Int,
    @PluralsRes val shortResourceId: Int,
) {
    Second(
        durationUnit = DurationUnit.SECONDS,
        resourceId = LR.plurals.second,
        shortResourceId = LR.plurals.second_short,
    ),
    Minute(
        durationUnit = DurationUnit.MINUTES,
        resourceId = LR.plurals.minute,
        shortResourceId = LR.plurals.minute_short,
    ),
    Hour(
        durationUnit = DurationUnit.HOURS,
        resourceId = LR.plurals.hour,
        shortResourceId = LR.plurals.hour_short,
    ),
    Day(
        durationUnit = DurationUnit.DAYS,
        resourceId = LR.plurals.day,
        shortResourceId = LR.plurals.day_short,
    ),
    ;

    internal fun append(
        builder: StringBuilder,
        resources: Resources,
        duration: Duration,
        pluralResourceId: (FriendlyDurationUnit) -> Int,
    ) {
        val unitCount = toUnitCount(duration)
        builder
            .append(unitCount)
            .append('\u00a0')
            .append(resources.getQuantityString(pluralResourceId(this), unitCount))
            .append(' ')
    }

    internal fun inWholeUnits(duration: Duration) = toUnitCount(duration).toDuration(durationUnit)

    private fun toUnitCount(duration: Duration) = duration.toInt(durationUnit)

    internal companion object {
        val reversedEntries = FriendlyDurationUnit.entries.reversed()
    }
}
