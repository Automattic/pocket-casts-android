package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.bookmark.BookmarkRowColors
import au.com.shiftyjelly.pocketcasts.compose.buttons.TimePlayButtonStyle
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortType
import au.com.shiftyjelly.pocketcasts.models.type.BookmarksSortTypeForPlayer
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkArguments
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.HeaderRowColors
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.MessageViewColors
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.NoBookmarksViewColors
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.ui.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
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
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel
@Inject constructor(
    private val bookmarkManager: BookmarkManager,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val userManager: UserManager,
    private val multiSelectHelper: MultiSelectBookmarksHelper,
    private val settings: Settings,
    private val playbackManager: PlaybackManager,
    private val theme: Theme,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _showOptionsDialog = MutableSharedFlow<Int>()
    val showOptionsDialog = _showOptionsDialog.asSharedFlow()

    private var sourceView: SourceView = SourceView.UNKNOWN

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadBookmarks(
        episodeUuid: String,
        sourceView: SourceView,
    ) {
        this.sourceView = sourceView
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch(ioDispatcher) {
            userManager.getSignInState().asFlow().collectLatest {
                if (!it.isSignedInAsPlusOrPatron) {
                    _uiState.value = UiState.PlusUpsell(sourceView)
                } else {
                    episodeManager.findEpisodeByUuid(episodeUuid)?.let { episode ->
                        val bookmarksFlow =
                            settings.bookmarkSortTypeForPlayerFlow.flatMapLatest { sortType ->
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
                                UiState.Empty(sourceView)
                            } else {
                                UiState.Loaded(
                                    bookmarks = bookmarks,
                                    isMultiSelecting = isMultiSelecting,
                                    isSelected = { selectedBookmark ->
                                        selectedList.map { bookmark -> bookmark.uuid }
                                            .contains(selectedBookmark.uuid)
                                    },
                                    onRowClick = ::onRowClick,
                                    sourceView = sourceView,
                                )
                            }
                        }.stateIn(viewModelScope)
                    } ?: run { // This shouldn't happen in the ideal world
                        LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Episode not found.")
                        _uiState.value = UiState.Empty(sourceView)
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
            _showOptionsDialog.emit(settings.getBookmarksSortTypeForPlayer().labelId)
        }
    }

    fun changeSortOrder(order: BookmarksSortType) {
        if (order !is BookmarksSortTypeForPlayer) return
        settings.setBookmarksSortType(order)
    }

    fun play(bookmark: Bookmark) {
        val time = bookmark.timeSecs
        viewModelScope.launch {
            val bookmarkEpisode = episodeManager.findEpisodeByUuid(bookmark.episodeUuid)
            bookmarkEpisode?.let {
                val shouldPlayEpisode = !playbackManager.isPlaying() ||
                    playbackManager.getCurrentEpisode()?.uuid != bookmarkEpisode.uuid
                if (shouldPlayEpisode) {
                    playbackManager.playNow(it, sourceView = SourceView.PODCAST_LIST)
                }
            }
            playbackManager.seekToTimeMs(positionMs = time * 1000)
        }
    }

    fun buildBookmarkArguments(onSuccess: (BookmarkArguments) -> Unit) {
        (_uiState.value as? UiState.Loaded)?.let {
            val bookmark =
                it.bookmarks.firstOrNull { bookmark -> multiSelectHelper.isSelected(bookmark) }
            bookmark?.let {
                val episodeUuid = bookmark.episodeUuid
                viewModelScope.launch(ioDispatcher) {
                    val podcast = podcastManager.findPodcastByUuidSuspend(bookmark.podcastUuid)
                    val timeSecs = bookmark.timeSecs
                    val backgroundColor =
                        if (podcast == null) 0xFF000000.toInt() else theme.playerBackgroundColor(podcast)
                    val tintColor =
                        if (podcast == null) 0xFFFFFFFF.toInt() else theme.playerHighlightColor(podcast)
                    val arguments = BookmarkArguments(
                        bookmarkUuid = bookmark.uuid,
                        episodeUuid = episodeUuid,
                        timeSecs = timeSecs,
                        backgroundColor = backgroundColor,
                        tintColor = tintColor
                    )
                    onSuccess(arguments)
                }
            }
        }
    }

    sealed class UiState {
        data class Empty(val sourceView: SourceView) : UiState() {
            val colors: NoBookmarksViewColors
                get() = when (sourceView) {
                    SourceView.PLAYER -> NoBookmarksViewColors.Player
                    else -> NoBookmarksViewColors.Default
                }
        }

        object Loading : UiState()
        data class Loaded(
            val bookmarks: List<Bookmark> = emptyList(),
            val isMultiSelecting: Boolean,
            val isSelected: (Bookmark) -> Boolean,
            val onRowClick: (Bookmark) -> Unit,
            val sourceView: SourceView,
        ) : UiState() {
            val headerRowColors: HeaderRowColors
                get() = when (sourceView) {
                    SourceView.PLAYER -> HeaderRowColors.Player
                    else -> HeaderRowColors.Default
                }
            val bookmarkRowColors: BookmarkRowColors
                get() = when (sourceView) {
                    SourceView.PLAYER -> BookmarkRowColors.Player
                    else -> BookmarkRowColors.Default
                }
            val timePlayButtonStyle: TimePlayButtonStyle
                get() = when (sourceView) {
                    SourceView.PLAYER -> TimePlayButtonStyle.Solid
                    else -> TimePlayButtonStyle.Outlined
                }
        }

        data class PlusUpsell(val sourceView: SourceView) : UiState() {
            val colors: MessageViewColors
                get() = when (sourceView) {
                    SourceView.PLAYER -> MessageViewColors.Player
                    else -> MessageViewColors.Default
                }
        }
    }
}
