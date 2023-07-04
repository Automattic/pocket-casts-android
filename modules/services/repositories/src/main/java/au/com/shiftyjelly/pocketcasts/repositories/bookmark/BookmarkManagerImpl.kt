package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class BookmarkManagerImpl @Inject constructor(
    appDatabase: AppDatabase
) : BookmarkManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val bookmarkDao = appDatabase.bookmarkDao()

    /**
     * Add a bookmark for the given episode.
     */
    override suspend fun add(episode: BaseEpisode, timeSecs: Int): Bookmark {
        val bookmark = Bookmark(
            uuid = UUID.randomUUID().toString(),
            episodeUuid = episode.uuid,
            podcastUuid = episode.podcastOrSubstituteUuid,
            timeSecs = timeSecs,
            createdAt = Date(),
            syncStatus = Bookmark.SYNC_STATUS_NOT_SYNCED,
            title = ""
        )
        bookmarkDao.insert(bookmark)
        return bookmark
    }

    /**
     * Find the bookmark by its UUID.
     */
    override suspend fun findBookmark(bookmarkUuid: String): Bookmark? {
        return bookmarkDao.findByUuid(bookmarkUuid)
    }

    /**
     * Find all bookmarks for the given episode.
     */
    override suspend fun findEpisodeBookmarks(episode: BaseEpisode): Flow<List<Bookmark>> {
        return bookmarkDao.findByPodcastAndEpisodeFlow(podcastUuid = episode.podcastOrSubstituteUuid, episodeUuid = episode.uuid)
    }

    /**
     * Mark the bookmark as deleted so it can be synced to other devices.
     */
    override suspend fun deleteToSync(bookmarkUuid: String) {
        bookmarkDao.updateDeleted(uuid = bookmarkUuid, deleted = true, syncStatus = Bookmark.SYNC_STATUS_NOT_SYNCED)
    }

    /**
     * Remove the bookmark from the database.
     */
    override suspend fun deleteSynced(bookmarkUuid: String) {
        bookmarkDao.deleteByUuid(bookmarkUuid)
    }
}
