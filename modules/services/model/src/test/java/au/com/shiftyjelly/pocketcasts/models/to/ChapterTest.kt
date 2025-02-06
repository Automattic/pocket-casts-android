package au.com.shiftyjelly.pocketcasts.models.to

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterTest {
    @Test
    fun `calculate progress`() {
        val chapter = Chapter(
            title = "",
            startTime = 5.seconds,
            endTime = 10.seconds,
            index = 0,
            uiIndex = 1,
        )

        assertEquals(0f, chapter.calculateProgress(1.milliseconds), 0.001f)
        assertEquals(0f, chapter.calculateProgress(5000.milliseconds), 0.001f)
        assertEquals(0.5f, chapter.calculateProgress(7500.milliseconds), 0.001f)
        assertEquals(1f, chapter.calculateProgress(9999.milliseconds), 0.001f)
        assertEquals(0f, chapter.calculateProgress(10000.milliseconds), 0.001f)
    }

    @Test
    fun `calculate remainging time`() {
        val chapter = Chapter(
            title = "",
            startTime = 5.seconds,
            endTime = 70.seconds,
            index = 0,
            uiIndex = 1,
        )

        assertEquals("1m", chapter.remainingTime(5000.milliseconds, playbackSpeed = 1.0, adjustRemainingTimeDuration = false))
        assertEquals("1m", chapter.remainingTime(10000.milliseconds, playbackSpeed = 1.0, adjustRemainingTimeDuration = false))
        assertEquals("59s", chapter.remainingTime(11000.milliseconds, playbackSpeed = 1.0, adjustRemainingTimeDuration = false))
        assertEquals("2s", chapter.remainingTime(68000.milliseconds, playbackSpeed = 1.0, adjustRemainingTimeDuration = false))
        assertEquals("1s", chapter.remainingTime(69000.milliseconds, playbackSpeed = 1.0, adjustRemainingTimeDuration = false))
        assertEquals("0s", chapter.remainingTime(69999.milliseconds, playbackSpeed = 1.0, adjustRemainingTimeDuration = false))
    }

    @Test
    fun `calclucate remainging time using playback speed`() {
        val chapter = Chapter(
            title = "",
            startTime = 0.seconds,
            endTime = 120.seconds,
            index = 0,
            uiIndex = 1,
        )

        assertEquals("4m", chapter.remainingTime(0.seconds, playbackSpeed = 0.5, adjustRemainingTimeDuration = true))
        assertEquals("2m", chapter.remainingTime(0.seconds, playbackSpeed = 1.0, adjustRemainingTimeDuration = true))
        assertEquals("1m", chapter.remainingTime(0.seconds, playbackSpeed = 1.5, adjustRemainingTimeDuration = true))
        assertEquals("1m", chapter.remainingTime(0.seconds, playbackSpeed = 2.0, adjustRemainingTimeDuration = true))

        assertEquals("2m", chapter.remainingTime(75.seconds, playbackSpeed = 0.5, adjustRemainingTimeDuration = true))
        assertEquals("45s", chapter.remainingTime(75.seconds, playbackSpeed = 1.0, adjustRemainingTimeDuration = true))
        assertEquals("30s", chapter.remainingTime(75.seconds, playbackSpeed = 1.5, adjustRemainingTimeDuration = true))
        assertEquals("23s", chapter.remainingTime(75.seconds, playbackSpeed = 2.0, adjustRemainingTimeDuration = true))
    }

    @Test
    fun `ignore remainging time using playback speed`() {
        val chapter = Chapter(
            title = "",
            startTime = 0.seconds,
            endTime = 120.seconds,
            index = 0,
            uiIndex = 1,
        )

        assertEquals("2m", chapter.remainingTime(0.seconds, playbackSpeed = 0.5, adjustRemainingTimeDuration = false))
        assertEquals("2m", chapter.remainingTime(0.seconds, playbackSpeed = 1.0, adjustRemainingTimeDuration = false))
        assertEquals("2m", chapter.remainingTime(0.seconds, playbackSpeed = 1.5, adjustRemainingTimeDuration = false))
        assertEquals("2m", chapter.remainingTime(0.seconds, playbackSpeed = 2.0, adjustRemainingTimeDuration = false))

        assertEquals("45s", chapter.remainingTime(75.seconds, playbackSpeed = 0.5, adjustRemainingTimeDuration = false))
        assertEquals("45s", chapter.remainingTime(75.seconds, playbackSpeed = 1.0, adjustRemainingTimeDuration = false))
        assertEquals("45s", chapter.remainingTime(75.seconds, playbackSpeed = 1.5, adjustRemainingTimeDuration = false))
        assertEquals("45s", chapter.remainingTime(75.seconds, playbackSpeed = 2.0, adjustRemainingTimeDuration = false))
    }
}
