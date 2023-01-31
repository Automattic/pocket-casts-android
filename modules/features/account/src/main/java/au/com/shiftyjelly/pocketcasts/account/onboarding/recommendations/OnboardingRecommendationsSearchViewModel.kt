package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.search.SearchHandler
import au.com.shiftyjelly.pocketcasts.search.SearchState
import au.com.shiftyjelly.pocketcasts.utils.Network
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class OnboardingRecommendationsSearchViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val searchHandler: SearchHandler,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {

    private val _state = MutableStateFlow(
        State(
            searchQuery = "",
            results = emptyList(),
            loading = false,
        )
    )
    val state: StateFlow<State> = _state

    data class State(
        val searchQuery: String,
        val results: List<PodcastResult>,
        val loading: Boolean,
    )

    data class PodcastResult(
        val podcast: Podcast,
        val isSubscribed: Boolean,
    )

    init {
        searchHandler.setSource(AnalyticsSource.ONBOARDING_RECOMMENDATIONS_SEARCH)
        searchHandler.setOnlySearchRemote(true)
        viewModelScope.launch {

            val subscribedUuidFlow = podcastManager
                .observeSubscribed()
                .asFlow()
                .map { ls ->
                    ls.map { it.uuid }
                }

            combine(
                subscribedUuidFlow,
                searchHandler.searchResults.asFlow()
            ) { subscribedUuids, searchState ->

                val podcasts = when (searchState) {
                    SearchState.NoResults -> emptyList()
                    is SearchState.Results -> {

                        // TODO handle loading
                        // TODO handle error

                        searchState.list
                            .filterIsInstance<FolderItem.Podcast>()
                            .map {
                                PodcastResult(
                                    podcast = it.podcast,
                                    isSubscribed = subscribedUuids.contains(it.podcast.uuid),
                                )
                            }
                    }
                }

                val isLoading = (searchState as? SearchState.Results)?.loading == true

                state.value.copy(
                    results = podcasts,
                    loading = isLoading,
                )
            }.stateIn(viewModelScope).collect {
                _state.value = it
            }
        }
    }

    fun updateSearchQuery(searchQuery: String) {
        _state.value = state.value.copy(searchQuery = searchQuery)
        searchHandler.updateSearchQuery(searchQuery)
    }

    fun queryImmediately(context: Context) {
        if (Network.isConnected(context)) {
            searchHandler.updateSearchQuery(state.value.searchQuery, immediate = true)
        } else {
            Toast.makeText(
                context,
                context.getString(LR.string.error_check_your_internet_connection),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun toggleSubscribed(podcastResult: PodcastResult) {
        val event: AnalyticsEvent
        val uuid = podcastResult.podcast.uuid
        if (podcastResult.isSubscribed) {
            event = AnalyticsEvent.PODCAST_UNSUBSCRIBED
            podcastManager.unsubscribeAsync(podcastUuid = uuid, playbackManager = playbackManager)
        } else {
            event = AnalyticsEvent.PODCAST_SUBSCRIBED
            podcastManager.subscribeToPodcast(podcastUuid = uuid, sync = true)
        }
        analyticsTracker.track(event, AnalyticsProp.podcastSubscribeToggled(uuid))

        _state.update {
            it.copy(
                results = it.results.map { podcast ->
                    if (podcast.podcast.uuid == uuid) {
                        podcast.copy(isSubscribed = !podcastResult.isSubscribed)
                    } else {
                        podcast
                    }
                }
            )
        }
    }

    companion object {
        private object AnalyticsProp {
            const val UUID = "uuid"
            const val SOURCE = "source"
            fun podcastSubscribeToggled(uuid: String) =
                mapOf(UUID to uuid, SOURCE to AnalyticsSource.ONBOARDING_RECOMMENDATIONS_SEARCH)
        }
    }
}
