package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
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
    override suspend fun add(episode: BaseEpisode, timeSecs: Int, title: String): Bookmark {
        // Prevent adding more than one bookmark at the same place
        val existingBookmark = bookmarkDao.findByEpisodeTime(podcastUuid = episode.podcastOrSubstituteUuid, episodeUuid = episode.uuid, timeSecs = timeSecs)
        if (existingBookmark != null) {
            return existingBookmark
        }
        val modifiedAt = System.currentTimeMillis()
        val bookmark = Bookmark(
            uuid = UUID.randomUUID().toString(),
            episodeUuid = episode.uuid,
            podcastUuid = episode.podcastOrSubstituteUuid,
            timeSecs = timeSecs,
            createdAt = Date(),
            title = title,
            titleModified = modifiedAt,
            deleted = false,
            deletedModified = modifiedAt,
            syncStatus = SyncStatus.NOT_SYNCED,
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
     * Find all bookmarks for the given episode. The flow will be updated when the bookmarks change.
     */
    override suspend fun findEpisodeBookmarksFlow(episode: BaseEpisode): Flow<List<Bookmark>> {
        return bookmarkDao.findByEpisodeFlow(podcastUuid = episode.podcastOrSubstituteUuid, episodeUuid = episode.uuid)
    }

    /**
     * Mark the bookmark as deleted so it can be synced to other devices.
     */
    override suspend fun deleteToSync(bookmarkUuid: String) {
        bookmarkDao.updateDeleted(
            uuid = bookmarkUuid,
            deleted = true,
            deletedModified = System.currentTimeMillis(),
            syncStatus = SyncStatus.NOT_SYNCED
        )
    }

    /**
     * Remove the bookmark from the database.
     */
    override suspend fun deleteSynced(bookmarkUuid: String) {
        bookmarkDao.deleteByUuid(bookmarkUuid)
    }

    /**
     * Find all bookmarks that need to be synced.
     */
    override fun findBookmarksToSync(): List<Bookmark> {
        return bookmarkDao.findNotSynced()
    }

    /**
     * Upsert the bookmarks from the server. The insert will replace the bookmark if the UUID already exists.
     */
    override suspend fun upsertSynced(bookmark: Bookmark): Bookmark {
        bookmarkDao.insert(bookmark)
        return bookmark
    }
}
