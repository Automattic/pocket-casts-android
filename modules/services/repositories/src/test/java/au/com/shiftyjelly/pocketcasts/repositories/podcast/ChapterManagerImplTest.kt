package au.com.shiftyjelly.pocketcasts.repositories.podcast

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.db.dao.ChapterDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ChapterManagerImplTest {
    private val chapterDao = mock<ChapterDao>()
    private val episodeDao = mock<EpisodeDao>()

    private val chapterManager = ChapterManagerImpl(chapterDao, episodeDao)

    @Test
    fun `observe no chapters`() = runBlocking {
        whenever(chapterDao.observerChaptersForEpisode("id")).thenReturn(flowOf(emptyList()))
        whenever(episodeDao.findByUuid("id")).doSuspendableAnswer { null }

        chapterManager.observerChaptersForEpisode("id").test {
            assertEquals(Chapters(emptyList<Chapter>()), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe single chapter`() = runBlocking {
        val dbChapter = DbChapter(
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = "Title",
            imageUrl = "Image",
            url = "https://pocketcasts.com/",
        )

        whenever(chapterDao.observerChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeDao.findByUuid("id")).doSuspendableAnswer { null }

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = Chapter(
                index = 1,
                title = "Title",
                startTime = 0.milliseconds,
                endTime = 1.milliseconds,
                url = "https://pocketcasts.com/".toHttpUrl(),
                imagePath = "Image",
                selected = true,
            )
            assertEquals(Chapters(listOf(expected)), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe multiple chapters`() = runBlocking {
        val dbChapters = listOf(
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = 1,
                title = "Title 1",
            ),
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 1,
                endTimeMs = 2,
                title = "Title 2",
            ),
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 2,
                endTimeMs = 3,
                title = "Title 3",
            ),
        )

        whenever(chapterDao.observerChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeDao.findByUuid("id")).doSuspendableAnswer { null }

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = listOf(
                Chapter(
                    index = 1,
                    title = "Title 1",
                    startTime = 0.milliseconds,
                    endTime = 1.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 2,
                    title = "Title 2",
                    startTime = 1.milliseconds,
                    endTime = 2.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 3,
                    title = "Title 3",
                    startTime = 2.milliseconds,
                    endTime = 3.milliseconds,
                    selected = true,
                ),
            )
            assertEquals(Chapters(expected), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapter without title`() = runBlocking {
        val dbChapter = DbChapter(
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = null,
            imageUrl = "Image",
            url = "https://pocketcasts.com/",
        )

        whenever(chapterDao.observerChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeDao.findByUuid("id")).doSuspendableAnswer { null }

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = Chapter(
                index = 1,
                title = "",
                startTime = 0.milliseconds,
                endTime = 1.milliseconds,
                url = "https://pocketcasts.com/".toHttpUrl(),
                imagePath = "Image",
                selected = true,
            )
            assertEquals(Chapters(listOf(expected)), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapter without image URL`() = runBlocking {
        val dbChapter = DbChapter(
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = "Title",
            imageUrl = null,
            url = "https://pocketcasts.com/",
        )

        whenever(chapterDao.observerChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeDao.findByUuid("id")).doSuspendableAnswer { null }

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = Chapter(
                index = 1,
                title = "Title",
                startTime = 0.milliseconds,
                endTime = 1.milliseconds,
                url = "https://pocketcasts.com/".toHttpUrl(),
                imagePath = null,
                selected = true,
            )
            assertEquals(Chapters(listOf(expected)), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapter without URL`() = runBlocking {
        val dbChapter = DbChapter(
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = "Title",
            imageUrl = "Image",
            url = null,
        )

        whenever(chapterDao.observerChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeDao.findByUuid("id")).doSuspendableAnswer { null }

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = Chapter(
                index = 1,
                title = "Title",
                startTime = 0.milliseconds,
                endTime = 1.milliseconds,
                url = null,
                imagePath = "Image",
                selected = true,
            )
            assertEquals(Chapters(listOf(expected)), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapter with invalid URL`() = runBlocking {
        val dbChapter = DbChapter(
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = "Title",
            imageUrl = "Image",
            url = "Invalid Url",
        )

        whenever(chapterDao.observerChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeDao.findByUuid("id")).doSuspendableAnswer { null }

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = Chapter(
                index = 1,
                title = "Title",
                startTime = 0.milliseconds,
                endTime = 1.milliseconds,
                url = null,
                imagePath = "Image",
                selected = true,
            )
            assertEquals(Chapters(listOf(expected)), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe deselected chapters`() = runBlocking {
        val dbChapters = listOf(
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = 1,
                title = "Title 1",
            ),
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 1,
                endTimeMs = 2,
                title = "Title 2",
            ),
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 2,
                endTimeMs = 3,
                title = "Title 3",
            ),
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 3,
                endTimeMs = 4,
                title = "Title 4",
            ),
        )

        whenever(chapterDao.observerChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeDao.findByUuid("id")).doSuspendableAnswer { PodcastEpisode("id", publishedDate = Date(), deselectedChapters = ChapterIndices(listOf(1, 4))) }

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = listOf(
                Chapter(
                    index = 1,
                    title = "Title 1",
                    startTime = 0.milliseconds,
                    endTime = 1.milliseconds,
                    selected = false,
                ),
                Chapter(
                    index = 2,
                    title = "Title 2",
                    startTime = 1.milliseconds,
                    endTime = 2.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 3,
                    title = "Title 3",
                    startTime = 2.milliseconds,
                    endTime = 3.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 4,
                    title = "Title 4",
                    startTime = 3.milliseconds,
                    endTime = 4.milliseconds,
                    selected = false,
                ),
            )
            assertEquals(Chapters(expected), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapters with unknown end timestamps`() = runBlocking {
        val dbChapters = listOf(
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = null,
                title = "Title 1",
            ),
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 1,
                endTimeMs = 2,
                title = "Title 2",
            ),
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 2,
                endTimeMs = null,
                title = "Title 3",
            ),
        )

        whenever(chapterDao.observerChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeDao.findByUuid("id")).doSuspendableAnswer { null }

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = listOf(
                Chapter(
                    index = 1,
                    title = "Title 1",
                    startTime = 0.milliseconds,
                    endTime = 1.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 2,
                    title = "Title 2",
                    startTime = 1.milliseconds,
                    endTime = 2.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 3,
                    title = "Title 3",
                    startTime = 2.milliseconds,
                    endTime = Duration.INFINITE,
                    selected = true,
                ),
            )
            assertEquals(Chapters(expected), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapters with invalid timestamps`() = runBlocking {
        val dbChapters = listOf(
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = 3, // end time is larger than next start time
                title = "Title 1",
            ),
            DbChapter(
                episodeUuid = "id",
                startTimeMs = 2,
                endTimeMs = 3,
                title = "Title 2",
            ),
        )

        whenever(chapterDao.observerChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeDao.findByUuid("id")).doSuspendableAnswer { null }

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = listOf(
                Chapter(
                    index = 1,
                    title = "Title 1",
                    startTime = 0.milliseconds,
                    endTime = 2.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 2,
                    title = "Title 2",
                    startTime = 2.milliseconds,
                    endTime = 3.milliseconds,
                    selected = true,
                ),
            )
            assertEquals(Chapters(expected), awaitItem())

            awaitComplete()
        }
    }
}
