package au.com.shiftyjelly.pocketcasts.models.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.dao.ChapterDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserEpisodeDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import com.squareup.moshi.Moshi
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChapterDaoSelectionTest {
    private lateinit var testDb: AppDatabase
    private lateinit var chapterDao: ChapterDao
    private lateinit var episodeDao: EpisodeDao
    private lateinit var userEpisodeDao: UserEpisodeDao

    @Before
    fun setupDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        chapterDao = testDb.chapterDao()
        episodeDao = testDb.episodeDao()
        userEpisodeDao = testDb.userEpisodeDao()
    }

    @After
    fun closeDb() {
        testDb.close()
    }

    @Test
    fun deselectPodcastEpisodeChapter() = runTest {
        episodeDao.insert(PodcastEpisode(uuid = "id", publishedDate = Date()))

        chapterDao.selectChapter("id", 0, select = false)

        val episode = episodeDao.findByUuid("id")!!
        assertEquals(ChapterIndices(listOf(0)), episode.deselectedChapters)
    }

    @Test
    fun deselectUserEpisodeChapter() = runTest {
        userEpisodeDao.insert(UserEpisode(uuid = "id", publishedDate = Date()))

        chapterDao.selectChapter("id", 0, select = false)

        val episode = userEpisodeDao.findEpisodeByUuid("id")!!
        assertEquals(ChapterIndices(listOf(0)), episode.deselectedChapters)
    }

    @Test
    fun doNotFailWhenDeselectingChapterForNonExistingEpisode() = runTest {
        chapterDao.selectChapter("id", 0, select = false)
    }

    @Test
    fun deselectChapterUsingModificationDate() = runTest {
        episodeDao.insert(PodcastEpisode(uuid = "id", publishedDate = Date()))

        val now = Date()
        chapterDao.selectChapter("id", 0, select = false, modifiedAt = now)

        val episode = episodeDao.findByUuid("id")!!
        assertEquals(now, episode.deselectedChaptersModified)
    }

    @Test
    fun selectChapterBack() = runTest {
        episodeDao.insert(PodcastEpisode(uuid = "id", publishedDate = Date()))
        chapterDao.selectChapter("id", 0, select = false)

        chapterDao.selectChapter("id", 0, select = true)

        val episode = episodeDao.findByUuid("id")!!
        assertEquals(ChapterIndices(emptyList()), episode.deselectedChapters)
    }

    @Test
    fun deselectedChapterIsAddedOnlyOnce() = runTest {
        episodeDao.insert(PodcastEpisode(uuid = "id", publishedDate = Date()))

        repeat(10) {
            chapterDao.selectChapter("id", 0, select = false)
        }

        val episode = episodeDao.findByUuid("id")!!
        assertEquals(ChapterIndices(listOf(0)), episode.deselectedChapters)
    }

    @Test
    fun deselectMultipleChapters() = runTest {
        val chapters = List(10) { it }
        episodeDao.insert(PodcastEpisode(uuid = "id", publishedDate = Date()))

        chapters.forEach {
            chapterDao.selectChapter("id", it, select = false)
        }

        val episode = episodeDao.findByUuid("id")!!
        assertEquals(ChapterIndices(chapters), episode.deselectedChapters)
    }

    @Test
    fun selectMultipleChapters() = runTest {
        val chapters = List(10) { it }
        episodeDao.insert(PodcastEpisode(uuid = "id", publishedDate = Date()))
        chapters.forEach {
            chapterDao.selectChapter("id", it, select = false)
        }

        chapterDao.selectChapter("id", 9, select = true)
        chapterDao.selectChapter("id", 8, select = true)
        chapterDao.selectChapter("id", 7, select = true)

        val episode = episodeDao.findByUuid("id")!!
        assertEquals(ChapterIndices(List(7) { it }), episode.deselectedChapters)
    }
}
