package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val transcriptsManager: TranscriptsManager,
    playbackManager: PlaybackManager,
) : ViewModel() {

    val uiState: StateFlow<UiState> = playbackManager.playbackStateFlow
        .map { it.episodeUuid }
        .distinctUntilChanged()
        .flatMapLatest(::createUiStateFlow)
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState())

    private fun createUiStateFlow(episodeId: String) =
        transcriptsManager.observerTranscriptForEpisode(episodeId)
            .map { transcript ->
                UiState(
                    transcript = transcript,
                    episodeId = episodeId,
                )
            }

    data class UiState(
        val transcript: Transcript? = null,
        val episodeId: String? = null,
    )
}
