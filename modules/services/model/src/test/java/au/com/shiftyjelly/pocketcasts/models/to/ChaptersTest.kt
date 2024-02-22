package au.com.shiftyjelly.pocketcasts.models.to

import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlagWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ChaptersTest {

    @Mock
    private lateinit var featureFlagWrapper: FeatureFlagWrapper

    @Test
    fun `given feature flag true, then next chapter returned from selected chapters`() {
        whenever(featureFlagWrapper.isEnabled(any())).thenReturn(true)
        val chapters = initChapters()

        val chapter = chapters.getNextSelectedChapter(150)

        assert(chapter?.title == "5")
    }

    @Test
    fun `given feature flag true, then prev chapter returned from selected chapters`() {
        whenever(featureFlagWrapper.isEnabled(any())).thenReturn(true)
        val chapters = initChapters()

        val chapter = chapters.getPreviousSelectedChapter(350)

        assert(chapter?.title == "1")
    }

    @Test
    fun `given feature flag false, then next chapter returned from all chapters`() {
        whenever(featureFlagWrapper.isEnabled(any())).thenReturn(false)
        val chapters = initChapters()

        val chapter = chapters.getNextSelectedChapter(150)

        assert(chapter?.title == "3")
    }

    @Test
    fun `given feature flag false, then prev chapter returned from all chapters`() {
        whenever(featureFlagWrapper.isEnabled(any())).thenReturn(false)
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
            featureFlagWrapper = featureFlagWrapper,
        )
    }
}
