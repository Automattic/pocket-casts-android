package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType.DRAG_DROP
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType.NAME_A_TO_Z
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class FolderManagerImpl @Inject constructor(
    appDatabase: AppDatabase,
    private val podcastManager: PodcastManager,
    private val settings: Settings
) : FolderManager, CoroutineScope {

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
            syncModified = System.currentTimeMillis()
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

    override fun observeFolders(): Flowable<List<Folder>> {
        return folderDao.observeFolders()
    }

    override fun findFoldersFlow(): Flow<List<Folder>> {
        return folderDao.findFoldersFlow()
    }

    override fun findFoldersSingle(): Single<List<Folder>> {
        return folderDao.findFoldersSingle()
    }

    override fun findByUuidFlowable(uuid: String): Flowable<List<Folder>> {
        return folderDao.findByUuidFlowable(uuid)
    }

    override fun findByUuidFlow(uuid: String): Flow<List<Folder>> {
        return folderDao.findByUuidFlow(uuid)
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

    override fun findFoldersToSync(): List<Folder> {
        return folderDao.findNotSynced()
    }

    override fun markAllSynced() {
        folderDao.updateAllSynced()
    }

    override suspend fun getHomeFolder(): List<FolderItem> {
        val sortType = settings.podcastsSortType.flow.value

        val podcasts = if (sortType == EPISODE_DATE_NEWEST_TO_OLDEST) {
            podcastManager.findPodcastsOrderByLatestEpisode(orderAsc = false)
        } else {
            podcastManager.findSubscribed()
        }
        val folders = folderDao.findFolders()
        val folderItems = combineFoldersPodcasts(folders, podcasts)

        return if (sortType == EPISODE_DATE_NEWEST_TO_OLDEST) {
            folderItems
        } else {
            folderItems.sortedWith(sortType.folderComparator)
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
        if (folder.podcastsSortType == EPISODE_DATE_NEWEST_TO_OLDEST) {
            return podcastManager.findFolderPodcastsOrderByLatestEpisode(folderUuid = folderUuid)
        }
        val podcasts = podcastManager.findPodcastsInFolder(folderUuid = folderUuid)
        return podcasts.sortedWith(folder.podcastsSortType.podcastComparator)
    }

    private fun buildHomeFolderItems(podcasts: List<Podcast>, folders: List<FolderItem>, podcastSortType: PodcastsSortType): List<FolderItem> {
        if (podcastSortType == EPISODE_DATE_NEWEST_TO_OLDEST) {
            val items = mutableListOf<FolderItem>()
            val uuidToFolder = folders.associateBy({ it.uuid }, { it }).toMutableMap()
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
        } else {
            val folderUuids = folders.map { it.uuid }.toHashSet()
            val items = podcasts
                // add the podcasts not in a folder or if the folder doesn't exist
                .filter { podcast -> podcast.folderUuid == null || !folderUuids.contains(podcast.folderUuid) }
                .map { FolderItem.Podcast(it) }
                .toMutableList<FolderItem>()
                // add the folders
                .apply { addAll(folders) }

            val itemsSorted = when (podcastSortType) {
                NAME_A_TO_Z -> items.sortedWith(compareBy { PodcastsSortType.cleanStringForSort(it.title) })
                DATE_ADDED_OLDEST_TO_NEWEST -> items.sortedWith(compareBy { it.addedDate })
                DRAG_DROP -> items.sortedWith(compareBy { it.sortPosition })
                else -> items
            }

            return itemsSorted
        }
    }

    override fun countFolders() = folderDao.count()
}
