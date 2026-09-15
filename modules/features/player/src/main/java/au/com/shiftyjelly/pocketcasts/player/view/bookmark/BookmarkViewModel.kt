package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkSuggestion
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.BookmarkTranscript
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.BookmarkEditFormDismissedEvent
import com.automattic.eventhorizon.BookmarkEditFormShownEvent
import com.automattic.eventhorizon.BookmarkEditFormSubmittedEvent
import com.automattic.eventhorizon.BookmarkSourceType
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SourceViewType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class BookmarkViewModel
@Inject constructor(
    private val episodeManager: EpisodeManager,
    private val userEpisodeManager: UserEpisodeManager,
    private val bookmarkManager: BookmarkManager,
    private val transcriptManager: TranscriptManager,
    private val showNotesManager: ShowNotesManager,
    private val eventHorizon: EventHorizon,
    @ApplicationContext private val context: Context,
) : ViewModel(),
    CoroutineScope {

    private lateinit var arguments: BookmarkArguments
    private var capturedSuggestion: BookmarkSuggestion? = null
    private var passageEdited = false
    private var loadJob: Job? = null
    private var analyticsSource: SourceViewType = SourceViewType.Player

    private val defaultTitle: String get() = context.getString(LR.string.bookmark)
    private var originalTitle: String = defaultTitle

    companion object {
        private const val DEFAULT_TITLE = "Bookmark"
        private val SUGGESTION_TIMEOUT = 10.seconds

        private fun buildSelectedTextFieldValue(text: String): TextFieldValue {
            return TextFieldValue(text = text, selection = TextRange(0, text.length))
        }
    }

    data class UiState(
        val bookmarkUuid: String? = null,
        val title: TextFieldValue = buildSelectedTextFieldValue(DEFAULT_TITLE),
        val passage: String? = null,
        val passageLocation: Int? = null,
        val referenceTime: Int? = null,
        val podcastUuid: String? = null,
        val titleSuggestion: TitleSuggestion = TitleSuggestion.None,
        val isNewBookmark: Boolean = true,
        val canEditTranscript: Boolean = false,
        val isCapturingPassage: Boolean = false,
    )

    sealed interface TitleSuggestion {
        data object None : TitleSuggestion
        data object Generating : TitleSuggestion
        data class Available(val title: String) : TitleSuggestion
    }
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private var mutableUiState: MutableStateFlow<UiState> = MutableStateFlow(UiState(title = buildSelectedTextFieldValue(defaultTitle)))
    val uiState: StateFlow<UiState> = mutableUiState

    fun load(arguments: BookmarkArguments) {
        if (loadJob != null) return
        this.arguments = arguments
        analyticsSource = arguments.source.analyticsValue
        val bookmarkUuid = arguments.bookmarkUuid
        val editingExisting = bookmarkUuid != null && !arguments.isNewBookmark
        mutableUiState.value = mutableUiState.value.copy(
            bookmarkUuid = bookmarkUuid,
            isNewBookmark = !editingExisting,
        )
        loadJob = viewModelScope.launch {
            // load the existing bookmark
            val episode = episodeManager.findEpisodeByUuid(arguments.episodeUuid)
            val bookmark = if (bookmarkUuid == null) {
                if (episode == null) return@launch
                bookmarkManager.findByEpisodeTime(
                    episode = episode,
                    timeSecs = arguments.timeSecs,
                )
            } else {
                bookmarkManager.findBookmark(bookmarkUuid)
            }
            val podcastUuid = bookmark?.podcastUuid ?: (episode as? PodcastEpisode)?.podcastUuid
            if (bookmark != null) {
                originalTitle = bookmark.title
                mutableUiState.value = mutableUiState.value.copy(
                    bookmarkUuid = bookmark.uuid,
                    title = buildSelectedTextFieldValue(bookmark.title),
                    passage = displayPassage(bookmark),
                    passageLocation = bookmark.passageLocation,
                    referenceTime = bookmark.referenceTime,
                    podcastUuid = podcastUuid,
                    isNewBookmark = mutableUiState.value.isNewBookmark && bookmarkUuid != null,
                )
                val passage = bookmark.passage
                if (mutableUiState.value.isNewBookmark && passage != null && FeatureFlag.isEnabled(Feature.SMART_BOOKMARKS)) {
                    generateTitleSuggestionFromPassage(passage)
                }
                if (displayPassage(bookmark) != null) {
                    updateCanEditTranscript()
                }
            } else if (bookmarkUuid == null && FeatureFlag.isEnabled(Feature.SMART_BOOKMARKS)) {
                mutableUiState.value = mutableUiState.value.copy(podcastUuid = podcastUuid)
                generateTitleSuggestion(arguments.episodeUuid, arguments.timeSecs)
            }
        }
    }

    suspend fun discardNewBookmarkIfNeeded() {
        val state = uiState.value
        val bookmarkUuid = state.bookmarkUuid
        if (state.isNewBookmark && bookmarkUuid != null) {
            bookmarkManager.deleteToSync(bookmarkUuid)
        }
    }

    private suspend fun generateTitleSuggestion(episodeUuid: String, timeSecs: Int) {
        mutableUiState.value = mutableUiState.value.copy(
            titleSuggestion = TitleSuggestion.Generating,
            isCapturingPassage = true,
        )
        val suggestion = withTimeoutOrNull(SUGGESTION_TIMEOUT) {
            bookmarkManager.suggestBookmark(episodeUuid, timeSecs)
        }
        capturedSuggestion = suggestion
        mutableUiState.value = if (suggestion != null) {
            mutableUiState.value.copy(
                passage = suggestion.passage,
                passageLocation = suggestion.passageLocation,
                referenceTime = suggestion.referenceTimeSecs,
                canEditTranscript = true,
                isCapturingPassage = false,
            )
        } else {
            mutableUiState.value.copy(isCapturingPassage = false)
        }
        val suggestedTitle = suggestion?.title?.takeIf { it.isNotBlank() }
        when {
            suggestedTitle == null -> mutableUiState.value = mutableUiState.value.copy(titleSuggestion = TitleSuggestion.None)
            uiState.value.title.text == originalTitle -> applySuggestion(suggestedTitle)
            else -> mutableUiState.value = mutableUiState.value.copy(titleSuggestion = TitleSuggestion.Available(suggestedTitle))
        }
    }

    private suspend fun generateTitleSuggestionFromPassage(passage: String) {
        mutableUiState.value = mutableUiState.value.copy(titleSuggestion = TitleSuggestion.Generating)
        val suggestedTitle = withTimeoutOrNull(SUGGESTION_TIMEOUT) {
            bookmarkManager.suggestTitle(passage)
        }?.takeIf { it.isNotBlank() }
        when {
            suggestedTitle == null -> mutableUiState.value = mutableUiState.value.copy(titleSuggestion = TitleSuggestion.None)
            uiState.value.title.text == originalTitle -> applySuggestion(suggestedTitle)
            else -> mutableUiState.value = mutableUiState.value.copy(titleSuggestion = TitleSuggestion.Available(suggestedTitle))
        }
    }

    fun onPassageEdited(passage: String, passageLocation: Int) {
        passageEdited = true
        mutableUiState.value = mutableUiState.value.copy(passage = passage, passageLocation = passageLocation, canEditTranscript = true)
    }

    private fun displayPassage(bookmark: Bookmark) = bookmark.passage?.takeIf { FeatureFlag.isEnabled(Feature.SMART_BOOKMARKS) }

    private suspend fun updateCanEditTranscript() {
        val state = mutableUiState.value
        val passage = state.passage ?: return
        state.podcastUuid?.takeIf { it.isNotBlank() }?.let { podcastUuid ->
            runCatching { showNotesManager.loadShowNotes(podcastUuid, arguments.episodeUuid) }
                .onFailure { if (it is CancellationException) throw it }
        }
        val transcript = transcriptManager.loadGeneratedTranscript(arguments.episodeUuid) ?: return
        val located = BookmarkTranscript.from(transcript).passageDisplaySpan(passage, state.passageLocation) != null
        mutableUiState.value = mutableUiState.value.copy(canEditTranscript = located)
    }

    private suspend fun resolveEditedReferenceTimeSecs(passage: String, passageLocation: Int?): Int? {
        val transcript = transcriptManager.loadGeneratedTranscript(arguments.episodeUuid) ?: return null
        val model = BookmarkTranscript.from(transcript)
        val span = model.passageDisplaySpan(passage, passageLocation) ?: return null
        val referenceTimeMs = model.referenceTimeMsAt(span.start) ?: return null
        return (referenceTimeMs / 1000).toInt()
    }

    fun changeTitle(title: TextFieldValue) {
        val titleLimited = title.copy(text = title.text.take(100))
        val suggestion = uiState.value.titleSuggestion
        mutableUiState.value = mutableUiState.value.copy(
            title = titleLimited,
            titleSuggestion = if (suggestion is TitleSuggestion.Generating) TitleSuggestion.None else suggestion,
        )
    }

    fun applySuggestion(title: String) {
        mutableUiState.value = mutableUiState.value.copy(
            title = buildSelectedTextFieldValue(title.take(100)),
            titleSuggestion = TitleSuggestion.None,
        )
    }

    fun saveBookmark(onSaved: (Bookmark, isExistingBookmark: Boolean) -> Unit) {
        launch {
            try {
                val state = uiState.value
                val bookmarkUuid = state.bookmarkUuid
                val episodeUuid = arguments.episodeUuid
                val isExistingBookmark = !state.isNewBookmark
                val title = state.title.text.replace('\n', ' ').trim().ifBlank { defaultTitle }
                val editedReferenceTimeSecs = if (passageEdited) {
                    state.passage?.let { resolveEditedReferenceTimeSecs(it, state.passageLocation) }
                } else {
                    null
                }
                val bookmark = if (bookmarkUuid == null) {
                    val episode = episodeManager.findByUuid(episodeUuid)
                        ?: userEpisodeManager.findEpisodeByUuid(episodeUuid)
                        ?: return@launch
                    loadJob?.cancel()
                    val suggestion = capturedSuggestion
                    val created = bookmarkManager.add(
                        episode = episode,
                        timeSecs = arguments.timeSecs,
                        title = title,
                        creationSource = BookmarkSourceType.Player,
                        passage = if (passageEdited) state.passage else suggestion?.passage,
                        passageLocation = if (passageEdited) state.passageLocation else suggestion?.passageLocation,
                        referenceTime = editedReferenceTimeSecs ?: suggestion?.referenceTimeSecs,
                    )
                    if (suggestion == null && FeatureFlag.isEnabled(Feature.SMART_BOOKMARKS)) {
                        bookmarkManager.enrichBookmarkPassage(created)
                    }
                    created
                } else {
                    bookmarkManager.updateTitle(bookmarkUuid, title)
                    val passage = state.passage
                    val passageLocation = state.passageLocation
                    if (passageEdited && passage != null && passageLocation != null) {
                        bookmarkManager.updatePassage(bookmarkUuid, passage, passageLocation, editedReferenceTimeSecs)
                    }
                    bookmarkManager.findBookmark(bookmarkUuid)
                }
                if (bookmark != null) {
                    onSaved(bookmark, isExistingBookmark)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun onShown(isNewBookmark: Boolean) {
        eventHorizon.track(
            BookmarkEditFormShownEvent(
                source = analyticsSource,
                isNewBookmark = isNewBookmark,
            ),
        )
    }

    fun onClose() {
        eventHorizon.track(
            BookmarkEditFormDismissedEvent(
                source = analyticsSource,
                isNewBookmark = uiState.value.isNewBookmark,
            ),
        )
    }

    fun onSubmitBookmark() {
        eventHorizon.track(
            BookmarkEditFormSubmittedEvent(
                source = analyticsSource,
                isNewBookmark = uiState.value.isNewBookmark,
            ),
        )
    }
}
