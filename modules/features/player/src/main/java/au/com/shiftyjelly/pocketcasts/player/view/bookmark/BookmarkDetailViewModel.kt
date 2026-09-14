package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
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
    private val bookmarkManager: BookmarkManager,
    private val episodeManager: EpisodeManager,
    private val transcriptManager: TranscriptManager,
    private val showNotesManager: ShowNotesManager,
    private val settings: Settings,
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

    data class UiState(
        val title: String = "",
        val passage: String? = null,
        val timeSecs: Int = 0,
        val referenceTime: Int? = null,
        val episode: BaseEpisode? = null,
        val useEpisodeArtwork: Boolean = false,
        val transcriptState: TranscriptState = TranscriptState.None,
    )

    private var bookmarkUuid: String? = null
    private lateinit var episodeUuid: String
    private lateinit var podcastUuid: String

    private val mutableState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = mutableState

    private var loaded = false

    fun load(
        bookmarkUuid: String,
        title: String,
        episodeUuid: String,
        podcastUuid: String,
        passage: String?,
        passageLocation: Int?,
        timeSecs: Int,
        referenceTime: Int?,
    ) {
        if (loaded) return
        loaded = true
        this.bookmarkUuid = bookmarkUuid
        this.episodeUuid = episodeUuid
        this.podcastUuid = podcastUuid
        mutableState.value = UiState(
            title = title,
            passage = passage,
            timeSecs = timeSecs,
            referenceTime = referenceTime,
            useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(Element.Bookmarks),
        )
        loadEpisode()
        loadTranscript(passage, passageLocation)
    }

    private fun loadEpisode() {
        viewModelScope.launch {
            val episode = episodeManager.findEpisodeByUuid(episodeUuid)
            mutableState.value = mutableState.value.copy(episode = episode)
        }
    }

    fun refresh() {
        val uuid = bookmarkUuid ?: return
        viewModelScope.launch {
            val bookmark = bookmarkManager.findBookmark(uuid) ?: return@launch
            mutableState.value = mutableState.value.copy(
                title = bookmark.title,
                passage = bookmark.passage,
                timeSecs = bookmark.timeSecs,
                referenceTime = bookmark.referenceTime,
            )
            val loaded = mutableState.value.transcriptState as? TranscriptState.Loaded
            val passage = bookmark.passage
            if (loaded != null && passage != null) {
                val span = loaded.transcript.passageDisplaySpan(passage, bookmark.passageLocation)
                mutableState.value = mutableState.value.copy(
                    transcriptState = if (span == null) TranscriptState.Unavailable else loaded.copy(passage = span),
                )
            } else {
                loadTranscript(passage, bookmark.passageLocation)
            }
        }
    }

    private fun loadTranscript(passage: String?, passageLocation: Int?) {
        if (passage == null || !FeatureFlag.isEnabled(Feature.SMART_BOOKMARKS)) {
            mutableState.value = mutableState.value.copy(transcriptState = TranscriptState.None)
            return
        }
        mutableState.value = mutableState.value.copy(transcriptState = TranscriptState.Loading)
        viewModelScope.launch {
            runCatching {
                showNotesManager.loadShowNotes(podcastUuid, episodeUuid)
            }.onFailure {
                if (it is CancellationException) throw it
            }
            val transcript = transcriptManager.loadGeneratedTranscript(episodeUuid)
            if (transcript == null) {
                mutableState.value = mutableState.value.copy(transcriptState = TranscriptState.Unavailable)
                return@launch
            }
            val model = BookmarkTranscript.from(transcript)
            val span = model.passageDisplaySpan(passage, passageLocation)
            mutableState.value = mutableState.value.copy(
                transcriptState = if (span == null) TranscriptState.Unavailable else TranscriptState.Loaded(model, span),
            )
        }
    }
}
