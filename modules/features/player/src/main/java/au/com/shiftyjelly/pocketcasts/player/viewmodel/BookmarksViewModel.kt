package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.ui.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel
@Inject constructor(
    private val bookmarkManager: BookmarkManager,
    private val episodeManager: EpisodeManager,
    private val userManager: UserManager,
    private val multiSelectHelper: MultiSelectBookmarksHelper,
    private val settings: Settings,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _showOptionsDialog = MutableSharedFlow<Int>()
    val showOptionsDialog = _showOptionsDialog.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadBookmarks(episodeUuid: String) {
        viewModelScope.launch(ioDispatcher) {
            userManager.getSignInState().asFlow().collectLatest {
                if (!it.isSignedInAsPlusOrPatron) {
                    _uiState.value = UiState.PlusUpsell
                } else {
                    episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                        val bookmarksFlow = settings.bookmarkSortTypeFlow.flatMapLatest { sortType ->
                            bookmarkManager.findEpisodeBookmarksFlow(
                                episode = episode,
                                sortType = sortType,
                            )
                        }
                        val isMultiSelectingFlow = multiSelectHelper.isMultiSelectingLive.asFlow()
                        val selectedListFlow = multiSelectHelper.selectedListLive.asFlow()
                        combine(
                            bookmarksFlow,
                            isMultiSelectingFlow,
                            selectedListFlow,
                        ) { bookmarks, isMultiSelecting, selectedList ->
                            _uiState.value = if (bookmarks.isEmpty()) {
                                UiState.Empty
                            } else {
                                UiState.Loaded(
                                    bookmarks = bookmarks,
                                    isMultiSelecting = isMultiSelecting,
                                    isSelected = { selectedList.contains(it) },
                                    onRowClick = ::onRowClick,
                                )
                            }
                        }.stateIn(viewModelScope)
                    } ?: run { // This shouldn't happen in the ideal world
                        Timber.e("Episode not found.")
                        _uiState.value = UiState.Empty
                    }
                }
            }
        }

        multiSelectHelper.listener = object : MultiSelectHelper.Listener<Bookmark> {
            override fun multiSelectSelectAll() {
                (_uiState.value as? UiState.Loaded)?.bookmarks?.let {
                    multiSelectHelper.selectAllInList(it)
                }
            }

            override fun multiSelectSelectNone() {
                (_uiState.value as? UiState.Loaded)?.bookmarks?.let { bookmarks ->
                    bookmarks.forEach { multiSelectHelper.deselect(it) }
                }
            }

            override fun multiDeselectAllAbove(multiSelectable: Bookmark) {
                (_uiState.value as? UiState.Loaded)?.bookmarks?.let { bookmarks ->
                    val startIndex = bookmarks.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        val episodesAbove = bookmarks.subList(0, startIndex + 1)
                        multiSelectHelper.deselectAllInList(episodesAbove)
                    }
                }
            }

            override fun multiDeselectAllBelow(multiSelectable: Bookmark) {
                (_uiState.value as? UiState.Loaded)?.bookmarks?.let { bookmarks ->
                    val startIndex = bookmarks.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        bookmarks.subList(startIndex, bookmarks.size)
                            .forEach { multiSelectHelper.deselect(it) }
                    }
                }
            }

            override fun multiSelectSelectAllUp(multiSelectable: Bookmark) {
                (_uiState.value as? UiState.Loaded)?.bookmarks?.let { bookmarks ->
                    val startIndex = bookmarks.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(bookmarks.subList(0, startIndex + 1))
                    }
                }
            }

            override fun multiSelectSelectAllDown(multiSelectable: Bookmark) {
                (_uiState.value as? UiState.Loaded)?.bookmarks?.let { bookmarks ->
                    val startIndex = bookmarks.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(
                            bookmarks.subList(
                                startIndex,
                                bookmarks.size
                            )
                        )
                    }
                }
            }
        }
    }

    private fun onRowClick(bookmark: Bookmark) {
        if ((_uiState.value as? UiState.Loaded)?.isMultiSelecting == false) return

        if (multiSelectHelper.isSelected(bookmark)) {
            multiSelectHelper.deselect(bookmark)
        } else {
            multiSelectHelper.select(bookmark)
        }
    }

    fun onOptionsMenuClicked() {
        viewModelScope.launch {
            _showOptionsDialog.emit(settings.getBookmarksSortType().mapToLocalizedString())
        }
    }

    fun changeSortOrder(order: BookmarksSortType) {
        settings.setBookmarksSortType(order)
    }

    sealed class UiState {
        object Empty : UiState()
        object Loading : UiState()
        data class Loaded(
            val bookmarks: List<Bookmark> = emptyList(),
            val isMultiSelecting: Boolean,
            val isSelected: (Bookmark) -> Boolean,
            val onRowClick: (Bookmark) -> Unit,
        ) : UiState()

        object PlusUpsell : UiState()
    }
}
