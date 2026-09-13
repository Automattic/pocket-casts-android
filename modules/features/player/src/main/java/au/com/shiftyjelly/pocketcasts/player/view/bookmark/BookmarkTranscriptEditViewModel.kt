package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.BookmarkTranscript
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TextSpan
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class BookmarkTranscriptEditViewModel @Inject constructor(
    private val bookmarkManager: BookmarkManager,
    private val transcriptManager: TranscriptManager,
    private val showNotesManager: ShowNotesManager,
) : ViewModel() {

    private lateinit var arguments: BookmarkTranscriptEditArguments

    sealed interface UiState {
        data object Loading : UiState
        data object NotAvailable : UiState
        data class Loaded(
            val transcript: BookmarkTranscript,
            val passage: TextSpan?,
        ) : UiState {
            val canSave: Boolean get() = passage?.let { transcript.passage(it).text.isNotEmpty() } == true
        }
    }

    private val mutableUiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = mutableUiState

    fun load(arguments: BookmarkTranscriptEditArguments) {
        this.arguments = arguments
        viewModelScope.launch {
            val bookmark = bookmarkManager.findBookmark(arguments.bookmarkUuid)
            val storedPassage = bookmark?.passage
            if (bookmark == null || storedPassage == null) {
                mutableUiState.value = UiState.NotAvailable
                return@launch
            }
            runCatching {
                showNotesManager.loadShowNotes(bookmark.podcastUuid, bookmark.episodeUuid)
            }.onFailure {
                if (it is CancellationException) throw it
            }
            val transcript = transcriptManager.loadGeneratedTranscript(arguments.episodeUuid)
            if (transcript == null) {
                mutableUiState.value = UiState.NotAvailable
                return@launch
            }
            val model = BookmarkTranscript.from(transcript)
            mutableUiState.value = UiState.Loaded(
                transcript = model,
                passage = model.passageDisplaySpan(storedPassage, bookmark.passageLocation),
            )
        }
    }

    fun onPassageChange(passage: TextSpan) {
        mutableUiState.update { state ->
            if (state is UiState.Loaded) state.copy(passage = passage) else state
        }
    }

    fun save(onSaved: () -> Unit) {
        val state = uiState.value as? UiState.Loaded
        val passage = state?.passage
        if (state == null || passage == null) {
            onSaved()
            return
        }
        viewModelScope.launch {
            val selected = state.transcript.passage(passage)
            if (selected.text.isNotEmpty()) {
                bookmarkManager.updatePassage(
                    bookmarkUuid = arguments.bookmarkUuid,
                    passage = selected.text,
                    passageLocation = selected.location,
                )
            }
            onSaved()
        }
    }
}
