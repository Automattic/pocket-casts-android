package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChaptersTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun `given feature flag true, then next chapter returned from selected chapters`() {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, true)
        val chapters = initChapters()

        val chapter = chapters.getNextSelectedChapter(150.milliseconds)

        assert(chapter?.title == "5")
    }

    @Test
    fun `given feature flag true, then prev chapter returned from selected chapters`() {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, true)
        val chapters = initChapters()

        val chapter = chapters.getPreviousSelectedChapter(350.milliseconds)

        assert(chapter?.title == "1")
    }

    @Test
    fun `given feature flag false, then next chapter returned from all chapters`() {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, false)
        val chapters = initChapters()

        val chapter = chapters.getNextSelectedChapter(150.milliseconds)

        assert(chapter?.title == "3")
    }

    @Test
    fun `given feature flag false, then prev chapter returned from all chapters`() {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, false)
        val chapters = initChapters()

        val chapter = chapters.getPreviousSelectedChapter(350.milliseconds)

        assert(chapter?.title == "3")
    }

    @Test
    fun `given feature flag true, then calculate skipped chapters duration`() {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, true)
        val chapters = initChapters()

        assertEquals(300.milliseconds, chapters.skippedChaptersDuration(0.milliseconds))
        assertEquals(100.milliseconds, chapters.skippedChaptersDuration(301.milliseconds))
        assertEquals(150.milliseconds, chapters.skippedChaptersDuration(251.milliseconds))
    }

    @Test
    fun `given feature flag false, then do not calculate skipped chapters duration`() {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, false)
        val chapters = initChapters()

        assertEquals(Duration.ZERO, chapters.skippedChaptersDuration(0.milliseconds))
    }

    private fun initChapters(): Chapters {
        return Chapters(
            items = listOf(
                Chapter("1", 0.milliseconds, 101.milliseconds, selected = true),
                Chapter("2", 101.milliseconds, 201.milliseconds, selected = false),
                Chapter("3", 201.milliseconds, 301.milliseconds, selected = false),
                Chapter("4", 301.milliseconds, 401.milliseconds, selected = false),
                Chapter("5", 401.milliseconds, 500.milliseconds, selected = true),
            ),
        )
    }
}
