package au.com.shiftyjelly.pocketcasts.repositories.searchhistory

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class SearchHistoryManagerImpl @Inject constructor(
    appDatabase: AppDatabase,
) : SearchHistoryManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
    private val searchHistoryDao = appDatabase.searchHistoryDao()

    override suspend fun findAll(
        showFolders: Boolean,
        limit: Int,
    ): List<SearchHistoryEntry> {
        val items = searchHistoryDao.findAll(showFolders, limit)
        return items.map { SearchHistoryEntry.fromSearchHistoryItem(it) }
    }

    override suspend fun add(entry: SearchHistoryEntry) {
        searchHistoryDao.insert(entry.toSearchHistoryItem())
    }

    override suspend fun remove(entry: SearchHistoryEntry) {
        searchHistoryDao.delete(entry.toSearchHistoryItem())
    }

    override suspend fun clearAll() {
        searchHistoryDao.deleteAll()
    }
}
