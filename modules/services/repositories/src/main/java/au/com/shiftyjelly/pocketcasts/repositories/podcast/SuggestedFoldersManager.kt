package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.SuggestedFoldersDao
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.SuggestedFoldersRequest
import au.com.shiftyjelly.pocketcasts.utils.extensions.md5
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

class SuggestedFoldersManager @Inject constructor(
    private val podcastCacheService: PodcastCacheServiceManager,
    private val settings: Settings,
    appDatabase: AppDatabase,
) {

    private val suggestedFoldersDao: SuggestedFoldersDao = appDatabase.suggestedFoldersDao()

    fun getSuggestedFolders(): Flow<List<SuggestedFolder>> = suggestedFoldersDao.findAll()

    suspend fun refreshSuggestedFolders(podcastUuids: List<String>) {
        try {
            withContext(Dispatchers.IO) {
                if (podcastUuids.isEmpty()) {
                    suggestedFoldersDao.deleteAll()
                    settings.suggestedFoldersFollowedHash.set("", updateModifiedAt = false)
                } else {
                    val currentHash = podcastUuids.sorted().md5()

                    if (currentHash != settings.suggestedFoldersFollowedHash.value) {
                        val folders = podcastCacheService.suggestedFolders(SuggestedFoldersRequest(podcastUuids))
                        suggestedFoldersDao.deleteAndInsertAll(folders)
                        currentHash?.let { settings.suggestedFoldersFollowedHash.set(it, updateModifiedAt = false) }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Refreshing suggested folders failed")
        }
    }

    suspend fun replaceSuggestedFolders(newFolders: List<SuggestedFolder>) {
        suggestedFoldersDao.deleteFolders(newFolders)
    }

    suspend fun deleteAllSuggestedFolders() {
        suggestedFoldersDao.deleteAll()
    }
}
