package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID

class BookmarkManagerTest {
    lateinit var appDatabase: AppDatabase
    lateinit var episodeDao: EpisodeDao
    lateinit var bookmarkManager: BookmarkManager

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        bookmarkManager = BookmarkManagerImpl(appDatabase = appDatabase)
        episodeDao = appDatabase.episodeDao()
    }

    @After
    fun tearDown() {
        appDatabase.close()
    }

    @Test
    fun add() {
        val episodeUuid = UUID.randomUUID().toString()
        val podcastUuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = episodeUuid, podcastUuid = podcastUuid, publishedDate = Date())
        episodeDao.insert(episode)

        runTest {
            val bookmark = bookmarkManager.add(episode = episode, timeSecs = 61)
            val foundBookmark = bookmarkManager.findBookmark(bookmarkUuid = bookmark.uuid)
            assertNotNull(foundBookmark)
            assertEquals(episodeUuid, foundBookmark?.episodeUuid)
            assertEquals(podcastUuid, foundBookmark?.podcastUuid)
            assertEquals(61, foundBookmark?.timeSecs)
            assertEquals(Bookmark.SYNC_STATUS_NOT_SYNCED, foundBookmark?.syncStatus)
            assertNotNull(foundBookmark?.createdAt)
            assert(foundBookmark?.deleted == false)
        }
    }

    /**
     * Test findEpisodeBookmarks returns the bookmarks for an episode.
     */
    @Test
    fun findEpisodeBookmarks() {
        val episodeUuid = UUID.randomUUID().toString()
        val podcastUuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = episodeUuid, podcastUuid = podcastUuid, publishedDate = Date())

        runTest {
            val bookmarkOne = bookmarkManager.add(episode = episode, timeSecs = 61)
            val bookmarkTwo = bookmarkManager.add(episode = episode, timeSecs = 122)
            val bookmarks = bookmarkManager.findEpisodeBookmarks(episode).take(1).first()
            assertEquals(2, bookmarks.size)
            val sortedBookmarks = bookmarks.sortedBy { it.timeSecs }
            assertEquals(bookmarkOne.uuid, sortedBookmarks[0].uuid)
            assertEquals(bookmarkTwo.uuid, sortedBookmarks[1].uuid)
        }
    }

    /**
     * Test the deleteToSync sets the bookmark as deleted and not synced, without removing it from the database.
     */
    @Test
    fun deleteToSync() {
        val episodeUuid = UUID.randomUUID().toString()
        val podcastUuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = episodeUuid, podcastUuid = podcastUuid, publishedDate = Date())

        runTest {
            val bookmark = bookmarkManager.add(episode = episode, timeSecs = 61)
            bookmarkManager.deleteToSync(bookmark.uuid)
            val savedBookmark = bookmarkManager.findBookmark(bookmark.uuid)
            assertNotNull(savedBookmark)
            assertEquals(true, savedBookmark?.deleted)
            assertEquals(Bookmark.SYNC_STATUS_NOT_SYNCED, savedBookmark?.syncStatus)
        }
    }

    /**
     * Test the deleteSynced removes the bookmark from the database.
     */
    @Test
    fun deleteSynced() {
        val episodeUuid = UUID.randomUUID().toString()
        val podcastUuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = episodeUuid, podcastUuid = podcastUuid, publishedDate = Date())

        runTest {
            val bookmark = bookmarkManager.add(episode = episode, timeSecs = 61)
            bookmarkManager.deleteSynced(bookmark.uuid)
            val savedBookmark = bookmarkManager.findBookmark(bookmark.uuid)
            assertNull(savedBookmark)
        }
    }
}
