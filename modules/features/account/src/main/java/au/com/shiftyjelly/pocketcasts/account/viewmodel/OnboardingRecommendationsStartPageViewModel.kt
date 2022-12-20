package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class OnboardingRecommendationsStartPageViewModel @Inject constructor(
    val podcastManager: PodcastManager,
    val playbackManager: PlaybackManager,
    val analyticsTracker: AnalyticsTrackerWrapper,
    repository: ListRepository,
    settings: Settings,
    app: Application,
) : AndroidViewModel(app) {

    data class State(val trendingPodcasts: List<RecommendationPodcast>, private val anySubscribed: Boolean) {
        val buttonRes = if (anySubscribed) {
            LR.string.navigation_continue
        } else {
            LR.string.not_now
        }

        companion object {
            val EMPTY = State(
                trendingPodcasts = emptyList(),
                anySubscribed = false
            )
        }
    }

    data class RecommendationPodcast(
        val uuid: String,
        val title: String,
        val isSubscribed: Boolean,
    )

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.EMPTY)
    val state: StateFlow<State> = _state

    init {
        viewModelScope.launch {

            val feed = try {
                repository.getDiscoverFeed().await()
            } catch (e: Exception) {
                Timber.e(e)
                return@launch
            }

            val regionCode = settings.getDiscoveryCountryCode()
            val region = feed.regions[regionCode]
                ?: feed.regions[feed.defaultRegionCode]

            if (region == null) {
                val message = "Could not get region $regionCode"
                Timber.e(message)
                return@launch
            }

            val replacements = mapOf(
                feed.regionCodeToken to region.code,
                feed.regionNameToken to region.name
            )

            val updatedList = feed.layout.transformWithRegion(
                region,
                replacements,
                getApplication<Application>().resources
            ) // Update the list with the correct region substituted in where needed

            val discoverPodcastList = updatedList
                .find { it.id == "trending" } // only care about the trending list
                ?.let { trendingRow ->
                    try {
                        repository
                            .getListFeed(trendingRow.source).await()
                            .podcasts
                    } catch (e: Exception) {
                        Timber.e(e)
                        null
                    }
                }

            if (discoverPodcastList == null) {
                Timber.e("Could not get trending podcast list")
                return@launch
            }

            podcastManager
                .observeSubscribed()
                .asFlow()
                .map { subscribed ->
                    val subscribedUuids = subscribed.map { it.uuid }
                    val trendingPodcasts = discoverPodcastList.map { discoverPodcast ->
                        RecommendationPodcast(
                            uuid = discoverPodcast.uuid,
                            title = discoverPodcast.title ?: "",
                            isSubscribed = discoverPodcast.uuid in subscribedUuids
                        )
                    }
                    State(
                        trendingPodcasts = trendingPodcasts,
                        anySubscribed = subscribed.isNotEmpty()
                    )
                }
                .stateIn(viewModelScope)
                .collectLatest { _state.value = it }
        }
    }

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.RECOMMENDATIONS_SHOWN)
    }

    fun onBackPressed() {
        viewModelScope.launch(Dispatchers.IO) {
            analyticsTracker.track(
                AnalyticsEvent.RECOMMENDATIONS_DISMISSED,
                mapOf(SUBSCRIPTIONS_PROP to podcastManager.countSubscribed())
            )
        }
    }

    fun onSearch() {
        analyticsTracker.track(AnalyticsEvent.RECOMMENDATIONS_SEARCH_TAPPED)
    }

    fun onImportClick() {
        analyticsTracker.track(AnalyticsEvent.RECOMMENDATIONS_IMPORT_TAPPED)
    }

    fun onComplete() {
        viewModelScope.launch(Dispatchers.IO) {
            analyticsTracker.track(
                AnalyticsEvent.RECOMMENDATIONS_CONTINUE_TAPPED,
                mapOf(SUBSCRIPTIONS_PROP to podcastManager.countSubscribed())
            )
        }
    }

    fun updateSubscribed(podcast: RecommendationPodcast) {
        if (podcast.isSubscribed) {
            podcastManager.unsubscribeAsync(podcastUuid = podcast.uuid, playbackManager = playbackManager)
        } else {
            podcastManager.subscribeToPodcast(podcastUuid = podcast.uuid, sync = true)
        }
    }

    companion object {
        private const val SUBSCRIPTIONS_PROP = "subscriptions"
    }
}
