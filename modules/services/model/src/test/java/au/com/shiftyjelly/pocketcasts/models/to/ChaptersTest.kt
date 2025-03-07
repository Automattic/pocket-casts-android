package au.com.shiftyjelly.pocketcasts.models.to

import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertEquals
import org.junit.Test

class ChaptersTest {
    @Test
    fun `next chapter returned correctly from selected chapters`() {
        val chapters = initChapters()

        val chapter = chapters.getNextSelectedChapter(150.milliseconds)

        assert(chapter?.title == "5")
    }

    @Test
    fun `prev chapter returned correctly from selected chapters`() {
        val chapters = initChapters()

        val chapter = chapters.getPreviousSelectedChapter(350.milliseconds)

        assert(chapter?.title == "1")
    }

    @Test
    fun `skipped chapters duration calculated correctly`() {
        val chapters = initChapters()

        assertEquals(300.milliseconds, chapters.skippedChaptersDuration(0.milliseconds))
        assertEquals(100.milliseconds, chapters.skippedChaptersDuration(301.milliseconds))
        assertEquals(150.milliseconds, chapters.skippedChaptersDuration(251.milliseconds))
    }

    private fun initChapters(): Chapters {
        return Chapters(
            items = listOf(
                Chapter("1", 0.milliseconds, 101.milliseconds, selected = true, index = 0, uiIndex = 1),
                Chapter("2", 101.milliseconds, 201.milliseconds, selected = false, index = 1, uiIndex = 2),
                Chapter("3", 201.milliseconds, 301.milliseconds, selected = false, index = 2, uiIndex = 3),
                Chapter("4", 301.milliseconds, 401.milliseconds, selected = false, index = 3, uiIndex = 4),
                Chapter("5", 401.milliseconds, 500.milliseconds, selected = true, index = 4, uiIndex = 5),
            ),
        )
    }
}
