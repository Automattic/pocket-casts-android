package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.di.ModelModule
import au.com.shiftyjelly.pocketcasts.models.di.addTypeConverters
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeDefault
import com.squareup.moshi.Moshi
import java.time.Instant
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BookmarkManagerTest {
    private lateinit var appDatabase: AppDatabase
    private lateinit var episodeDao: EpisodeDao
    private lateinit var bookmarkManager: BookmarkManager

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addTypeConverters(ModelModule.provideRoomConverters(Moshi.Builder().build()))
            .build()
        bookmarkManager = BookmarkManagerImpl(
            appDatabase = appDatabase,
            analyticsTracker = AnalyticsTracker.test(),
        )
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
            val bookmark = bookmarkManager.add(episode = episode, timeSecs = 61, title = "Bookmark Title", BookmarkManager.CreationSource.PLAYER)
            val foundBookmark = bookmarkManager.findBookmark(bookmarkUuid = bookmark.uuid)
            assertNotNull(foundBookmark)
            assertEquals(episodeUuid, foundBookmark?.episodeUuid)
            assertEquals(podcastUuid, foundBookmark?.podcastUuid)
            assertEquals(61, foundBookmark?.timeSecs)
            assertEquals(SyncStatus.NOT_SYNCED, foundBookmark?.syncStatus)
            assertEquals("Bookmark Title", foundBookmark?.title)
            assertNotNull(foundBookmark?.createdAt)
            assert(foundBookmark?.deleted == false)
        }
    }

    /**
     * Test more than one bookmark can't be added for the same episode.
     */
    @Test
    fun addDuplicate() {
        val episodeUuid = UUID.randomUUID().toString()
        val podcastUuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = episodeUuid, podcastUuid = podcastUuid, publishedDate = Date())
        episodeDao.insert(episode)

        runTest {
            val bookmarkOne = bookmarkManager.add(episode = episode, timeSecs = 20, title = "", creationSource = BookmarkManager.CreationSource.PLAYER)
            bookmarkManager.add(episode = episode, timeSecs = 20, title = "", creationSource = BookmarkManager.CreationSource.PLAYER)
            val bookmarks = bookmarkManager.findEpisodeBookmarksFlow(episode, BookmarksSortTypeDefault.DATE_ADDED_NEWEST_TO_OLDEST).take(1).first()
            assertEquals(1, bookmarks.size)
            assertEquals(bookmarkOne.uuid, bookmarks[0].uuid)
        }
    }

    /**
     * Test findEpisodeBookmarksFlow timestamp order returns the bookmarks sorted by timestamp
     */
    @Test
    fun findEpisodeBookmarksOrderTimestamp() {
        val episodeUuid = UUID.randomUUID().toString()
        val podcastUuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = episodeUuid, podcastUuid = podcastUuid, publishedDate = Date())

        runTest {
            val bookmarkOne = bookmarkManager.add(episode = episode, timeSecs = 610, title = "", creationSource = BookmarkManager.CreationSource.PLAYER)
            val bookmarkTwo = bookmarkManager.add(episode = episode, timeSecs = 122, title = "", creationSource = BookmarkManager.CreationSource.PLAYER)

            val bookmarks = bookmarkManager.findEpisodeBookmarksFlow(
                episode = episode,
                sortType = BookmarksSortTypeDefault.TIMESTAMP,
            ).take(1).first()

            assertEquals(bookmarkTwo.uuid, bookmarks[0].uuid)
            assertEquals(bookmarkOne.uuid, bookmarks[1].uuid)
        }
    }

    /**
     * Test findEpisodeBookmarksFlow newest to older order returns newest bookmark first
     */
    @Test
    fun findEpisodeBookmarksOrderNewestToOldest() {
        val episodeUuid = UUID.randomUUID().toString()
        val podcastUuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = episodeUuid, podcastUuid = podcastUuid, publishedDate = Date())

        runTest {
            val bookmarkOne = bookmarkManager.add(episode = episode, timeSecs = 10, title = "", creationSource = BookmarkManager.CreationSource.PLAYER, addedAt = Instant.ofEpochMilli(0))
            val bookmarkTwo = bookmarkManager.add(episode = episode, timeSecs = 20, title = "", creationSource = BookmarkManager.CreationSource.PLAYER, addedAt = Instant.ofEpochMilli(1))

            val bookmarks = bookmarkManager.findEpisodeBookmarksFlow(
                episode = episode,
                sortType = BookmarksSortTypeDefault.DATE_ADDED_NEWEST_TO_OLDEST,
            ).take(1).first()

            assertEquals(bookmarkTwo.uuid, bookmarks[0].uuid)
            assertEquals(bookmarkOne.uuid, bookmarks[1].uuid)
        }
    }

    /**
     * Test findEpisodeBookmarksFlow oldest to newest order returns oldest bookmark first
     */
    @Test
    fun findEpisodeBookmarksOrderOldestToNewest() {
        val episodeUuid = UUID.randomUUID().toString()
        val podcastUuid = UUID.randomUUID().toString()
        val episode = PodcastEpisode(uuid = episodeUuid, podcastUuid = podcastUuid, publishedDate = Date())

        runTest {
            val bookmarkOne = bookmarkManager.add(episode = episode, timeSecs = 10, title = "", creationSource = BookmarkManager.CreationSource.PLAYER, addedAt = Instant.ofEpochMilli(0))
            val bookmarkTwo = bookmarkManager.add(episode = episode, timeSecs = 20, title = "", creationSource = BookmarkManager.CreationSource.PLAYER, addedAt = Instant.ofEpochMilli(1))

            val bookmarks = bookmarkManager.findEpisodeBookmarksFlow(
                episode = episode,
                sortType = BookmarksSortTypeDefault.DATE_ADDED_OLDEST_TO_NEWEST,
            ).take(1).first()

            assertEquals(bookmarkOne.uuid, bookmarks[0].uuid)
            assertEquals(bookmarkTwo.uuid, bookmarks[1].uuid)
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
            val bookmark = bookmarkManager.add(episode = episode, timeSecs = 61, title = "", creationSource = BookmarkManager.CreationSource.PLAYER)
            bookmarkManager.deleteToSync(bookmark.uuid)
            val savedBookmark = bookmarkManager.findBookmark(bookmark.uuid, deleted = true)
            assertNotNull(savedBookmark)
            assertEquals(SyncStatus.NOT_SYNCED, savedBookmark?.syncStatus)
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
            val bookmark = bookmarkManager.add(episode = episode, timeSecs = 61, title = "", creationSource = BookmarkManager.CreationSource.PLAYER)
            bookmarkManager.deleteSynced(bookmark.uuid)
            val savedBookmark = bookmarkManager.findBookmark(bookmark.uuid)
            assertNull(savedBookmark)
        }
    }
}
