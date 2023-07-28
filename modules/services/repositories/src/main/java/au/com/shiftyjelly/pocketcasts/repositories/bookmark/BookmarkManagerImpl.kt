package au.com.shiftyjelly.pocketcasts.repositories.bookmark

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortType
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class BookmarkManagerImpl @Inject constructor(
    appDatabase: AppDatabase,
) : BookmarkManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val bookmarkDao = appDatabase.bookmarkDao()

    /**
     * Add a bookmark for the given episode.
     */
    override suspend fun add(episode: BaseEpisode, timeSecs: Int, title: String): Bookmark {
        // Prevent adding more than one bookmark at the same place
        val existingBookmark = findByEpisodeTime(episode = episode, timeSecs = timeSecs)
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

    override suspend fun updateTitle(bookmarkUuid: String, title: String) {
        bookmarkDao.updateTitle(
            bookmarkUuid = bookmarkUuid,
            title = title,
            titleModified = System.currentTimeMillis(),
            syncStatus = SyncStatus.NOT_SYNCED
        )
    }

    /**
     * Find the bookmark by its UUID.
     */
    override suspend fun findBookmark(bookmarkUuid: String): Bookmark? {
        return bookmarkDao.findByUuid(bookmarkUuid)
    }

    /**
     * Find the bookmark by episode and time
     */
    override suspend fun findByEpisodeTime(episode: BaseEpisode, timeSecs: Int): Bookmark? {
        return bookmarkDao.findByEpisodeTime(podcastUuid = episode.podcastOrSubstituteUuid, episodeUuid = episode.uuid, timeSecs = timeSecs)
    }

    /**
     * Find all bookmarks for the given episode. The flow will be updated when the bookmarks change.
     */
    override suspend fun findEpisodeBookmarksFlow(
        episode: BaseEpisode,
        sortType: BookmarksSortType,
    ) = when (sortType) {
        BookmarksSortType.DATE_ADDED_NEWEST_TO_OLDEST ->
            bookmarkDao.findByEpisodeOrderCreatedAtFlow(
                podcastUuid = episode.podcastOrSubstituteUuid,
                episodeUuid = episode.uuid,
                isAsc = false,
            )

        BookmarksSortType.DATE_ADDED_OLDEST_TO_NEWEST ->
            bookmarkDao.findByEpisodeOrderCreatedAtFlow(
                podcastUuid = episode.podcastOrSubstituteUuid,
                episodeUuid = episode.uuid,
                isAsc = true,
            )

        BookmarksSortType.TIMESTAMP ->
            bookmarkDao.findByEpisodeOrderTimeFlow(
                podcastUuid = episode.podcastOrSubstituteUuid,
                episodeUuid = episode.uuid,
            )
    }

    /**
     * Find all bookmarks for the given podcast.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun findPodcastBookmarksFlow(
        podcastUuid: String,
    ) = bookmarkDao.findByPodcastFlow(podcastUuid = podcastUuid).flatMapLatest { helper ->
        flowOf(helper.map { it.toBookmark() })
    }

    override suspend fun searchInPodcastByTitle(podcastUuid: String, title: String) =
        bookmarkDao.searchInPodcastByTitle(podcastUuid, "%$title%").map { it.uuid }

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
