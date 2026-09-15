package au.com.shiftyjelly.pocketcasts.podcasts.helper.search

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.DefaultDispatcher
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManagerImpl
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PodcastScreenSearchClearedEvent
import com.automattic.eventhorizon.PodcastScreenSearchPerformedEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

class EpisodeSearchHandler @Inject constructor(
    settings: Settings,
    private val cacheServiceManager: PodcastCacheServiceManagerImpl,
    private val eventHorizon: EventHorizon,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : SearchHandler<BaseEpisode>() {
    private val searchDebounce = settings.getEpisodeSearchDebounceMs().coerceAtLeast(0L)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    override fun getSearchResultsFlow(podcastUuid: String): Flow<SearchResult> = searchQueryFlow
        // Only debounce when search has a value otherwise it slows down loading the pages
        .debounce { if (it.isEmpty()) 0L else searchDebounce }
        .flatMapLatest { searchTerm ->
            if (searchTerm.length > 1) {
                flow {
                    val episodeUuids = cacheServiceManager.searchEpisodes(podcastUuid, searchTerm)
                    emit(SearchResult(searchTerm, episodeUuids))
                }.catch { emit(noSearchResult) }
            } else {
                flowOf(noSearchResult)
            }
        }
        // asFlowable() doesn't pick a thread, so keep results and the combineLatest downstream off the main thread
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()

    override fun trackSearchIfNeeded(oldValue: String, newValue: String) {
        val event = if (oldValue.isEmpty() && newValue.isNotEmpty()) {
            PodcastScreenSearchPerformedEvent
        } else if (oldValue.isNotEmpty() && newValue.isEmpty()) {
            PodcastScreenSearchClearedEvent
        } else {
            null
        }
        event?.let(eventHorizon::track)
    }
}
