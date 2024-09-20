package au.com.shiftyjelly.pocketcasts.podcasts.helper.search

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManagerImpl
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class EpisodeSearchHandler @Inject constructor(
    settings: Settings,
    private val cacheServiceManager: PodcastCacheServiceManagerImpl,
    private val analyticsTracker: AnalyticsTracker,
) : SearchHandler<BaseEpisode>() {
    private val searchDebounce = settings.getEpisodeSearchDebounceMs()

    override fun getSearchResultsObservable(podcastUuid: String): Observable<SearchResult> =
        searchQueryRelay.debounce { // Only debounce when search has a value otherwise it slows down loading the pages
            if (it.isEmpty()) {
                Observable.empty()
            } else {
                Observable.timer(searchDebounce, TimeUnit.MILLISECONDS)
            }
        }.switchMapSingle { searchTerm ->
            if (searchTerm.length > 2) {
                cacheServiceManager.searchEpisodes(podcastUuid, searchTerm)
                    .map { SearchResult(searchTerm, it) }
                    .onErrorReturnItem(noSearchResult)
            } else {
                Single.just(noSearchResult)
            }
        }.distinctUntilChanged()

    override fun trackSearchIfNeeded(oldValue: String, newValue: String) {
        if (oldValue.isEmpty() && newValue.isNotEmpty()) {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SEARCH_PERFORMED)
        } else if (oldValue.isNotEmpty() && newValue.isEmpty()) {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SEARCH_CLEARED)
        }
    }
}
