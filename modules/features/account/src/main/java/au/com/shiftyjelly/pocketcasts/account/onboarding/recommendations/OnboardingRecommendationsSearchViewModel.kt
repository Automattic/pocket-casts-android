package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.search.SearchHandler
import au.com.shiftyjelly.pocketcasts.search.SearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingRecommendationsSearchViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val searchHandler: SearchHandler,
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

    fun updateSearchQuery(searchQuery: String) {
        _state.value = state.value.copy(searchQuery = searchQuery)
        searchHandler.updateSearchQuery(searchQuery)
    }

    init {
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

    fun toggleSubscribed(podcastResult: PodcastResult) {
        val uuid = podcastResult.podcast.uuid
        if (podcastResult.isSubscribed) {
            podcastManager.unsubscribeAsync(podcastUuid = uuid, playbackManager = playbackManager)
        } else {
            podcastManager.subscribeToPodcast(podcastUuid = uuid, sync = true)
        }
    }
}
