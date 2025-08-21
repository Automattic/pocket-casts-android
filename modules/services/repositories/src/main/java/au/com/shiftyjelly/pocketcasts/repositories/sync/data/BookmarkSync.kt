package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import androidx.room.withTransaction
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import au.com.shiftyjelly.pocketcasts.servers.extensions.toTimestamp
import com.google.protobuf.boolValue
import com.google.protobuf.int32Value
import com.google.protobuf.int64Value
import com.google.protobuf.stringValue
import com.pocketcasts.service.api.BookmarkResponse
import com.pocketcasts.service.api.Record
import com.pocketcasts.service.api.SyncUserBookmark
import com.pocketcasts.service.api.createdAtOrNull
import com.pocketcasts.service.api.isDeletedModifiedOrNull
import com.pocketcasts.service.api.isDeletedOrNull
import com.pocketcasts.service.api.record
import com.pocketcasts.service.api.syncUserBookmark
import com.pocketcasts.service.api.timeOrNull
import com.pocketcasts.service.api.titleModifiedOrNull
import com.pocketcasts.service.api.titleOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class BookmarkSync(
    private val syncManager: SyncManager,
    private val appDatabase: AppDatabase,
) {
    private val bookmarkDao = appDatabase.bookmarkDao()

    suspend fun fullSync() {
        processServerBookmark(
            serverBookmarks = syncManager.getBookmarksOrThrow().bookmarksList,
            getUuid = { bookmark -> bookmark.bookmarkUuid },
            isDeleted = { false },
            applyServerBookmark = { localBookmark, serverBookmark -> localBookmark.applyServerBookmark(serverBookmark) },
        )
    }

    suspend fun processIncrementalResponse(serverBookmarks: List<SyncUserBookmark>) {
        processServerBookmark(
            serverBookmarks = serverBookmarks,
            getUuid = { bookmark -> bookmark.bookmarkUuid },
            isDeleted = { bookmark -> bookmark.isDeletedOrNull?.value == true },
            applyServerBookmark = { localBookmark, serverBookmark -> localBookmark.applyServerBookmark(serverBookmark) },
        )
    }

    suspend fun incrementalData(): List<Record> {
        val bookmarks = bookmarkDao.getAllUnsynced()
        return withContext(Dispatchers.Default) {
            bookmarks.map { localBookmark ->
                record {
                    bookmark = syncUserBookmark {
                        bookmarkUuid = localBookmark.uuid
                        podcastUuid = localBookmark.podcastUuid
                        episodeUuid = localBookmark.episodeUuid
                        time = int32Value {
                            value = localBookmark.timeSecs
                        }
                        createdAt = localBookmark.createdAt.toTimestamp()
                        localBookmark.titleModified?.let { modifiedAt ->
                            title = stringValue {
                                value = localBookmark.title
                            }
                            titleModified = int64Value {
                                value = modifiedAt
                            }
                        }
                        localBookmark.deletedModified?.let { modifiedAt ->
                            isDeleted = boolValue {
                                value = localBookmark.deleted
                            }
                            isDeletedModified = int64Value {
                                value = modifiedAt
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun <T> processServerBookmark(
        serverBookmarks: List<T>,
        getUuid: (T) -> String,
        isDeleted: (T) -> Boolean,
        applyServerBookmark: (Bookmark, T) -> Bookmark,
    ) {
        val deletedBookmarks = serverBookmarks.filter(isDeleted)
        val remainingBookmarks = serverBookmarks - deletedBookmarks
        val remainingBookmarksMap = remainingBookmarks.associateBy(getUuid)

        appDatabase.withTransaction {
            bookmarkDao.deleteAll(deletedBookmarks.map(getUuid))

            val existingBookmarks = bookmarkDao.getAll(remainingBookmarks.map(getUuid))
            val existingBookmarksUuids = existingBookmarks.map(Bookmark::uuid)
            existingBookmarks.forEach { bookmark ->
                val serverBookmark = remainingBookmarksMap[bookmark.uuid] ?: return@forEach
                applyServerBookmark(bookmark, serverBookmark)
            }
            val newBookmarks = remainingBookmarks.mapNotNull { serverBookmark ->
                if (getUuid(serverBookmark) !in existingBookmarksUuids) {
                    applyServerBookmark(Bookmark(uuid = ""), serverBookmark)
                } else {
                    null
                }
            }
            bookmarkDao.upsertAll(existingBookmarks + newBookmarks)
        }
    }
}

private fun Bookmark.applyServerBookmark(serverBookmark: SyncUserBookmark) = apply {
    syncStatus = SyncStatus.SYNCED
    uuid = serverBookmark.bookmarkUuid
    podcastUuid = serverBookmark.podcastUuid
    episodeUuid = serverBookmark.episodeUuid
    serverBookmark.timeOrNull?.value?.let { value ->
        timeSecs = value
    }
    serverBookmark.createdAtOrNull?.toDate()?.let { value ->
        createdAt = value
    }
    serverBookmark.titleModifiedOrNull?.value?.let { modifiedAt ->
        serverBookmark.titleOrNull?.value?.let { value ->
            title = value
            titleModified = modifiedAt
        }
    }
    serverBookmark.isDeletedModifiedOrNull?.value?.let { modifiedAt ->
        serverBookmark.isDeletedOrNull?.value?.let { value ->
            deleted = value
            deletedModified = modifiedAt
        }
    }
}

private fun Bookmark.applyServerBookmark(serverBookmark: BookmarkResponse) = apply {
    syncStatus = SyncStatus.SYNCED
    uuid = serverBookmark.bookmarkUuid
    podcastUuid = serverBookmark.podcastUuid
    episodeUuid = serverBookmark.episodeUuid
    timeSecs = serverBookmark.time
    serverBookmark.createdAtOrNull?.toDate()?.let { value ->
        createdAt = value
    }
    title = serverBookmark.title
}
