package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.SuggestedFoldersDao
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.SuggestedFoldersRequest
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class SuggestedFoldersManager @Inject constructor(
    private val podcastCacheService: PodcastCacheServiceManager,
    appDatabase: AppDatabase,
) {

    private val suggestedFoldersDao: SuggestedFoldersDao = appDatabase.suggestedFoldersDao()

    fun getSuggestedFolders(): Flow<List<SuggestedFolder>> = suggestedFoldersDao.findAll()

    suspend fun refreshSuggestedFolders(podcastUuids: List<String>) {
        try {
            val folders = podcastCacheService.suggestedFolders(SuggestedFoldersRequest(podcastUuids))
            suggestedFoldersDao.deleteAndInsertAll(folders)
        } catch (e: Exception) {
            Timber.e(e, "Refreshing suggested folders failed")
        }
    }

    suspend fun deleteSuggestedFolders(folders: List<SuggestedFolder>) {
        suggestedFoldersDao.deleteFolders(folders)
    }
}
