package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.transcript.BookmarkTranscript
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TextSpan
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BookmarkDetailViewModel @Inject constructor(
    private val transcriptManager: TranscriptManager,
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

    fun load(episodeUuid: String, passage: String?, passageLocation: Int?) {
        if (passage == null || !FeatureFlag.isEnabled(Feature.SMART_BOOKMARKS)) {
            mutableState.value = TranscriptState.None
            return
        }
        mutableState.value = TranscriptState.Loading
        viewModelScope.launch {
            val transcript = transcriptManager.loadGeneratedTranscript(episodeUuid)
            if (transcript == null) {
                mutableState.value = TranscriptState.Unavailable
                return@launch
            }
            val model = BookmarkTranscript.from(transcript)
            mutableState.value = TranscriptState.Loaded(model, model.passageDisplaySpan(passage, passageLocation))
        }
    }
}
