package au.com.shiftyjelly.pocketcasts.repositories.searchhistory

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
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
}
