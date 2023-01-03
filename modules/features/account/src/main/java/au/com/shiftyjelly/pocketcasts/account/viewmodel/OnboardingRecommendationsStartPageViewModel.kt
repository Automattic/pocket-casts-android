package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
    private val repository: ListRepository,
    private val settings: Settings,
    app: Application,
) : AndroidViewModel(app) {

    data class State(
        val sections: List<RecommendationSection>,
        val showLoadingSpinner: Boolean,
    ) {
        private val anySubscribed: Boolean = sections.any { it.anySubscribed }

        val buttonRes = if (anySubscribed) {
            LR.string.navigation_continue
        } else {
            LR.string.not_now
        }

        companion object {
            val EMPTY = State(
                sections = emptyList(),
                showLoadingSpinner = true
            )
        }
    }

    data class RecommendationPodcast(
        val uuid: String,
        val title: String,
        val isSubscribed: Boolean,
    )

    private data class SectionInternal(
        val title: String,
        val podcasts: List<DiscoverPodcast>
    )

    data class RecommendationSection(
        val title: String,
        val numToShow: Int = NUM_TO_SHOW_DEFAULT,
        private val podcasts: List<RecommendationPodcast>,
    ) {
        val anySubscribed = podcasts.any { it.isSubscribed }
        val visiblePodcasts = podcasts.take(numToShow)
    }

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.EMPTY)
    val state: StateFlow<State> = _state

    init {
        viewModelScope.launch {

            val sectionsFlow = MutableStateFlow<List<SectionInternal>>(emptyList())
            launch {
                val subscriptionsFlow = podcastManager
                    .observeSubscribed()
                    .asFlow()
                    .map { subscribed ->
                        subscribed.map { it.uuid }
                    }
                combine(sectionsFlow, subscriptionsFlow) { sections, subscriptions ->
                    sections.map { section ->
                        val podcasts = section.podcasts.map { podcast ->
                            RecommendationPodcast(
                                uuid = podcast.uuid,
                                title = podcast.title ?: "",
                                isSubscribed = podcast.uuid in subscriptions,
                            )
                        }
                        RecommendationSection(
                            title = section.title,
                            podcasts = podcasts,
                        )
                    }
                }.collect { sections ->
                    _state.update { it.copy(sections = sections) }
                }
            }

            val feed = try {
                repository.getDiscoverFeed().await()
            } catch (e: Exception) {
                Timber.e("Exception retrieving Discover feed: $e")
                return@launch
            }

            val regionCode = settings.getDiscoveryCountryCode()
            val region = feed.regions[regionCode]
                ?: feed.regions[feed.defaultRegionCode]
                    .let {
                        Timber.e("Could not get region for $regionCode")
                        return@launch
                    }

            // Update list with the correct region substituted in where appropriate
            val replacements = mapOf(
                feed.regionCodeToken to region.code,
                feed.regionNameToken to region.name
            )
            val updatedList = feed.layout.transformWithRegion(region, replacements, getApplication<Application>().resources)

            updateFlowWithTrending(sectionsFlow, updatedList)
            updateFlowWithCategories(sectionsFlow, updatedList, replacements)

            _state.update { it.copy(showLoadingSpinner = false) }
        }
    }

    fun showMore(sectionTitle: String) {
        _state.update { oldState ->
            oldState.copy(
                sections = oldState.sections.map {
                    if (it.title == sectionTitle) {
                        it.copy(numToShow = it.numToShow + NUM_TO_SHOW_INCREASE)
                    } else {
                        it
                    }
                }
            )
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

    private suspend fun updateFlowWithTrending(
        sectionsFlow: MutableStateFlow<List<SectionInternal>>,
        updatedList: List<DiscoverRow>
    ) {
        val trendingPodcasts = updatedList
            .find { it.id == "trending" }
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

        if (trendingPodcasts != null) {
            sectionsFlow.emit(
                sectionsFlow.value + SectionInternal(
                    title = getApplication<Application>().resources.getString(LR.string.discover_trending),
                    podcasts = trendingPodcasts
                )
            )
        }
    }

    private suspend fun updateFlowWithCategories(
        sectionsFlow: MutableStateFlow<List<SectionInternal>>,
        updatedList: List<DiscoverRow>,
        replacements: Map<String, String>
    ) {
        val categories = updatedList
            .find { it.type is ListType.Categories }
            ?.let { row ->
                try {
                    repository
                        .getCategoriesList(row.source)
                        .await()
                        .map {
                            it.transformWithReplacements(
                                replacements,
                                getApplication<Application>().resources
                            )
                        }
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            } ?: emptyList()

        // Make network calls one at a time so the UI doesn't wait for all the
        // calls to complete before updating and to maintain order
        categories.forEach { category ->
            repository
                .getListFeed(category.source).await()
                .podcasts?.let { podcasts ->
                    sectionsFlow.emit(
                        sectionsFlow.value + SectionInternal(
                            title = category.title,
                            podcasts = podcasts
                        )
                    )
                }
        }
    }

    companion object {
        private const val SUBSCRIPTIONS_PROP = "subscriptions"
        const val NUM_TO_SHOW_DEFAULT = 6
        private const val NUM_TO_SHOW_INCREASE = 6
    }
}
