package au.com.shiftyjelly.pocketcasts.settings.stats

import au.com.shiftyjelly.pocketcasts.compose.stats.HeatLevel
import au.com.shiftyjelly.pocketcasts.models.to.DailyListenedTime
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningHeatmapTest {
    private val start = LocalDate.parse("2024-06-01")
    private val end = LocalDate.parse("2024-06-30")

    @Test
    fun mapsQuartilesToHeatLevels() {
        val data = listOf(
            DailyListenedTime("2024-06-02", 10.0),
            DailyListenedTime("2024-06-03", 20.0),
            DailyListenedTime("2024-06-04", 30.0),
            DailyListenedTime("2024-06-05", 40.0),
        )

        val result = listeningHeatLevels(data, start, end)

        assertEquals(HeatLevel.Low, result[LocalDate.parse("2024-06-02")])
        assertEquals(HeatLevel.Medium, result[LocalDate.parse("2024-06-03")])
        assertEquals(HeatLevel.High, result[LocalDate.parse("2024-06-04")])
        assertEquals(HeatLevel.Max, result[LocalDate.parse("2024-06-05")])
        assertEquals(4, result.size)
    }

    @Test
    fun excludesZeroOutOfRangeAndUnparsableDays() {
        val data = listOf(
            DailyListenedTime("2024-06-04", 30.0),
            DailyListenedTime("2024-06-06", 0.0),
            DailyListenedTime("2024-05-30", 99.0),
            DailyListenedTime("not-a-date", 50.0),
        )

        val result = listeningHeatLevels(data, start, end)

        assertEquals(setOf(LocalDate.parse("2024-06-04")), result.keys)
    }

    @Test
    fun returnsEmptyWhenNoListening() {
        assertEquals(emptyMap<LocalDate, HeatLevel>(), listeningHeatLevels(emptyList(), start, end))
    }

    @Test
    fun returnsEmptyWhenAllDaysZero() {
        val data = listOf(
            DailyListenedTime("2024-06-02", 0.0),
            DailyListenedTime("2024-06-03", 0.0),
        )

        assertEquals(emptyMap<LocalDate, HeatLevel>(), listeningHeatLevels(data, start, end))
    }
}
