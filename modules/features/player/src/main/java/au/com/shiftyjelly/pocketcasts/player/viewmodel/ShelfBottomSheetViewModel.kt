package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptsManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ShelfBottomSheetViewModel.Factory::class)
class ShelfBottomSheetViewModel @AssistedInject constructor(
    @Assisted private val episodeId: String?,
    private val transcriptsManager: TranscriptsManager,
) : ViewModel() {
    private var _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        viewModelScope.launch {
            episodeId?.let {
                transcriptsManager.observerTranscriptForEpisode(episodeId)
                    .distinctUntilChangedBy { it?.episodeUuid }
                    .stateIn(viewModelScope)
                    .collectLatest { transcript ->
                        _uiState.update { it.copy(transcript = transcript) }
                    }
            } ?: _uiState.update { it.copy(transcript = null) }
        }
    }

    data class UiState(
        val transcript: Transcript? = null,
    )

    @AssistedFactory
    interface Factory {
        fun create(episodeId: String?): ShelfBottomSheetViewModel
    }
}
