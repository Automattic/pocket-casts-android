package au.com.shiftyjelly.pocketcasts.search

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchHandler: SearchHandler
) : ViewModel() {

    val searchResults = searchHandler.searchResults
    val loading = searchHandler.loading

    fun updateSearchQuery(query: String) {
        searchHandler.updateSearchQuery(query)
    }

    fun setOnlySearchRemote(remote: Boolean) {
        searchHandler.setOnlySearchRemote(remote)
    }

    fun setSource(source: AnalyticsSource) {
        searchHandler.setSource(source)
    }
}

sealed class SearchState {
    object NoResults : SearchState()
    data class Results(val list: List<FolderItem>, val loading: Boolean, val error: Throwable?) : SearchState()
}
