package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkArguments
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.search.BookmarkSearchHandler
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortType
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeDefault
import au.com.shiftyjelly.pocketcasts.preferences.model.BookmarksSortTypeForProfile
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.combine
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@HiltViewModel
class BookmarksViewModel
@Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val bookmarkManager: BookmarkManager,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    val multiSelectHelper: MultiSelectBookmarksHelper,
    private val settings: Settings,
    private val playbackManager: PlaybackManager,
    private val theme: Theme,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val bookmarkSearchHandler: BookmarkSearchHandler,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _showOptionsDialog = MutableSharedFlow<Int>()
    val showOptionsDialog = _showOptionsDialog.asSharedFlow()

    private val _message = MutableSharedFlow<BookmarkMessage>()
    val message = _message.asSharedFlow()

    private var isFragmentActive: Boolean = true

    private var sourceView: SourceView = SourceView.UNKNOWN
        set(value) {
            field = value
            multiSelectHelper.source = value
        }

    private val multiSelectListener = object : MultiSelectHelper.Listener<Bookmark> {
        override fun multiSelectSelectAll() {
            (_uiState.value as? UiState.Loaded)?.bookmarks?.let {
                multiSelectHelper.selectAllInList(it)
            }
        }

        override fun multiSelectSelectNone() {
            (_uiState.value as? UiState.Loaded)?.bookmarks?.let { bookmarks ->
                multiSelectHelper.deselectAllInList(bookmarks)
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
                    val bookmarksBelow = bookmarks.subList(startIndex, bookmarks.size)
                    multiSelectHelper.deselectAllInList(bookmarksBelow)
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
                            bookmarks.size,
                        ),
                    )
                }
            }
        }
    }

    init {
        multiSelectHelper.listener = multiSelectListener
    }

    fun loadBookmarks(
        episodeUuid: String?,
        sourceView: SourceView,
    ) {
        this.sourceView = sourceView
        viewModelScope.coroutineContext.cancelChildren()
        viewModelScope.launch(ioDispatcher) {
            setupUiStateFlow(episodeUuid)
        }
    }

    private suspend fun setupUiStateFlow(
        episodeUuid: String?,
    ) {
        val bookmarksFlow = getBookmarksFlow(episodeUuid, sourceView)
        val episode = episodeUuid?.let { episodeManager.findEpisodeByUuid(episodeUuid) }
        val isMultiSelectingFlow = multiSelectHelper.isMultiSelectingLive.asFlow()
        val selectedListFlow = multiSelectHelper.selectedListLive.asFlow()
        val bookmarkSearchResults = bookmarkSearchHandler.getBookmarkSearchResultsFlow()
        combine(
            bookmarksFlow,
            isMultiSelectingFlow,
            selectedListFlow,
            settings.cachedSubscription.flow,
            settings.artworkConfiguration.flow,
            bookmarkSearchResults,
        ) { bookmarks, isMultiSelecting, selectedList, subscription, artworkConfiguration, searchResults ->
            val isPaidUser = subscription != null
            _uiState.value = if (!isPaidUser) {
                UiState.Upsell(sourceView)
            } else if (bookmarks.isEmpty()) {
                UiState.Empty(sourceView)
            } else {
                val searchText = (_uiState.value as? UiState.Loaded)?.searchText ?: ""
                val filteredBookmarks = if (searchResults.searchTerm.isNotEmpty()) {
                    bookmarks.filter { bookmark -> searchResults.searchUuids?.contains(bookmark.uuid) == true }
                } else {
                    bookmarks
                }
                val episodes = episode?.let { listOf(it) }
                    ?: episodeManager.findEpisodesByUuids(filteredBookmarks.map { it.episodeUuid }.distinct())
                val bookmarkIdAndEpisodeMap = filteredBookmarks.associate { bookmark ->
                    bookmark.uuid to episodes.firstOrNull { it.uuid == bookmark.episodeUuid }
                }
                UiState.Loaded(
                    bookmarks = filteredBookmarks,
                    bookmarkIdAndEpisodeMap = bookmarkIdAndEpisodeMap,
                    isMultiSelecting = isMultiSelecting,
                    useEpisodeArtwork = artworkConfiguration.useEpisodeArtwork(Element.Bookmarks),
                    isSelected = { selectedBookmark ->
                        selectedList.map { bookmark -> bookmark.uuid }
                            .contains(selectedBookmark.uuid)
                    },
                    onRowClick = ::onRowClick,
                    sourceView = sourceView,
                    showIcon = sourceView == SourceView.PROFILE,
                    searchEnabled = sourceView == SourceView.PROFILE,
                    searchText = searchText,
                    showEpisodeTitle = sourceView == SourceView.PROFILE,
                )
            }
        }.stateIn(viewModelScope)
            // Stop collecting on player close when viewModelScope is still active but fragment is not.
            .takeWhile { !isFragmentActive }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getBookmarksFlow(
        episodeUuid: String?,
        sourceView: SourceView,
    ): Flow<List<Bookmark>> {
        val bookmarksSortTypeFlow = sourceView.mapToBookmarksSortTypeUserSetting().flow
        return bookmarksSortTypeFlow.flatMapLatest { sortType ->
            if (episodeUuid == null) {
                if (sortType is BookmarksSortTypeForProfile) {
                    bookmarkManager.findBookmarksFlow(sortType)
                } else {
                    flowOf(emptyList())
                }
            } else {
                episodeManager.findEpisodeByUuid(episodeUuid)?.let {
                    bookmarkManager.findEpisodeBookmarksFlow(
                        episode = it,
                        sortType = sortType as BookmarksSortTypeDefault,
                    )
                } ?: run {
                    // This shouldn't happen in the ideal world
                    LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Episode not found.")
                    flowOf(emptyList())
                }
            }
        }
    }

    fun onPlayerOpen() {
        isFragmentActive = true
        multiSelectHelper.listener = multiSelectListener
    }

    fun onPlayerClose() {
        isFragmentActive = false
        multiSelectHelper.listener = null
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
            _showOptionsDialog.emit(sourceView.mapToBookmarksSortTypeUserSetting().flow.value.labelId)
        }
    }

    suspend fun getSharedBookmark(): Triple<Podcast, PodcastEpisode, Bookmark>? {
        return (_uiState.value as? UiState.Loaded)?.let {
            val bookmark = it.bookmarks.firstOrNull { bookmark -> multiSelectHelper.isSelected(bookmark) } ?: return null
            val podcast = podcastManager.findPodcastByUuid(bookmark.podcastUuid) ?: return null
            val episode = episodeManager.findEpisodeByUuid(bookmark.episodeUuid) as? PodcastEpisode ?: return null
            Triple(podcast, episode, bookmark)
        }
    }

    fun onSearchTextChanged(searchText: String) {
        (uiState.value as? UiState.Loaded)?.let {
            _uiState.value = it.copy(searchText = searchText)
            bookmarkSearchHandler.searchQueryUpdated(searchText.trim())
        }
    }

    fun changeSortOrder(order: BookmarksSortType) {
        sourceView.mapToBookmarksSortTypeUserSetting().set(order, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.BOOKMARKS_SORT_BY_CHANGED,
            mapOf(
                "sort_order" to order.key,
                "source" to sourceView.analyticsValue,
            ),
        )
    }

    fun play(bookmark: Bookmark) {
        viewModelScope.launch {
            val bookmarkEpisode = episodeManager.findEpisodeByUuid(bookmark.episodeUuid)
            bookmarkEpisode?.let {
                val shouldLoadOrSwitchEpisode = !playbackManager.isPlaying() ||
                    playbackManager.getCurrentEpisode()?.uuid != bookmarkEpisode.uuid
                if (shouldLoadOrSwitchEpisode) {
                    playbackManager.playNowSync(it, sourceView = sourceView)
                }
            } ?: run {
                _message.emit(BookmarkMessage.BookmarkEpisodeNotFound)
                return@launch
            }
            _message.emit(BookmarkMessage.PlayingBookmark(bookmark.title))
            playbackManager.seekToTimeMs(positionMs = bookmark.timeSecs * 1000)
            analyticsTracker.track(
                AnalyticsEvent.BOOKMARK_PLAY_TAPPED,
                mapOf(
                    "source" to sourceView.analyticsValue,
                    "episode_uuid" to bookmark.episodeUuid,
                ),
            )
        }
    }

    suspend fun createBookmarkArguments(): BookmarkArguments? {
        val loadedState = _uiState.value as? UiState.Loaded ?: return null
        val bookmark = loadedState.bookmarks.firstOrNull(multiSelectHelper::isSelected) ?: return null
        val podcast = podcastManager.findPodcastByUuid(bookmark.podcastUuid)
        return BookmarkArguments(
            bookmarkUuid = bookmark.uuid,
            episodeUuid = bookmark.episodeUuid,
            timeSecs = bookmark.timeSecs,
            podcastColors = podcast?.let(::PodcastColors) ?: PodcastColors.ForUserEpisode,
        )
    }

    private fun SourceView.mapToBookmarksSortTypeUserSetting(): UserSetting<BookmarksSortType> {
        val sortType = when (this) {
            SourceView.PLAYER -> settings.playerBookmarksSortType
            SourceView.PROFILE -> settings.profileBookmarksSortType
            else -> settings.episodeBookmarksSortType
        }
        @Suppress("UNCHECKED_CAST")
        return sortType as UserSetting<BookmarksSortType>
    }

    fun searchBarClearButtonTapped() {
        analyticsTracker.track(AnalyticsEvent.BOOKMARKS_SEARCHBAR_CLEAR_BUTTON_TAPPED)
    }

    fun onHeadphoneControlsButtonTapped() {
        analyticsTracker.track(
            AnalyticsEvent.BOOKMARKS_EMPTY_GO_TO_HEADPHONE_SETTINGS,
            mapOf("source" to sourceView.analyticsValue),
        )
    }

    fun onGetBookmarksButtonTapped() {
        analyticsTracker.track(
            AnalyticsEvent.BOOKMARKS_GET_BOOKMARKS_BUTTON_TAPPED,
            mapOf("source" to sourceView.analyticsValue),
        )
    }

    fun onShare(podcastUuid: String, episodeUuid: String, source: SourceView) {
        analyticsTracker.track(AnalyticsEvent.BOOKMARK_SHARE_TAPPED, mapOf("podcast_uuid" to podcastUuid, "episode_uuid" to episodeUuid, "source" to source.analyticsValue))
    }

    sealed class UiState {
        data class Empty(val sourceView: SourceView) : UiState()
        data object Loading : UiState()
        data class Loaded(
            val bookmarks: List<Bookmark> = emptyList(),
            val bookmarkIdAndEpisodeMap: Map<String, BaseEpisode?>,
            val isMultiSelecting: Boolean,
            val useEpisodeArtwork: Boolean,
            val isSelected: (Bookmark) -> Boolean,
            val onRowClick: (Bookmark) -> Unit,
            val sourceView: SourceView,
            val showIcon: Boolean = false,
            val searchText: String = "",
            val searchEnabled: Boolean = false,
            val showEpisodeTitle: Boolean = false,
        ) : UiState()

        data class Upsell(val sourceView: SourceView) : UiState() {
            internal val colors: MessageViewColors
                get() = when (sourceView) {
                    SourceView.PLAYER -> MessageViewColors.Player
                    else -> MessageViewColors.Default
                }
        }
    }

    sealed class BookmarkMessage {
        data object BookmarkEpisodeNotFound : BookmarkMessage()
        data class PlayingBookmark(val bookmarkTitle: String) : BookmarkMessage()
    }
}

internal sealed class MessageViewColors {
    @Composable
    abstract fun backgroundColor(): Color

    @Composable
    abstract fun textColor(): Color

    @Composable
    abstract fun buttonTextColor(): Color

    object Default : MessageViewColors() {
        @Composable
        override fun backgroundColor(): Color = MaterialTheme.theme.colors.primaryUi01Active

        @Composable
        override fun textColor(): Color = MaterialTheme.theme.colors.primaryText02

        @Composable
        override fun buttonTextColor(): Color = MaterialTheme.theme.colors.primaryInteractive01
    }

    object Player : MessageViewColors() {
        @Composable
        override fun backgroundColor(): Color = MaterialTheme.theme.colors.playerContrast06

        @Composable
        override fun textColor(): Color = MaterialTheme.theme.colors.playerContrast02

        @Composable
        override fun buttonTextColor(): Color = MaterialTheme.theme.colors.playerContrast01
    }
}
