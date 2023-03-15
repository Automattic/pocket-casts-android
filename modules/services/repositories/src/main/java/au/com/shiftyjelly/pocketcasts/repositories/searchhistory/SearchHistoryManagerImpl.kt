package au.com.shiftyjelly.pocketcasts.repositories.searchhistory

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

private const val MAX_HISTORY_COUNT = 20

class SearchHistoryManagerImpl @Inject constructor(
    appDatabase: AppDatabase,
) : SearchHistoryManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
    private val searchHistoryDao = appDatabase.searchHistoryDao()

    override suspend fun findAll(
        showFolders: Boolean,
    ): List<SearchHistoryEntry> {
        val items = searchHistoryDao.findAll(showFolders)
        return items.map { SearchHistoryEntry.fromSearchHistoryItem(it) }
    }

    override suspend fun add(entry: SearchHistoryEntry) {
        searchHistoryDao.insert(entry.toSearchHistoryItem())
        truncateHistory(MAX_HISTORY_COUNT)
    }

    override suspend fun remove(entry: SearchHistoryEntry) {
        searchHistoryDao.delete(entry.toSearchHistoryItem())
    }

    override suspend fun clearAll() {
        searchHistoryDao.deleteAll()
    }

    override suspend fun truncateHistory(limit: Int) {
        searchHistoryDao.truncateHistory(limit)
    }
}
