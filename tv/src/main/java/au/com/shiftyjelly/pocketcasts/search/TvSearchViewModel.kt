package au.com.shiftyjelly.pocketcasts.search

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.TvCategoryPodcasts
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedLoader
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverPodcast
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.search.ImprovedSearchManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
) : ViewModel() {

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
    }

    fun onQueryChange(query: String) {
        _query.value = query
        searchJob?.cancel()
        val term = query.trim()
        if (term.isEmpty()) {
            _searchState.value = TvSearchState.Idle
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _searchState.value = TvSearchState.Searching
            _searchState.value = try {
                val localPodcasts = podcastManager.findSubscribedFlow(term).first().map(Podcast::toSearchItem)
                val remoteResults = improvedSearchManager.combinedSearch(term)
                val podcasts = (localPodcasts + remoteResults.filterIsInstance<ImprovedSearchResultItem.PodcastItem>())
                    .distinctBy(ImprovedSearchResultItem.PodcastItem::uuid)
                val episodes = remoteResults.filterIsInstance<ImprovedSearchResultItem.EpisodeItem>()
                    .distinctBy(ImprovedSearchResultItem.EpisodeItem::uuid)
                if (podcasts.isEmpty() && episodes.isEmpty()) {
                    TvSearchState.NoResults
                } else {
                    TvSearchState.Results(podcasts = podcasts, episodes = episodes)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to search on TV")
                TvSearchState.Error
            }
        }
    }

    fun onFilterSelected(filter: TvSearchFilter) {
        _filter.value = filter
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
    }
}

private fun Podcast.toSearchItem() = ImprovedSearchResultItem.PodcastItem(
    uuid = uuid,
    title = title,
    author = author,
    isFollowed = true,
    isExplicit = explicit == true,
)

enum class TvSearchFilter(
    @StringRes val labelRes: Int,
) {
    TopResults(LR.string.search_filters_top_results),
    Podcasts(LR.string.search_filters_podcasts),
    Episodes(LR.string.search_filters_episodes),
}

sealed interface TvSearchState {
    data object Idle : TvSearchState
    data object Searching : TvSearchState
    data object NoResults : TvSearchState
    data object Error : TvSearchState
    data class Results(
        val podcasts: List<ImprovedSearchResultItem.PodcastItem>,
        val episodes: List<ImprovedSearchResultItem.EpisodeItem>,
    ) : TvSearchState
}
