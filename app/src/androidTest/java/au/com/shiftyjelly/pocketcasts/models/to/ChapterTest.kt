package au.com.shiftyjelly.pocketcasts.models.to

import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterTest {

    @Test
    fun remainingTime() {
        val chapter = Chapter(
            title = "",
            startTime = 5000,
            endTime = 70000
        )

        assertEquals("1m", chapter.remainingTime(5000))
        assertEquals("59s", chapter.remainingTime(11000))
        assertEquals("2s", chapter.remainingTime(68000))
        assertEquals("0s", chapter.remainingTime(70000))
    }

    @Test
    fun calculateProgress() {
        val chapter = Chapter(
            title = "",
            startTime = 5000,
            endTime = 10000
        )

        assertEquals(0f, chapter.calculateProgress(1))
        assertEquals(0f, chapter.calculateProgress(5000))
        assertEquals(0.5f, chapter.calculateProgress(7500))
        assertEquals(1f, chapter.calculateProgress(10000))
        assertEquals(0f, chapter.calculateProgress(11000))
    }
}
