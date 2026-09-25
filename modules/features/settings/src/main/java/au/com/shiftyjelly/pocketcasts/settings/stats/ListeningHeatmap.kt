package au.com.shiftyjelly.pocketcasts.settings.stats

import au.com.shiftyjelly.pocketcasts.compose.stats.HeatLevel
import au.com.shiftyjelly.pocketcasts.models.to.DailyListenedTime
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

fun listeningHeatLevels(
    dailyListenedTime: List<DailyListenedTime>,
    start: LocalDate,
    end: LocalDate,
): Map<LocalDate, HeatLevel> {
    val secondsByDate = dailyListenedTime
        .mapNotNull { entry -> entry.listenDate.toLocalDateOrNull()?.let { date -> date to entry.totalPlayedSeconds } }
        .filter { (date, _) -> date in start..end }
        .toMap()

    val sortedNonZero = secondsByDate.values.filter { it > 0 }.sorted()
    if (sortedNonZero.isEmpty()) {
        return emptyMap()
    }

    val q1 = quantile(sortedNonZero, 0.25)
    val q2 = quantile(sortedNonZero, 0.5)
    val q3 = quantile(sortedNonZero, 0.75)

    return secondsByDate
        .mapValues { (_, seconds) -> heatLevelFor(seconds, q1, q2, q3) }
        .filterValues { it != HeatLevel.None }
}

private fun heatLevelFor(seconds: Double, q1: Double, q2: Double, q3: Double) = when {
    seconds <= 0 -> HeatLevel.None
    seconds <= q1 -> HeatLevel.Low
    seconds <= q2 -> HeatLevel.Medium
    seconds <= q3 -> HeatLevel.High
    else -> HeatLevel.Max
}

private fun quantile(sortedValues: List<Double>, fraction: Double): Double {
    val position = fraction * (sortedValues.size - 1)
    val lowerIndex = floor(position).toInt()
    val upperIndex = ceil(position).toInt()
    val lower = sortedValues[lowerIndex]
    if (lowerIndex == upperIndex) {
        return lower
    }
    val upper = sortedValues[upperIndex]
    return lower + (position - lowerIndex) * (upper - lower)
}

private fun String.toLocalDateOrNull() = runCatching { LocalDate.parse(this) }.getOrNull()
