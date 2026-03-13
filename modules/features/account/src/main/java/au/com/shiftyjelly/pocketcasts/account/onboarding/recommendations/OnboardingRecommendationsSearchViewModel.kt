package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.search.SearchHandler
import au.com.shiftyjelly.pocketcasts.search.SearchUiState
import au.com.shiftyjelly.pocketcasts.utils.Network
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PodcastSubscribedEvent
import com.automattic.eventhorizon.PodcastUnsubscribedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class OnboardingRecommendationsSearchViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val searchHandler: SearchHandler,
    private val analyticsTracker: AnalyticsTracker,
    private val eventHorizon: EventHorizon,
) : ViewModel() {

    private val _state = MutableStateFlow(
        State(
            searchQuery = "",
            results = emptyList(),
            loading = false,
        ),
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
        searchHandler.setSource(SourceView.ONBOARDING_RECOMMENDATIONS_SEARCH)
        searchHandler.setOnlySearchRemote(true)
        viewModelScope.launch {

            val subscribedUuidFlow = podcastManager
                .subscribedRxFlowable()
                .asFlow()
                .map { ls ->
                    ls.map { it.uuid }
                }

            combine(
                subscribedUuidFlow,
                searchHandler.searchResults,
            ) { subscribedUuids, searchState ->
                val podcasts = when (searchState) {
                    is SearchUiState.SearchOperation.Success -> {
                        // TODO handle loading
                        // TODO handle error

                        searchState.results.podcasts
                            .filterIsInstance<FolderItem.Podcast>()
                            .map {
                                PodcastResult(
                                    podcast = it.podcast,
                                    isSubscribed = subscribedUuids.contains(it.podcast.uuid),
                                )
                            }
                    }

                    else -> emptyList()
                }

                val isLoading = searchState is SearchUiState.SearchOperation.Loading

                state.value.copy(
                    results = podcasts,
                    loading = isLoading,
                )
            }.stateIn(viewModelScope).collect {
                _state.value = it
            }
        }
        analyticsTracker.track(
            AnalyticsEvent.SEARCH_SHOWN,
            mapOf(AnalyticsProp.SOURCE to SourceView.ONBOARDING_RECOMMENDATIONS.analyticsValue),
        )
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
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun toggleSubscribed(podcastResult: PodcastResult) {
        val uuid = podcastResult.podcast.uuid
        val event = if (podcastResult.isSubscribed) {
            podcastManager.unsubscribeAsync(podcastUuid = uuid, SourceView.ONBOARDING_RECOMMENDATIONS_SEARCH)
            PodcastUnsubscribedEvent(
                uuid = uuid,
                source = SourceView.ONBOARDING_RECOMMENDATIONS_SEARCH.eventHorizonValue,
            )
        } else {
            podcastManager.subscribeToPodcast(podcastUuid = uuid, sync = true)
            PodcastSubscribedEvent(
                uuid = uuid,
                source = SourceView.ONBOARDING_RECOMMENDATIONS_SEARCH.eventHorizonValue,
            )
        }
        eventHorizon.track(event)

        _state.update {
            it.copy(
                results = it.results.map { podcast ->
                    if (podcast.podcast.uuid == uuid) {
                        podcast.copy(isSubscribed = !podcastResult.isSubscribed)
                    } else {
                        podcast
                    }
                },
            )
        }
    }

    fun onBackPressed() {
        analyticsTracker.track(
            AnalyticsEvent.SEARCH_DISMISSED,
            mapOf(AnalyticsProp.SOURCE to SourceView.ONBOARDING_RECOMMENDATIONS.analyticsValue),
        )
    }

    companion object {
        private object AnalyticsProp {
            const val SOURCE = "source"
        }
    }
}
