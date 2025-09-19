package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AddToPlaylistViewModel.Factory::class)
class AddToPlaylistViewModel @AssistedInject constructor(
    private val playlistManager: PlaylistManager,
    @Assisted("id")private val episodeUuid: String,
    @Assisted("title") initialPlaylistTitle: String,
) : ViewModel() {
    private val _createdPlaylist = CompletableDeferred<String>(viewModelScope.coroutineContext[Job])

    val createdPlaylist: Deferred<String> get() = _createdPlaylist

    val newPlaylistNameState = TextFieldState(
        initialText = initialPlaylistTitle,
        initialSelection = TextRange(0, initialPlaylistTitle.length),
    )

    private var isCreationTriggered = false

    fun createPlaylist() {
        val sanitizedName = newPlaylistNameState.text.toString().trim()
        if (isCreationTriggered || sanitizedName.isEmpty()) {
            return
        }
        isCreationTriggered = true
        viewModelScope.launch {
            val playlistUuid = playlistManager.createManualPlaylist(sanitizedName)
            playlistManager.addManualEpisode(playlistUuid, episodeUuid)
            _createdPlaylist.complete(playlistUuid)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("id") episodeUuid: String,
            @Assisted("title") initialPlaylistTitle: String,
        ): AddToPlaylistViewModel
    }
}
