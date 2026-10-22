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
import com.pocketcasts.service.api.passageLocationOrNull
import com.pocketcasts.service.api.passageModifiedOrNull
import com.pocketcasts.service.api.passageOrNull
import com.pocketcasts.service.api.record
import com.pocketcasts.service.api.referenceTimeModifiedOrNull
import com.pocketcasts.service.api.referenceTimeOrNull
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

    suspend fun fullSync(): Boolean {
        val serverBookmarks = syncManager.getBookmarksOrThrow().bookmarksList
        processServerBookmark(
            serverBookmarks = serverBookmarks,
            getUuid = { bookmark -> bookmark.bookmarkUuid },
            isDeleted = { false },
            applyServerBookmark = { localBookmark, serverBookmark -> localBookmark.applyServerBookmark(serverBookmark) },
        )
        return serverBookmarks.isNotEmpty()
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
                        localBookmark.passageModified?.let { modifiedAt ->
                            passage = stringValue {
                                value = localBookmark.passage.orEmpty()
                            }
                            passageLocation = int32Value {
                                value = localBookmark.passageLocation ?: 0
                            }
                            passageModified = int64Value {
                                value = modifiedAt
                            }
                        }
                        localBookmark.referenceTimeModified?.let { modifiedAt ->
                            referenceTime = int32Value {
                                value = localBookmark.referenceTime ?: 0
                            }
                            referenceTimeModified = int64Value {
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
    serverBookmark.passageModifiedOrNull?.value?.let { modifiedAt ->
        val passageValue = serverBookmark.passageOrNull?.value?.takeIf { it.isNotEmpty() }
        passage = passageValue
        passageLocation = passageValue?.let { serverBookmark.passageLocationOrNull?.value }
        passageModified = modifiedAt
    }
    serverBookmark.referenceTimeModifiedOrNull?.value?.let { modifiedAt ->
        referenceTime = serverBookmark.referenceTimeOrNull?.value
        referenceTimeModified = modifiedAt
    }
}

internal fun Bookmark.applyServerBookmark(serverBookmark: BookmarkResponse) = apply {
    val localPassageModified = passageModified
    val localReferenceTimeModified = referenceTimeModified

    uuid = serverBookmark.bookmarkUuid
    podcastUuid = serverBookmark.podcastUuid
    episodeUuid = serverBookmark.episodeUuid
    timeSecs = serverBookmark.time
    serverBookmark.createdAtOrNull?.toDate()?.let { value ->
        createdAt = value
    }
    title = serverBookmark.title

    val serverPassage = serverBookmark.passageOrNull?.value?.takeIf { it.isNotEmpty() }
    val serverPassageModified = serverBookmark.passageModifiedOrNull?.value
    val serverPassageApplied = if (serverPassageModified != null) {
        serverPassageModified >= (localPassageModified ?: Long.MIN_VALUE)
    } else {
        // Legacy rows carry a passage without a modified timestamp; take it when there is no local edit to protect.
        serverPassage != null && localPassageModified == null
    }
    if (serverPassageApplied) {
        passage = serverPassage
        passageLocation = serverPassage?.let { serverBookmark.passageLocationOrNull?.value }
        passageModified = serverPassageModified ?: passageModified
    }

    val serverReferenceTime = serverBookmark.referenceTimeOrNull?.value
    val serverReferenceTimeModified = serverBookmark.referenceTimeModifiedOrNull?.value
    val serverReferenceTimeApplied = if (serverReferenceTimeModified != null) {
        serverReferenceTimeModified >= (localReferenceTimeModified ?: Long.MIN_VALUE)
    } else {
        serverReferenceTime != null && localReferenceTimeModified == null
    }
    if (serverReferenceTimeApplied) {
        referenceTime = serverReferenceTime
        referenceTimeModified = serverReferenceTimeModified ?: referenceTimeModified
    }

    // When a locally-newer passage or reference time is kept, stay unsynced so the local value still uploads.
    val localPassageKept = localPassageModified != null && !serverPassageApplied
    val localReferenceTimeKept = localReferenceTimeModified != null && !serverReferenceTimeApplied
    syncStatus = if (localPassageKept || localReferenceTimeKept) SyncStatus.NOT_SYNCED else SyncStatus.SYNCED
}
