package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import au.com.shiftyjelly.pocketcasts.servers.extensions.toTimestamp
import com.pocketcasts.service.api.PodcastFolder
import com.pocketcasts.service.api.Record
import com.pocketcasts.service.api.SyncUserFolder
import com.pocketcasts.service.api.dateAddedOrNull
import com.pocketcasts.service.api.record
import com.pocketcasts.service.api.syncUserFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class FoldersSync(
    private val folderManager: FolderManager,
) {
    suspend fun fullSync(serverFolders: List<PodcastFolder>) {
        val localFolders = folderManager.getAll()
        val localUuids = localFolders.map(Folder::uuid)
        val serverUuids = serverFolders.map(PodcastFolder::getFolderUuid)

        val serverMissingUuids = localUuids - serverUuids
        folderManager.deleteAll(serverMissingUuids)

        val syncedFolders = serverFolders.mapNotNull(::toFolder)
        folderManager.upsertAll(syncedFolders)
    }

    suspend fun incrementalData(): List<Record> {
        val folders = folderManager.findFoldersToSync()
        return withContext(Dispatchers.Default) {
            folders.map(::toRecord)
        }
    }

    suspend fun processIncrementalResponse(serverFolders: List<SyncUserFolder>) {
        val folders = serverFolders.mapNotNull(::toFolder)
        val (deletedFolders, existingFolders) = folders.partition(Folder::deleted)
        folderManager.deleteAll(deletedFolders.map(Folder::uuid))
        folderManager.upsertAll(existingFolders)
    }
}

private fun toRecord(localFolder: Folder): Record {
    return record {
        folder = syncUserFolder {
            folderUuid = localFolder.uuid
            isDeleted = localFolder.deleted
            name = localFolder.name
            color = localFolder.color
            sortPosition = localFolder.sortPosition
            podcastsSortType = localFolder.podcastsSortType.serverId
            dateAdded = localFolder.addedDate.toTimestamp()
        }
    }
}

private fun toFolder(serverFolder: SyncUserFolder): Folder? {
    return serverFolder.dateAddedOrNull?.toDate()?.let { dateAdded ->
        Folder(
            uuid = serverFolder.folderUuid,
            name = serverFolder.name,
            color = serverFolder.color,
            addedDate = dateAdded,
            sortPosition = serverFolder.sortPosition,
            podcastsSortType = PodcastsSortType.fromServerId(serverFolder.podcastsSortType),
            deleted = serverFolder.isDeleted,
            syncModified = Folder.SYNC_MODIFIED_FROM_SERVER,
        )
    }
}

private fun toFolder(serverFolder: PodcastFolder): Folder? {
    val dateAdded = serverFolder.dateAdded?.toDate()
    if (serverFolder.folderUuid == null || serverFolder.name == null || dateAdded == null) {
        return null
    }
    return Folder(
        uuid = serverFolder.folderUuid,
        name = serverFolder.name,
        color = serverFolder.color,
        addedDate = dateAdded,
        sortPosition = serverFolder.sortPosition,
        podcastsSortType = PodcastsSortType.fromServerId(serverFolder.podcastsSortType),
        deleted = false,
        syncModified = Folder.SYNC_MODIFIED_FROM_SERVER,
    )
}
