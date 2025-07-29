package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = CreatePlaylistViewModel.Factory::class)
class CreatePlaylistViewModel @AssistedInject constructor(
    @Assisted initialPlaylistName: String,
) : ViewModel() {
    val playlistNameState = TextFieldState(
        initialText = initialPlaylistName,
        initialSelection = TextRange(0, initialPlaylistName.length),
    )

    @AssistedFactory
    interface Factory {
        fun create(initialPlaylistName: String): CreatePlaylistViewModel
    }
}
