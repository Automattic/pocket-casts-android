package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.db.dao.ChapterDao
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter as Chapter

@RunWith(AndroidJUnit4::class)
class ChapterDaoTest {
    private lateinit var testDb: AppDatabase
    private lateinit var chapterDao: ChapterDao

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        chapterDao = testDb.chapterDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun insertSingleChapter() = runBlocking {
        val chapter = Chapter(
            episodeUuid = "episode-id",
            startTimeMs = 0L,
        )

        chapterDao.replaceAllChapters("episode-id", listOf(chapter))

        val result = chapterDao.findAll()
        assertEquals(listOf(chapter), result)
    }

    @Test
    fun doNotInsertTheSameChapterTwice() = runBlocking {
        val chapter = Chapter(
            episodeUuid = "episode-id",
            startTimeMs = 0L,
        )

        chapterDao.replaceAllChapters("episode-id", listOf(chapter.copy(title = "Title"), chapter))

        val result = chapterDao.findAll()
        assertEquals(listOf(chapter), result)
    }

    @Test
    fun insertMultipleChapters() = runBlocking {
        val chapters = List(10) { index ->
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
            )
        }

        chapterDao.replaceAllChapters("episode-id", chapters)

        val result = chapterDao.findAll()
        assertEquals(chapters, result)
    }

    @Test
    fun doNotInsertChaptersForOtherEpisodes() = runBlocking {
        val id1 = "episode-id-1"
        val id2 = "episode-id-2"

        val chapters = listOf(
            Chapter(
                episodeUuid = id1,
                startTimeMs = 0L,
            ),
            Chapter(
                episodeUuid = id1,
                startTimeMs = 1L,
            ),
            Chapter(
                episodeUuid = id2,
                startTimeMs = 0L,
            ),
        )

        chapterDao.replaceAllChapters(id1, chapters)

        val result = chapterDao.findAll()
        assertEquals(chapters.take(2), result)
    }

    @Test
    fun replaceAllEmbeddedChapters() = runBlocking {
        val chapters1 = List(10) { index ->
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
                isEmbedded = true,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters1)

        val chapters2 = List(5) { index ->
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
                isEmbedded = true,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters2)

        val result = chapterDao.findAll()
        assertEquals(chapters2, result)
    }

    @Test
    fun doNotReplaceEmptyEmbeddedChapters() = runBlocking {
        val chapters = List(10) { index ->
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
                isEmbedded = true,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters)

        chapterDao.replaceAllChapters("episode-id", emptyList())

        val result = chapterDao.findAll()
        assertEquals(chapters, result)
    }

    @Test
    fun replaceNotEmbeddedWithEmbeddedChapters() = runBlocking {
        val chapters1 = List(10) { index ->
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
                isEmbedded = false,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters1)

        val chapters2 = List(5) { index ->
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
                isEmbedded = true,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters2)

        val result = chapterDao.findAll()
        assertEquals(chapters2, result)
    }

    @Test
    fun doNotReplaceEmbeddedWithNotEmbeddedChapters() = runBlocking {
        val chapters1 = List(5) { index ->
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
                isEmbedded = true,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters1)

        val chapters2 = List(10) { index ->
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
                isEmbedded = false,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters2)

        val result = chapterDao.findAll()
        assertEquals(chapters1, result)
    }

    @Test
    fun replaceNotEmbeddedChaptersIfExistingCountIsSmaller() = runBlocking {
        val chapters1 = List(5) { index ->
            Chapter(
                title = "$index-0",
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters1)

        val chapters2 = List(6) { index ->
            Chapter(
                title = "$index-1",
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters2)

        val result = chapterDao.findAll()
        assertEquals(chapters2, result)
    }

    @Test
    fun replaceNotEmbeddedChaptersIfExistingCountIsEqual() = runBlocking {
        val chapters1 = List(5) { index ->
            Chapter(
                title = "$index-0",
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters1)

        val chapters2 = List(5) { index ->
            Chapter(
                title = "$index-1",
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters2)

        val result = chapterDao.findAll()
        assertEquals(chapters2, result)
    }

    @Test
    fun doNotReplaceNotEmbeddedChaptersIfExistingCountIsLarger() = runBlocking {
        val chapters1 = List(6) { index ->
            Chapter(
                title = "$index-0",
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters1)

        val chapters2 = List(5) { index ->
            Chapter(
                title = "$index-1",
                episodeUuid = "episode-id",
                startTimeMs = 0L + index,
            )
        }
        chapterDao.replaceAllChapters("episode-id", chapters2)

        val result = chapterDao.findAll()
        assertEquals(chapters1, result)
    }

    @Test
    fun doNotDeleteChaptersForOtherEpisodes() = runBlocking {
        val id1 = "episode-id-1"
        val id2 = "episode-id-2"

        val chapters1 = listOf(
            Chapter(
                episodeUuid = id1,
                startTimeMs = 0L,
            ),
            Chapter(
                episodeUuid = id1,
                startTimeMs = 1L,
            ),
        )
        val chapters2 = listOf(
            Chapter(
                episodeUuid = id2,
                startTimeMs = 0L,
            ),
            Chapter(
                episodeUuid = id2,
                startTimeMs = 1L,
            ),
        )
        chapterDao.replaceAllChapters(id1, chapters1)
        chapterDao.replaceAllChapters(id2, chapters2)

        val result = chapterDao.findAll()
        assertEquals(chapters1 + chapters2, result)
    }

    @Test
    fun observerChaptersForEpisodeInAscendingOrder() = runBlocking {
        val id1 = "episode-id-1"
        val id2 = "episode-id-2"

        chapterDao.observerChaptersForEpisode(id1).test {
            assertEquals(emptyList<Chapter>(), awaitItem())

            val chaptersEpisode1 = List(3) { index ->
                Chapter(
                    episodeUuid = id1,
                    startTimeMs = 10L + index,
                )
            }
            chapterDao.replaceAllChapters(id1, chaptersEpisode1)
            assertEquals(chaptersEpisode1, awaitItem())

            val chapterEpisode2 = Chapter(
                episodeUuid = id2,
                startTimeMs = 0L,
            )
            chapterDao.replaceAllChapters(id2, listOf(chapterEpisode2))
            expectNoEvents()

            val chapterEpisode1 = Chapter(
                episodeUuid = id1,
                startTimeMs = 0L,
            )
            chapterDao.replaceAllChapters(id1, chaptersEpisode1 + chapterEpisode1)
            assertEquals(listOf(chapterEpisode1) + chaptersEpisode1, awaitItem())
        }
    }
}
