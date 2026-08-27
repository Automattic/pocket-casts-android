package au.com.shiftyjelly.pocketcasts.search

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.TvCategoryPodcasts
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverEpisode
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedAnalytics
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedLoader
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverPodcast
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.discover.TvOpenedCategory
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchHistoryEntry
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.search.ImprovedSearchManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SearchEmptyResultsEvent
import com.automattic.eventhorizon.SearchFailedEvent
import com.automattic.eventhorizon.SearchFilterTappedEvent
import com.automattic.eventhorizon.SearchHistoryItemTappedEvent
import com.automattic.eventhorizon.SearchHistoryType
import com.automattic.eventhorizon.SearchPerformedEvent
import com.automattic.eventhorizon.SearchPredictiveTermTappedEvent
import com.automattic.eventhorizon.SearchResultFilterType
import com.automattic.eventhorizon.SearchResultTappedEvent
import com.automattic.eventhorizon.SearchResultType
import com.automattic.eventhorizon.SearchShownEvent
import com.automattic.eventhorizon.SourceViewType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class TvSearchViewModel @Inject constructor(
    private val discoverFeedLoader: TvDiscoverFeedLoader,
    private val syncManager: SyncManager,
    private val improvedSearchManager: ImprovedSearchManager,
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val searchHistoryManager: SearchHistoryManager,
    private val folderManager: FolderManager,
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
) : ViewModel() {

    private val discoverFeedAnalytics = TvDiscoverFeedAnalytics(eventHorizon, settings, SOURCE_SEARCH, localRowIds = emptySet())

    private val _categories = MutableStateFlow<List<DiscoverCategory>>(emptyList())
    val categories: StateFlow<List<DiscoverCategory>> = _categories.asStateFlow()

    private val _discoverRows = MutableStateFlow<List<TvDiscoverRow>>(emptyList())
    val discoverRows: StateFlow<List<TvDiscoverRow>> = _discoverRows.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchState = MutableStateFlow<TvSearchState>(TvSearchState.Idle)
    val searchState: StateFlow<TvSearchState> = _searchState.asStateFlow()

    private val _filter = MutableStateFlow(TvSearchFilter.TopResults)
    val filter: StateFlow<TvSearchFilter> = _filter.asStateFlow()

    private val _hasFolderResults = MutableStateFlow(false)
    val hasFolderResults: StateFlow<Boolean> = _hasFolderResults.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _playStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playStarted: SharedFlow<Unit> = _playStarted.asSharedFlow()

    private val _playFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playFailures: SharedFlow<Unit> = _playFailures.asSharedFlow()

    private val _actionsEpisode = MutableStateFlow<PodcastEpisode?>(null)
    val actionsEpisode: StateFlow<PodcastEpisode?> = _actionsEpisode.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val discover = try {
                discoverFeedLoader.searchDiscoverFeed()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to load TV search discover feed")
                return@launch
            }
            // Publish categories and rows independently so the categories row (2 requests) does not
            // wait for the whole row fan-out (~10-20 requests) to resolve.
            launch { _categories.value = discoverFeedLoader.loadCategories(discover) }
            launch {
                _discoverRows.value = try {
                    discoverFeedLoader.buildRows(discover, syncManager.isLoggedIn())
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Timber.e(exception, "Failed to load TV search discover rows")
                    emptyList()
                }
            }
        }
        viewModelScope.launch {
            try {
                refreshHistory()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to load TV search history")
            }
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
        searchJob?.cancel()
        val term = query.trim()
        if (term.isEmpty()) {
            _suggestions.value = emptyList()
            _searchState.value = TvSearchState.Idle
            updateFolderResults(hasFolders = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _searchState.value = TvSearchState.Searching
            eventHorizon.track(SearchPerformedEvent(source = SourceViewType.Search))
            _searchState.value = try {
                val fullSearch = async { runCatching { improvedSearchManager.combinedSearch(term) } }
                val foldersSearch = async {
                    try {
                        searchFolders(term)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        Timber.e(exception, "Failed to search TV folders")
                        emptyList()
                    }
                }
                val localPodcasts = podcastManager.findSubscribedFlow(term).first().map(Podcast::toSearchItem)
                val localUuids = localPodcasts.mapTo(HashSet(), ImprovedSearchResultItem.PodcastItem::uuid)
                val predictiveResults = try {
                    improvedSearchManager.autoCompleteSearch(term)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Timber.e(exception, "Failed to load TV search suggestions")
                    emptyList()
                }
                _suggestions.value = predictiveResults.filterIsInstance<SearchAutoCompleteItem.Term>().map { it.term }
                val predictivePodcasts = predictiveResults.filterIsInstance<SearchAutoCompleteItem.Podcast>().map { it.toSearchItem() }
                val earlyPodcasts = (predictivePodcasts + localPodcasts)
                    .distinctBy(ImprovedSearchResultItem.PodcastItem::uuid)
                    .map { if (it.uuid in localUuids) it.copy(isFollowed = true) else it }
                if (earlyPodcasts.isNotEmpty()) {
                    _searchState.value = TvSearchState.Results(podcasts = earlyPodcasts, episodes = emptyList(), isPartial = true)
                }

                val remoteResults = fullSearch.await().getOrThrow()
                val remotePodcasts = remoteResults.filterIsInstance<ImprovedSearchResultItem.PodcastItem>()
                val podcasts = (predictivePodcasts + remotePodcasts + localPodcasts)
                    .distinctBy(ImprovedSearchResultItem.PodcastItem::uuid)
                    .map { if (it.uuid in localUuids) it.copy(isFollowed = true) else it }
                val episodes = remoteResults.filterIsInstance<ImprovedSearchResultItem.EpisodeItem>()
                    .distinctBy(ImprovedSearchResultItem.EpisodeItem::uuid)
                val folders = foldersSearch.await()
                updateFolderResults(hasFolders = folders.isNotEmpty())
                if (podcasts.isEmpty() && episodes.isEmpty() && folders.isEmpty()) {
                    eventHorizon.track(SearchEmptyResultsEvent(source = SourceViewType.Search, term = term))
                    TvSearchState.NoResults
                } else {
                    TvSearchState.Results(podcasts = podcasts, episodes = episodes, folders = folders)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to search on TV")
                updateFolderResults(hasFolders = false)
                eventHorizon.track(SearchFailedEvent(source = SourceViewType.Search, term = term))
                TvSearchState.Error
            }
        }
    }

    fun onFilterSelected(filter: TvSearchFilter) {
        if (_filter.value == filter) {
            return
        }
        _filter.value = filter
        eventHorizon.track(SearchFilterTappedEvent(source = SourceViewType.Search, filter = filter.analyticsValue))
    }

    private fun updateFolderResults(hasFolders: Boolean) {
        _hasFolderResults.value = hasFolders
        if (!hasFolders && _filter.value == TvSearchFilter.Folders) {
            _filter.value = TvSearchFilter.TopResults
        }
    }

    private suspend fun searchFolders(term: String): List<FolderItem.Folder> {
        if (!syncManager.isLoggedIn()) {
            return emptyList()
        }
        return folderManager.getAll()
            .filter { it.name.contains(term, ignoreCase = true) }
            .sortedBy { PodcastsSortType.cleanStringForSort(it.name) }
            .map { folder -> FolderItem.Folder(folder = folder, podcasts = folderManager.findFolderPodcastsSorted(folder.uuid)) }
    }

    suspend fun folderPodcasts(folderUuid: String): List<Podcast> {
        return folderManager.findFolderPodcastsSorted(folderUuid)
    }

    fun selectSuggestion(term: String) {
        eventHorizon.track(SearchPredictiveTermTappedEvent(source = SourceViewType.Search, term = term))
        saveSearchTerm(term)
        onQueryChange(term)
    }

    fun selectHistoryItem(term: String) {
        eventHorizon.track(SearchHistoryItemTappedEvent(source = SourceViewType.Search, type = SearchHistoryType.SearchTerm))
        onQueryChange(term)
    }

    fun trackPodcastResultTapped(podcast: ImprovedSearchResultItem.PodcastItem) {
        eventHorizon.track(
            SearchResultTappedEvent(
                source = SourceViewType.Search,
                uuid = podcast.uuid,
                resultType = if (podcast.isFollowed) SearchResultType.PodcastLocalResult else SearchResultType.PodcastRemoteResult,
            ),
        )
    }

    fun trackEpisodeResultTapped(episodeUuid: String) {
        eventHorizon.track(
            SearchResultTappedEvent(source = SourceViewType.Search, uuid = episodeUuid, resultType = SearchResultType.Episode),
        )
    }

    fun trackSearchShown() {
        eventHorizon.track(SearchShownEvent(source = SourceViewType.Search))
    }

    fun trackDiscoverListShown(row: TvDiscoverRow) = discoverFeedAnalytics.trackListImpression(row)

    fun trackDiscoverPodcastTapped(row: TvDiscoverRow, podcast: TvDiscoverPodcast) = discoverFeedAnalytics.trackPodcastTapped(row, podcast)

    fun trackDiscoverEpisodePodcastTapped(row: TvDiscoverRow, episode: TvDiscoverEpisode) = discoverFeedAnalytics.trackEpisodePodcastTapped(row, episode)

    fun trackCategoryPodcastTapped(category: TvOpenedCategory, listId: String?, podcast: TvDiscoverPodcast) = discoverFeedAnalytics.trackCategoryPodcastTapped(category, listId, podcast)

    fun trackCategoryPillTapped(category: DiscoverCategory, index: Int) = discoverFeedAnalytics.trackCategoryPillTapped(category, index)

    fun saveSearchTerm(term: String) {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) {
            return
        }
        viewModelScope.launch {
            try {
                searchHistoryManager.add(SearchHistoryEntry.SearchTerm(term = trimmed))
                refreshHistory()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to save TV search history")
            }
        }
    }

    private suspend fun refreshHistory() {
        _history.value = searchHistoryManager.findAll(showFolders = false)
            .filterIsInstance<SearchHistoryEntry.SearchTerm>()
            .map(SearchHistoryEntry.SearchTerm::term)
    }

    suspend fun categoryPodcasts(categoryId: Int, source: String): TvCategoryPodcasts {
        return discoverFeedLoader.loadCategoryPodcasts(source, categoryId, syncManager.isLoggedIn())
    }

    fun playEpisode(episode: ImprovedSearchResultItem.EpisodeItem) {
        viewModelScope.launch {
            try {
                val found = hydrate(episode)
                if (found != null) {
                    playbackManager.playNowSuspend(episode = found, sourceView = SourceView.SEARCH_RESULTS)
                    _playStarted.tryEmit(Unit)
                } else {
                    Timber.e("Episode %s not found to play from TV search", episode.uuid)
                    _playFailures.tryEmit(Unit)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to play episode from TV search")
                _playFailures.tryEmit(Unit)
            }
        }
    }

    fun openEpisodeActions(episode: ImprovedSearchResultItem.EpisodeItem) {
        viewModelScope.launch {
            try {
                val found = hydrate(episode)
                if (found != null) {
                    _actionsEpisode.value = found
                } else {
                    Timber.e("Episode %s not found to open actions from TV search", episode.uuid)
                    _playFailures.tryEmit(Unit)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to open episode actions from TV search")
                _playFailures.tryEmit(Unit)
            }
        }
    }

    fun dismissEpisodeActions() {
        _actionsEpisode.value = null
    }

    private suspend fun hydrate(episode: ImprovedSearchResultItem.EpisodeItem): PodcastEpisode? {
        return episodeManager.findByUuid(episode.uuid)
            ?: run {
                podcastManager.findOrDownloadPodcastRxSingle(episode.podcastUuid).await()
                episodeManager.findByUuid(episode.uuid)
            }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val SOURCE_SEARCH = "search"
    }
}

private val TvSearchFilter.analyticsValue
    get() = when (this) {
        TvSearchFilter.TopResults -> SearchResultFilterType.AllResults
        TvSearchFilter.Podcasts -> SearchResultFilterType.Podcasts
        TvSearchFilter.Episodes -> SearchResultFilterType.Episodes
        TvSearchFilter.Folders -> SearchResultFilterType.Unknown
    }

private fun Podcast.toSearchItem() = ImprovedSearchResultItem.PodcastItem(
    uuid = uuid,
    title = title,
    author = author,
    isFollowed = true,
    isExplicit = explicit == true,
)

private fun SearchAutoCompleteItem.Podcast.toSearchItem() = ImprovedSearchResultItem.PodcastItem(
    uuid = uuid,
    title = title,
    author = author,
    isFollowed = isSubscribed,
    isExplicit = isExplicit,
)

enum class TvSearchFilter(
    @StringRes val labelRes: Int,
) {
    TopResults(LR.string.search_filters_top_results),
    Podcasts(LR.string.search_filters_podcasts),
    Episodes(LR.string.search_filters_episodes),
    Folders(LR.string.search_filters_folders),
}

sealed interface TvSearchState {
    data object Idle : TvSearchState
    data object Searching : TvSearchState
    data object NoResults : TvSearchState
    data object Error : TvSearchState
    data class Results(
        val podcasts: List<ImprovedSearchResultItem.PodcastItem>,
        val episodes: List<ImprovedSearchResultItem.EpisodeItem>,
        val folders: List<FolderItem.Folder> = emptyList(),
        val isPartial: Boolean = false,
    ) : TvSearchState
}
