package au.com.shiftyjelly.pocketcasts.discover.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverListSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NetworksGridViewModel @Inject constructor(
    private val listRepository: ListRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    private var sourceUrl: String? = null
    private var loadJob: Job? = null

    /** The view model outlives its view, so a recreated view keeps whatever is already loaded. */
    fun load(sourceUrl: String) {
        if (sourceUrl == this.sourceUrl && _state.value is UiState.Loaded) {
            return
        }
        this.sourceUrl = sourceUrl
        fetch(sourceUrl)
    }

    fun retry() {
        sourceUrl?.let(::fetch)
    }

    private fun fetch(sourceUrl: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UiState.Loading
            val networks = listRepository.getListFeed(url = sourceUrl)?.networks
            // the row hides itself when it has no networks, so an empty grid means the feed failed us
            _state.value = if (networks.isNullOrEmpty()) UiState.Error else UiState.Loaded(networks)
        }
    }

    sealed interface UiState {
        data object Loading : UiState
        data class Loaded(val networks: List<DiscoverListSummary>) : UiState
        data object Error : UiState
    }
}
