package au.com.shiftyjelly.pocketcasts.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedLoader
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class TvSearchViewModel @Inject constructor(
    private val discoverFeedLoader: TvDiscoverFeedLoader,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _categories = MutableStateFlow<List<DiscoverCategory>>(emptyList())
    val categories: StateFlow<List<DiscoverCategory>> = _categories.asStateFlow()

    private val _discoverRows = MutableStateFlow<List<TvDiscoverRow>>(emptyList())
    val discoverRows: StateFlow<List<TvDiscoverRow>> = _discoverRows.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val discover = discoverFeedLoader.loadSearch(syncManager.isLoggedIn())
                _categories.value = discover.categories
                _discoverRows.value = discover.rows
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to load TV search discover feed")
            }
        }
    }
}
