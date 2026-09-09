package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkSuggestion
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.BookmarkEditFormDismissedEvent
import com.automattic.eventhorizon.BookmarkEditFormShownEvent
import com.automattic.eventhorizon.BookmarkEditFormSubmittedEvent
import com.automattic.eventhorizon.BookmarkSourceType
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SourceViewType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class BookmarkViewModel
@Inject constructor(
    private val episodeManager: EpisodeManager,
    private val userEpisodeManager: UserEpisodeManager,
    private val bookmarkManager: BookmarkManager,
    private val eventHorizon: EventHorizon,
) : ViewModel(),
    CoroutineScope {

    private lateinit var arguments: BookmarkArguments
    private var capturedSuggestion: BookmarkSuggestion? = null
    private var titleEdited = false

    companion object {
        private const val DEFAULT_TITLE = "Bookmark"

        private fun buildSelectedTextFieldValue(text: String): TextFieldValue {
            return TextFieldValue(text = text, selection = TextRange(0, text.length))
        }
    }

    data class UiState(
        val bookmarkUuid: String? = null,
        val title: TextFieldValue = buildSelectedTextFieldValue(DEFAULT_TITLE),
        val passage: String? = null,
        val titleSuggestion: TitleSuggestion = TitleSuggestion.None,
    ) {
        val isNewBookmark: Boolean = bookmarkUuid == null
    }

    sealed interface TitleSuggestion {
        data object None : TitleSuggestion
        data object Generating : TitleSuggestion
        data class Available(val title: String) : TitleSuggestion
    }
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private var mutableUiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = mutableUiState

    fun load(arguments: BookmarkArguments) {
        this.arguments = arguments
        val bookmarkUuid = arguments.bookmarkUuid
        mutableUiState.value = mutableUiState.value.copy(
            bookmarkUuid = bookmarkUuid,
        )
        viewModelScope.launch {
            // load the existing bookmark
            val bookmark = if (bookmarkUuid == null) {
                val episode = episodeManager.findEpisodeByUuid(arguments.episodeUuid) ?: return@launch
                bookmarkManager.findByEpisodeTime(
                    episode = episode,
                    timeSecs = arguments.timeSecs,
                )
            } else {
                bookmarkManager.findBookmark(bookmarkUuid)
            }
            if (bookmark != null) {
                mutableUiState.value = mutableUiState.value.copy(
                    bookmarkUuid = bookmark.uuid,
                    title = buildSelectedTextFieldValue(bookmark.title),
                    passage = displayPassage(bookmark),
                )
            } else if (bookmarkUuid == null && FeatureFlag.isEnabled(Feature.SMART_BOOKMARKS)) {
                generateTitleSuggestion(arguments.episodeUuid, arguments.timeSecs)
            }
        }
    }

    private suspend fun generateTitleSuggestion(episodeUuid: String, timeSecs: Int) {
        mutableUiState.value = mutableUiState.value.copy(titleSuggestion = TitleSuggestion.Generating)
        val suggestion = bookmarkManager.suggestBookmark(episodeUuid, timeSecs)
        capturedSuggestion = suggestion
        val suggestedTitle = suggestion?.title?.takeIf { it.isNotBlank() }
        when {
            suggestedTitle == null -> mutableUiState.value = mutableUiState.value.copy(titleSuggestion = TitleSuggestion.None)
            !titleEdited -> applySuggestion(suggestedTitle)
            else -> mutableUiState.value = mutableUiState.value.copy(titleSuggestion = TitleSuggestion.Available(suggestedTitle))
        }
    }

    fun refreshPassage() {
        val bookmarkUuid = uiState.value.bookmarkUuid ?: return
        viewModelScope.launch {
            val bookmark = bookmarkManager.findBookmark(bookmarkUuid) ?: return@launch
            mutableUiState.value = mutableUiState.value.copy(passage = displayPassage(bookmark))
        }
    }

    private fun displayPassage(bookmark: Bookmark) = bookmark.passage?.takeIf { FeatureFlag.isEnabled(Feature.SMART_BOOKMARKS) }

    fun changeTitle(title: TextFieldValue) {
        titleEdited = true
        val titleLimited = title.copy(text = title.text.take(100))
        val suggestion = uiState.value.titleSuggestion
        mutableUiState.value = mutableUiState.value.copy(
            title = titleLimited,
            titleSuggestion = if (suggestion is TitleSuggestion.Generating) TitleSuggestion.None else suggestion,
        )
    }

    fun applySuggestion(title: String) {
        mutableUiState.value = mutableUiState.value.copy(
            title = buildSelectedTextFieldValue(title),
            titleSuggestion = TitleSuggestion.None,
        )
    }

    fun saveBookmark(onSaved: (Bookmark, isExistingBookmark: Boolean) -> Unit) {
        launch {
            try {
                val state = uiState.value
                val bookmarkUuid = state.bookmarkUuid
                val episodeUuid = arguments.episodeUuid
                val isExistingBookmark = bookmarkUuid != null
                val bookmark = if (bookmarkUuid == null) {
                    val episode = episodeManager.findByUuid(episodeUuid)
                        ?: userEpisodeManager.findEpisodeByUuid(episodeUuid)
                        ?: return@launch
                    val suggestion = capturedSuggestion
                    bookmarkManager.add(
                        episode = episode,
                        timeSecs = arguments.timeSecs,
                        title = state.title.text,
                        creationSource = BookmarkSourceType.Player,
                        passage = suggestion?.passage,
                        passageLocation = suggestion?.passageLocation,
                        referenceTime = suggestion?.referenceTimeSecs,
                    )
                } else {
                    bookmarkManager.updateTitle(bookmarkUuid, state.title.text)
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
                source = SourceViewType.Player,
                isNewBookmark = isNewBookmark,
            ),
        )
    }

    fun onClose() {
        eventHorizon.track(
            BookmarkEditFormDismissedEvent(
                source = SourceViewType.Player,
                isNewBookmark = uiState.value.isNewBookmark,
            ),
        )
    }

    fun onSubmitBookmark() {
        eventHorizon.track(
            BookmarkEditFormSubmittedEvent(
                source = SourceViewType.Player,
                isNewBookmark = uiState.value.isNewBookmark,
            ),
        )
    }
}
