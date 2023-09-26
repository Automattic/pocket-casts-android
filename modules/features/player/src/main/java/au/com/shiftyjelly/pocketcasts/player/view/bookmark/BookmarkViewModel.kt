package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class BookmarkViewModel
@Inject constructor(
    private val episodeManager: EpisodeManager,
    private val userEpisodeManager: UserEpisodeManager,
    private val bookmarkManager: BookmarkManager
) : ViewModel(), CoroutineScope {

    private lateinit var arguments: BookmarkArguments

    companion object {
        private const val DEFAULT_TITLE = "Bookmark"

        private fun buildSelectedTextFieldValue(text: String): TextFieldValue {
            return TextFieldValue(text = text, selection = TextRange(0, text.length))
        }
    }

    data class UiState(
        val bookmarkUuid: String? = null,
        val title: TextFieldValue = buildSelectedTextFieldValue(DEFAULT_TITLE),
        val backgroundColor: Color = Color.Black,
        val tintColor: Color = Color.Blue
    ) {
        val isNewBookmark: Boolean = bookmarkUuid == null
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
            backgroundColor = Color(arguments.backgroundColor),
            tintColor = Color(arguments.tintColor)
        )
        viewModelScope.launch {
            // load the existing bookmark
            val bookmark = if (bookmarkUuid == null) {
                val episode = episodeManager.findEpisodeByUuid(arguments.episodeUuid) ?: return@launch
                bookmarkManager.findByEpisodeTime(
                    episode = episode,
                    timeSecs = arguments.timeSecs
                )
            } else {
                bookmarkManager.findBookmark(bookmarkUuid)
            }
            if (bookmark != null) {
                mutableUiState.value = mutableUiState.value.copy(
                    bookmarkUuid = bookmark.uuid,
                    title = buildSelectedTextFieldValue(bookmark.title)
                )
            }
        }
    }

    fun changeTitle(title: TextFieldValue) {
        // limit the title to 100 characters
        val titleLimited = title.copy(text = title.text.take(100))
        mutableUiState.value = mutableUiState.value.copy(title = titleLimited)
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
                    bookmarkManager.add(
                        episode = episode,
                        timeSecs = arguments.timeSecs,
                        title = state.title.text,
                        creationSource = BookmarkManager.CreationSource.PLAYER,
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
}
