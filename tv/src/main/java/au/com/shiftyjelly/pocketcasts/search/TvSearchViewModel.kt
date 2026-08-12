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
            val discover = try {
                discoverFeedLoader.searchDiscoverFeed()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to load TV search discover feed")
                return@launch
            }
            // Publish categories and rows independently so the categories row (2 requests) does not
            // wait for the whole row fan-out (~10-20 requests) to resolve.
            launch { _categories.value = discoverFeedLoader.loadCategories(discover) }
            launch {
                _discoverRows.value = try {
                    discoverFeedLoader.buildRows(discover, syncManager.isLoggedIn())
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Timber.e(exception, "Failed to load TV search discover rows")
                    emptyList()
                }
            }
        }
    }
}
