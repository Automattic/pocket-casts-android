package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.providers.InMemoryFeatureProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ChaptersTest {
    @Before
    fun setUp() {
        FeatureFlag.initialize(
            listOf(object : InMemoryFeatureProvider() {}),
        )
    }

    @Test
    fun `given feature flag true, then next chapter returned from selected chapters`() {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, true)
        val chapters = initChapters()

        val chapter = chapters.getNextSelectedChapter(150)

        assert(chapter?.title == "5")
    }

    @Test
    fun `given feature flag true, then prev chapter returned from selected chapters`() {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, true)
        val chapters = initChapters()

        val chapter = chapters.getPreviousSelectedChapter(350)

        assert(chapter?.title == "1")
    }

    @Test
    fun `given feature flag false, then next chapter returned from all chapters`() {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, false)
        val chapters = initChapters()

        val chapter = chapters.getNextSelectedChapter(150)

        assert(chapter?.title == "3")
    }

    @Test
    fun `given feature flag false, then prev chapter returned from all chapters`() {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, false)
        val chapters = initChapters()

        val chapter = chapters.getPreviousSelectedChapter(350)

        assert(chapter?.title == "3")
    }

    private fun initChapters(): Chapters {
        return Chapters(
            items = listOf(
                Chapter("1", 0, 100, selected = true),
                Chapter("2", 101, 200, selected = false),
                Chapter("3", 201, 300, selected = false),
                Chapter("4", 301, 400, selected = false),
                Chapter("5", 401, 500, selected = true),
            ),
        )
    }
}
