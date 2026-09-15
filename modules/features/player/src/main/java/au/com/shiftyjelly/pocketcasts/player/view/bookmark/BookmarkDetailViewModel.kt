package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.BookmarkTranscript
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TextSpan
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BookmarkDetailViewModel @Inject constructor(
    private val transcriptManager: TranscriptManager,
    private val showNotesManager: ShowNotesManager,
) : ViewModel() {

    sealed interface TranscriptState {
        data object None : TranscriptState
        data object Loading : TranscriptState
        data object Unavailable : TranscriptState
        data class Loaded(
            val transcript: BookmarkTranscript,
            val passage: TextSpan?,
        ) : TranscriptState
    }

    private val mutableState = MutableStateFlow<TranscriptState>(TranscriptState.None)
    val transcriptState: StateFlow<TranscriptState> = mutableState

    private var loaded = false

    fun load(episodeUuid: String, podcastUuid: String, passage: String?, passageLocation: Int?) {
        if (loaded) return
        loaded = true
        if (passage == null || !FeatureFlag.isEnabled(Feature.SMART_BOOKMARKS)) {
            mutableState.value = TranscriptState.None
            return
        }
        mutableState.value = TranscriptState.Loading
        viewModelScope.launch {
            runCatching {
                showNotesManager.loadShowNotes(podcastUuid, episodeUuid)
            }.onFailure {
                if (it is CancellationException) throw it
            }
            val transcript = transcriptManager.loadGeneratedTranscript(episodeUuid)
            if (transcript == null) {
                mutableState.value = TranscriptState.Unavailable
                return@launch
            }
            val model = BookmarkTranscript.from(transcript)
            val span = model.passageDisplaySpan(passage, passageLocation)
            mutableState.value = if (span == null) {
                TranscriptState.Unavailable
            } else {
                TranscriptState.Loaded(model, span)
            }
        }
    }
}
