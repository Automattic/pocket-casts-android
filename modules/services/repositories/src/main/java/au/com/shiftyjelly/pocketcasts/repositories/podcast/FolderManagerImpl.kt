package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import io.reactivex.Single
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class FolderManagerImpl @Inject constructor(
    appDatabase: AppDatabase,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) : FolderManager,
    CoroutineScope {

    private val folderDao = appDatabase.folderDao()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override suspend fun create(name: String, color: Int, podcastsSortType: PodcastsSortType, podcastUuids: List<String>): Folder {
        val existingFolders = folderDao.findFolders().map { FolderItem.Folder(it, emptyList()) }

        val newFolder = Folder(
            uuid = UUID.randomUUID().toString(),
            name = name,
            color = color,
            addedDate = Date(),
            sortPosition = 0,
            podcastsSortType = podcastsSortType,
            deleted = false,
            syncModified = System.currentTimeMillis(),
        )

        folderDao.insert(newFolder)

        podcastManager.updateFolderUuid(folderUuid = newFolder.uuid, podcastUuids = podcastUuids)

        // Update the drag and drop sort positions
        val podcasts = podcastManager.findPodcastsNotInFolder().map { FolderItem.Podcast(it) }
        val sorted = (existingFolders + podcasts).sortedBy { it.sortPosition }
        val newFolderItem = FolderItem.Folder(newFolder, emptyList())
        val newList = listOf(newFolderItem) + sorted
        updateSortPosition(newList)

        return newFolder
    }

    override suspend fun upsertSynced(folder: Folder): Folder {
        folderDao.insert(folder)
        return folder
    }

    override suspend fun delete(folder: Folder) {
        // remove all the podcasts in the folder
        val podcastUuids = podcastManager.findPodcastsInFolder(folder.uuid).map { it.uuid }
        if (podcastUuids.isNotEmpty()) {
            podcastManager.updateFolderUuid(null, podcastUuids)
        }
        // mark the folder as deleted, only signed in paid Pocket Casts users have the folder feature.
        folderDao.updateDeleted(uuid = folder.uuid, deleted = true, syncModified = System.currentTimeMillis())
    }

    override suspend fun deleteAll() {
        folderDao.deleteAll()
    }

    override suspend fun deleteSynced(folderUuid: String) {
        folderDao.deleteByUuid(folderUuid)
    }

    override suspend fun findByUuid(uuid: String): Folder? {
        return folderDao.findByUuid(uuid)
    }

    override suspend fun updatePodcasts(folderUuid: String, podcastUuids: List<String>) {
        val existingUuids = podcastManager.findPodcastsInFolder(folderUuid).map { podcast -> podcast.uuid }
        val removedUuids = existingUuids.minus(podcastUuids)
        if (removedUuids.isNotEmpty()) {
            podcastManager.updateFolderUuid(folderUuid = null, podcastUuids = removedUuids)
        }
        val addedUuids = podcastUuids.minus(existingUuids)
        if (addedUuids.isNotEmpty()) {
            podcastManager.updateFolderUuid(folderUuid = folderUuid, podcastUuids = addedUuids)
        }
    }

    override suspend fun updateColor(folderUuid: String, color: Int) {
        folderDao.updateFolderColor(uuid = folderUuid, color = color, syncModified = System.currentTimeMillis())
    }

    override suspend fun updateName(folderUuid: String, name: String) {
        folderDao.updateFolderName(uuid = folderUuid, name = name, syncModified = System.currentTimeMillis())
    }

    override suspend fun updateSortType(folderUuid: String, podcastsSortType: PodcastsSortType) {
        folderDao.updateFolderSortType(uuid = folderUuid, podcastsSortType = podcastsSortType, syncModified = System.currentTimeMillis())
    }

    override suspend fun removePodcast(podcast: Podcast) {
        podcastManager.updateFolderUuid(folderUuid = null, podcastUuids = listOf(podcast.uuid))
    }

    override fun observeFolders(): Flow<List<Folder>> {
        return folderDao.findFoldersFlow()
    }

    override fun findFoldersSingle(): Single<List<Folder>> {
        return folderDao.findFoldersRxSingle()
    }

    override suspend fun updatePositions(folders: List<Folder>) {
        folderDao.updateSortPositions(folders = folders, syncModified = System.currentTimeMillis())
    }

    override suspend fun updateSortPosition(folderItems: List<FolderItem>) {
        val podcasts = mutableListOf<Podcast>()
        val folders = mutableListOf<Folder>()
        folderItems.forEachIndexed { index, folderItem ->
            when (folderItem) {
                is FolderItem.Podcast -> {
                    val podcast = folderItem.podcast.apply {
                        sortPosition = index
                    }
                    podcasts.add(podcast)
                }
                is FolderItem.Folder -> {
                    val folder = folderItem.folder.apply {
                        sortPosition = index
                    }
                    folders.add(folder)
                }
            }
        }

        podcastManager.updatePodcastPositions(podcasts)
        updatePositions(folders)
    }

    override fun findFoldersToSyncBlocking(): List<Folder> {
        return folderDao.findNotSyncedBlocking()
    }

    override suspend fun findFoldersToSync(): List<Folder> {
        return folderDao.findNotSynced()
    }

    override suspend fun markAllSynced() {
        folderDao.updateAllSynced()
    }

    override suspend fun getHomeFolder(): List<FolderItem> {
        val sortType = settings.podcastsSortType.value

        val podcasts = when (sortType) {
            PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST -> podcastManager.findPodcastsOrderByLatestEpisode(orderAsc = false)
            PodcastsSortType.RECENTLY_PLAYED -> podcastManager.findPodcastsOrderByRecentlyPlayedEpisode()
            else -> podcastManager.findSubscribedNoOrder()
        }
        val folders = folderDao.findFolders()
        val folderItems = combineFoldersPodcasts(folders, podcasts)

        return when (sortType) {
            PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST,
            PodcastsSortType.RECENTLY_PLAYED,
            -> folderItems
            else -> folderItems.sortedWith(sortType.folderComparator)
        }
    }

    private fun combineFoldersPodcasts(folders: List<Folder>, podcasts: List<Podcast>): MutableList<FolderItem> {
        val folderItems = folders.map { folder -> FolderItem.Folder(folder = folder, podcasts = emptyList()) }
        val items = mutableListOf<FolderItem>()
        val uuidToFolder = folderItems.associateBy({ it.uuid }, { it }).toMutableMap()
        for (podcast in podcasts) {
            if (podcast.folderUuid == null) {
                items.add(FolderItem.Podcast(podcast))
            } else {
                // add the folder in the position of the podcast with the latest release date
                val folder = uuidToFolder.remove(podcast.folderUuid)
                if (folder != null) {
                    items.add(folder)
                }
            }
        }
        if (uuidToFolder.isNotEmpty()) {
            items.addAll(uuidToFolder.values)
        }
        return items
    }

    override suspend fun findFolderPodcastsSorted(folderUuid: String): List<Podcast> {
        val folder = findByUuid(folderUuid) ?: return emptyList()
        // use a query to get the podcasts ordered by episode release date
        return when (folder.podcastsSortType) {
            PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST -> podcastManager.findFolderPodcastsOrderByLatestEpisode(folderUuid = folderUuid)
            PodcastsSortType.RECENTLY_PLAYED -> podcastManager.findFolderPodcastsOrderByRecentlyPlayedEpisode(folderUuid = folderUuid)
            else -> {
                val podcasts = podcastManager.findPodcastsInFolder(folderUuid = folderUuid)
                podcasts.sortedWith(folder.podcastsSortType.podcastComparator)
            }
        }
    }

    override suspend fun countFolders() = folderDao.count()

    override suspend fun deleteAll(uuids: Collection<String>) {
        folderDao.deleteAll(uuids)
    }

    override suspend fun getAll(): List<Folder> {
        return folderDao.findFolders()
    }

    override suspend fun upsertAll(folders: Collection<Folder>) {
        folderDao.upsertAll(folders)
    }
}
