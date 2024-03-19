package au.com.shiftyjelly.pocketcasts.models.to

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterTest {
    @Test
    fun calculateProgress() {
        val chapter = Chapter(
            title = "",
            startTime = 5.seconds,
            endTime = 10.seconds,
        )

        assertEquals(0f, chapter.calculateProgress(1.milliseconds), 0.001f)
        assertEquals(0f, chapter.calculateProgress(5000.milliseconds), 0.001f)
        assertEquals(0.5f, chapter.calculateProgress(7500.milliseconds), 0.001f)
        assertEquals(1f, chapter.calculateProgress(10000.milliseconds), 0.001f)
        assertEquals(0f, chapter.calculateProgress(11000.milliseconds), 0.001f)
    }

    @Test
    fun remainingTime() {
        val chapter = Chapter(
            title = "",
            startTime = 5.seconds,
            endTime = 70.seconds,
        )

        assertEquals("1m", chapter.remainingTime(5000.milliseconds))
        assertEquals("1m", chapter.remainingTime(10000.milliseconds))
        assertEquals("59s", chapter.remainingTime(11000.milliseconds))
        assertEquals("2s", chapter.remainingTime(68000.milliseconds))
        assertEquals("1s", chapter.remainingTime(69000.milliseconds))
        assertEquals("0s", chapter.remainingTime(70000.milliseconds))
    }
}
