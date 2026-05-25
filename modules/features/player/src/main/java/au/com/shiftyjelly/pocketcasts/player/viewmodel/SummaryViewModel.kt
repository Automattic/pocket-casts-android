package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val transcriptManager: TranscriptManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    sealed interface SummaryState {
        data object Loading : SummaryState
        data class Loaded(val text: String) : SummaryState
        data object NotAvailable : SummaryState
    }

    private val _state = MutableStateFlow<SummaryState>(SummaryState.Loading)
    val state: StateFlow<SummaryState> = _state.asStateFlow()

    private var currentEpisodeUuid: String? = null
    private var loadJob: Job? = null

    fun clearSummary() {
        currentEpisodeUuid = null
        loadJob?.cancel()
        _state.value = SummaryState.NotAvailable
    }

    fun loadSummary(episodeUuid: String) {
        if (currentEpisodeUuid == episodeUuid && _state.value is SummaryState.Loaded) return
        currentEpisodeUuid = episodeUuid
        loadJob?.cancel()
        _state.value = SummaryState.Loading
        loadJob = viewModelScope.launch(ioDispatcher) {
            val text = transcriptManager.loadSummaryText(episodeUuid)
            _state.value = if (text != null) {
                SummaryState.Loaded(text)
            } else {
                SummaryState.NotAvailable
            }
        }
    }
}
