package au.com.shiftyjelly.pocketcasts.player.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.bookmark.BookmarkRowColors
import au.com.shiftyjelly.pocketcasts.compose.buttons.TimePlayButtonStyle
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkArguments
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.HeaderRowColors
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.MessageViewColors
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.NoBookmarksViewColors
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
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper.ShareType
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.combine6
import au.com.shiftyjelly.pocketcasts.utils.featureflag.BookmarkFeatureControl
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
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
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val bookmarkManager: BookmarkManager,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    val multiSelectHelper: MultiSelectBookmarksHelper,
    private val settings: Settings,
    private val playbackManager: PlaybackManager,
    private val theme: Theme,
    private val bookmarkFeature: BookmarkFeatureControl,
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
        combine6(
            bookmarksFlow,
            isMultiSelectingFlow,
            selectedListFlow,
            settings.cachedSubscriptionStatus.flow,
            settings.artworkConfiguration.flow,
            bookmarkSearchResults,
        ) { bookmarks, isMultiSelecting, selectedList, cachedSubscriptionStatus, artworkConfiguration, searchResults ->
            val userTier = (cachedSubscriptionStatus as? SubscriptionStatus.Paid)?.tier?.toUserTier() ?: UserTier.Free
            _uiState.value = if (!bookmarkFeature.isAvailable(userTier)) {
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
                } ?: run { // This shouldn't happen in the ideal world
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

    fun onShareClicked(context: Context) {
        (_uiState.value as? UiState.Loaded)?.let {
            val bookmark =
                it.bookmarks.firstOrNull { bookmark -> multiSelectHelper.isSelected(bookmark) }
            bookmark?.let {
                viewModelScope.launch {
                    val podcast = podcastManager.findPodcastByUuidSuspend(bookmark.podcastUuid)
                    val episode = episodeManager.findEpisodeByUuid(bookmark.episodeUuid)
                    if (podcast != null && episode is PodcastEpisode) {
                        SharePodcastHelper(
                            podcast,
                            episode,
                            bookmark.timeSecs.toDouble(),
                            context,
                            ShareType.BOOKMARK_TIME,
                            sourceView,
                            analyticsTracker,
                        ).showShareDialogDirect()
                    }
                }
            }
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

    fun buildBookmarkArguments(onSuccess: (BookmarkArguments) -> Unit) {
        (_uiState.value as? UiState.Loaded)?.let {
            val bookmark =
                it.bookmarks.firstOrNull { bookmark -> multiSelectHelper.isSelected(bookmark) }
            bookmark?.let {
                val episodeUuid = bookmark.episodeUuid
                viewModelScope.launch(ioDispatcher) {
                    val podcast = podcastManager.findPodcastByUuidSuspend(bookmark.podcastUuid)
                    val backgroundColor =
                        if (podcast == null) 0xFF000000.toInt() else theme.playerBackgroundColor(podcast)
                    val tintColor =
                        if (podcast == null) 0xFFFFFFFF.toInt() else theme.playerHighlightColor(podcast)
                    val arguments = BookmarkArguments(
                        bookmarkUuid = bookmark.uuid,
                        episodeUuid = episodeUuid,
                        timeSecs = bookmark.timeSecs,
                        backgroundColor = backgroundColor,
                        tintColor = tintColor,
                    )
                    onSuccess(arguments)
                }
            }
        }
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

    sealed class UiState {
        data class Empty(val sourceView: SourceView) : UiState() {
            val colors: NoBookmarksViewColors
                get() = when (sourceView) {
                    SourceView.PLAYER -> NoBookmarksViewColors.Player
                    else -> NoBookmarksViewColors.Default
                }
        }

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

        data class Upsell(val sourceView: SourceView) : UiState() {
            val colors: MessageViewColors
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
