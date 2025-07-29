package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel(assistedFactory = CreatePlaylistViewModel.Factory::class)
class CreatePlaylistViewModel @AssistedInject constructor(
    @Assisted initialPlaylistName: String,
) : ViewModel() {
    val playlistNameState = TextFieldState(
        initialText = initialPlaylistName,
        initialSelection = TextRange(0, initialPlaylistName.length),
    )

    private val _uiState = MutableStateFlow(
        UiState(
            smartPlaylistDraft = SmartPlaylistDraft(title = initialPlaylistName),
        ),
    )
    val uiState = _uiState.asStateFlow()

    fun updateDraftTitle() {
        _uiState.update { state ->
            val draft = state.smartPlaylistDraft.copy(title = playlistNameState.text.toString())
            state.copy(smartPlaylistDraft = draft)
        }
    }

    data class UiState(
        val smartPlaylistDraft: SmartPlaylistDraft,
    )

    @AssistedFactory
    interface Factory {
        fun create(initialPlaylistName: String): CreatePlaylistViewModel
    }
}
