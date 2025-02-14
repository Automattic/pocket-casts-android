package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.SuggestedFoldersDao
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.SuggestedFoldersRequest
import jakarta.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

class SuggestedFoldersManager @Inject constructor(
    private val podcastCacheService: PodcastCacheServiceManager,
    appDatabase: AppDatabase,
) : CoroutineScope {

    private val suggestedFoldersDao: SuggestedFoldersDao = appDatabase.suggestedFoldersDao()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    fun getSuggestedFolders(): Flow<List<SuggestedFolder>>? {
        try {
            return suggestedFoldersDao.findAll()
        } catch (e: Exception) {
            Timber.e(e, "Getting suggested folders failed")
            return null
        }
    }

    suspend fun refreshSuggestedFolders(podcastUuids: List<String>) {
        try {
            val request = SuggestedFoldersRequest(podcastUuids)
            val folders: List<SuggestedFolder> = podcastCacheService.suggestedFolders(request)
            suggestedFoldersDao.insertAll(folders)
        } catch (e: Exception) {
            Timber.e(e, "Refreshing suggested folders failed")
        }
    }
}
