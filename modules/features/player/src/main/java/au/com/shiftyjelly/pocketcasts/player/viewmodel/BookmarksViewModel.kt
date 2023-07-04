package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.ui.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel
@Inject constructor(
    private val bookmarkManager: BookmarkManager,
    private val episodeManager: EpisodeManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Empty)
    val uiState: StateFlow<UiState> = _uiState

    fun loadBookmarks(episodeUuid: String) {
        viewModelScope.launch(ioDispatcher) {
            episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                _uiState.value = UiState.Loading
                bookmarkManager.findEpisodeBookmarks(episode)
                    .stateIn(viewModelScope)
                    .collect { bookmarks ->
                        _uiState.value = if (bookmarks.isNotEmpty()) {
                            UiState.Loaded(bookmarks)
                        } else {
                            UiState.Empty
                        }
                    }
            }
        }
    }

    sealed class UiState {
        object Empty : UiState()
        object Loading : UiState()
        data class Loaded(
            val bookmarks: List<Bookmark> = emptyList(),
        ) : UiState()
    }
}
