package au.com.shiftyjelly.pocketcasts.models.to

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ChaptersTest {

    @Test
    fun `next chapter returned from selected chapters`() {
        val chapters = initChapters()

        val chapter = chapters.getNextSelectedChapter(150)

        assert(chapter?.title == "3")
    }

    @Test
    fun `prev chapter returned from selected chapters`() {
        val chapters = initChapters()

        val chapter = chapters.getPreviousSelectedChapter(150)

        assert(chapter?.title == "1")
    }

    private fun initChapters() =
        Chapters(
            listOf(
                Chapter("1", 0, 100, selected = true),
                Chapter("2", 101, 200, selected = false),
                Chapter("3", 201, 300, selected = true),
            ),
        )
}
