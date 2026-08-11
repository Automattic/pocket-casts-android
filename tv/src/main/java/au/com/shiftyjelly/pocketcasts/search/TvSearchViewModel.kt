package au.com.shiftyjelly.pocketcasts.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
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
    private val listRepository: ListRepository,
) : ViewModel() {

    private val _categories = MutableStateFlow<List<DiscoverCategory>>(emptyList())
    val categories: StateFlow<List<DiscoverCategory>> = _categories.asStateFlow()

    init {
        viewModelScope.launch {
            _categories.value = try {
                listRepository.getCategoriesList(CATEGORIES_URL)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to load TV browse categories")
                emptyList()
            }
        }
    }

    companion object {
        private const val CATEGORIES_URL = "${Settings.SERVER_STATIC_URL}/discover/json/categories_v2.json"
    }
}
