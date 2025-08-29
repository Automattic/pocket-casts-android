package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

interface FolderManager {

    suspend fun create(name: String, color: Int, podcastsSortType: PodcastsSortType, podcastUuids: List<String>): Folder
    suspend fun delete(folder: Folder)
    suspend fun deleteAll()
    suspend fun upsertSynced(folder: Folder): Folder
    suspend fun deleteSynced(folderUuid: String)
    suspend fun findByUuid(uuid: String): Folder?
    suspend fun updatePodcasts(folderUuid: String, podcastUuids: List<String>)
    suspend fun updateColor(folderUuid: String, color: Int)
    suspend fun updateName(folderUuid: String, name: String)
    suspend fun updateSortType(folderUuid: String, podcastsSortType: PodcastsSortType)
    suspend fun removePodcast(podcast: Podcast)
    suspend fun getHomeFolder(): List<FolderItem>
    suspend fun findFolderPodcastsSorted(folderUuid: String): List<Podcast>
    fun observeFolders(): Flow<List<Folder>>
    fun findFoldersSingle(): Single<List<Folder>>
    suspend fun updatePositions(folders: List<Folder>)
    suspend fun updateSortPosition(folderItems: List<FolderItem>)
    fun findFoldersToSyncBlocking(): List<Folder>
    suspend fun findFoldersToSync(): List<Folder>
    suspend fun markAllSynced()
    suspend fun countFolders(): Int
    suspend fun getAll(): List<Folder>
    suspend fun deleteAll(uuids: Collection<String>)
    suspend fun upsertAll(folders: Collection<Folder>)
}
