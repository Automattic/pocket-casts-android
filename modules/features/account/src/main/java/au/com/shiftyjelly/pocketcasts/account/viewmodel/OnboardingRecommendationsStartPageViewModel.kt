package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class OnboardingRecommendationsStartPageViewModel @Inject constructor(
    val podcastManager: PodcastManager,
    val playbackManager: PlaybackManager,
    val analyticsTracker: AnalyticsTracker,
    private val repository: ListRepository,
    private val settings: Settings,
    app: Application,
    categoriesManager: CategoriesManager,
) : AndroidViewModel(app) {

    data class State(
        val sections: List<Section>,
        val showLoadingSpinner: Boolean,
    ) {
        private val anySubscribed: Boolean = sections.any { it.anySubscribed }

        val buttonRes = if (anySubscribed || FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS)) {
            LR.string.navigation_continue
        } else {
            LR.string.not_now
        }

        companion object {
            val EMPTY = State(
                sections = emptyList(),
                showLoadingSpinner = true,
            )
        }
    }

    data class SectionId(val value: String)

    data class Podcast(
        val uuid: String,
        val title: String,
        val isSubscribed: Boolean,
    )

    // This class exists to hold sections before they are merged with the data
    // about the user's subscriptions to create a `RecommendationSection`.
    private data class SectionInternal(
        val title: String,
        val sectionId: SectionId,
        val podcasts: List<DiscoverPodcast>,
    )

    data class Section(
        val title: String,
        val sectionId: SectionId,
        val numToShow: Int,
        private val podcasts: List<Podcast>,
        private val onShowMoreFun: (Section) -> Unit,
    ) {
        val anySubscribed = podcasts.any { it.isSubscribed }
        val visiblePodcasts = podcasts.take(numToShow)
        fun onShowMore() = onShowMoreFun(this)
    }

    private val _state: MutableStateFlow<State> = MutableStateFlow(State.EMPTY)
    val state: StateFlow<State> = _state

    init {
        viewModelScope.launch {

            val sectionsFlow = MutableStateFlow<List<SectionInternal>>(emptyList())
            launch {
                val subscriptionsFlow = podcastManager
                    .subscribedRxFlowable()
                    .asFlow()
                    .map { subscribed ->
                        subscribed.map { it.uuid }
                    }
                combine(sectionsFlow, subscriptionsFlow) { sections, subscriptions ->
                    sections.map { section ->

                        val podcasts = section.podcasts.map { podcast ->
                            Podcast(
                                uuid = podcast.uuid,
                                title = podcast.title.orEmpty(),
                                isSubscribed = podcast.uuid in subscriptions,
                            )
                        }

                        _state.value.sections
                            .find { it.sectionId == section.sectionId }
                            ?.copy(podcasts = podcasts) // update previous section if it exists
                            ?: Section(
                                // otherwise create a new section
                                title = section.title,
                                sectionId = section.sectionId,
                                podcasts = podcasts,
                                numToShow = NUM_TO_SHOW_DEFAULT,
                                onShowMoreFun = ::onShowMore,
                            )
                    }
                }.collect { sections ->
                    _state.update { it.copy(sections = sections) }
                }
            }

            val feed = try {
                repository.getDiscoverFeed()
            } catch (e: Exception) {
                Timber.e("Exception retrieving Discover feed: $e")
                return@launch
            }

            val regionCode = settings.discoverCountryCode.value
            val region = feed.regions[regionCode]
                ?: feed.regions[feed.defaultRegionCode]
                    .let {
                        Timber.e("Could not get region for $regionCode")
                        return@launch
                    }

            // Update list with the correct region substituted in where appropriate
            val replacements = mapOf(
                feed.regionCodeToken to region.code,
                feed.regionNameToken to region.name,
            )
            val updatedList = feed.layout.transformWithRegion(region, replacements, getApplication<Application>().resources)

            val interestNames = categoriesManager.interestCategories.value.map { it.name }.toSet()
            if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS) && interestNames.isNotEmpty()) {
                updateFlowWithCategories(sectionsFlow, updatedList, replacements, interestNames)
                updateFlowWith("featured", sectionsFlow, updatedList, interestNames.size)
                updateFlowWith("trending", sectionsFlow, updatedList, interestNames.size + 1)
            } else {
                updateFlowWith("featured", sectionsFlow, updatedList)
                updateFlowWith("trending", sectionsFlow, updatedList)
                updateFlowWithCategories(sectionsFlow, updatedList, replacements)
            }

            _state.update { it.copy(showLoadingSpinner = false) }
        }
    }

    private fun onShowMore(section: Section) {
        analyticsTracker.track(
            AnalyticsEvent.RECOMMENDATIONS_MORE_TAPPED,
            mapOf(
                "section" to section.sectionId.value.lowercase(Locale.ENGLISH),
                "number_visible" to section.numToShow,
            ),
        )

        _state.update { oldState ->
            oldState.copy(
                sections = oldState.sections.map {
                    if (it.sectionId == section.sectionId) {
                        it.copy(numToShow = it.numToShow + NUM_TO_SHOW_INCREASE)
                    } else {
                        it
                    }
                },
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
                mapOf(AnalyticsProp.SUBSCRIPTIONS to podcastManager.countSubscribed()),
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
                mapOf(AnalyticsProp.SUBSCRIPTIONS to podcastManager.countSubscribed()),
            )
        }
    }

    fun updateSubscribed(podcast: Podcast) {
        val event: AnalyticsEvent
        if (podcast.isSubscribed) {
            event = AnalyticsEvent.PODCAST_UNSUBSCRIBED
            podcastManager.unsubscribeAsync(podcastUuid = podcast.uuid, playbackManager = playbackManager)
        } else {
            event = AnalyticsEvent.PODCAST_SUBSCRIBED
            podcastManager.subscribeToPodcast(podcastUuid = podcast.uuid, sync = true)
        }
        analyticsTracker.track(event, AnalyticsProp.podcastSubscribeToggled(podcast.uuid))

        // Immediately update subscribed state in the UI
        _state.update {
            it.copy(
                sections = it.sections.map { section ->
                    section.copy(
                        podcasts = section.visiblePodcasts.map { podcastInList ->
                            if (podcastInList.uuid == podcast.uuid) {
                                podcastInList.copy(isSubscribed = !podcastInList.isSubscribed)
                            } else {
                                podcastInList
                            }
                        },
                    )
                },
            )
        }
    }

    private suspend fun updateFlowWith(
        id: String,
        sectionsFlow: MutableStateFlow<List<SectionInternal>>,
        updatedList: List<DiscoverRow>,
        insertToPosition: Int? = null,
    ) {
        val listItem = updatedList.find { it.id == id }
        if (listItem == null) {
            Timber.e("Could not find section with id $id")
            return
        }

        val feed = try {
            repository.getListFeed(listItem.source)
        } catch (e: Exception) {
            Timber.e(e)
            return
        }

        val podcasts = feed?.podcasts
        if (podcasts.isNullOrEmpty()) return

        val title = listItem.title.tryToLocalise(getApplication<Application>().resources)
        val sectionToAdd = SectionInternal(
            title = title,
            sectionId = SectionId(id),
            podcasts = podcasts,
        )
        val updatedList = sectionsFlow.value.toMutableList().apply {
            insertToPosition?.let {
                add(min(it, this.size), sectionToAdd)
            } ?: add(sectionToAdd)
        }.toList()

        sectionsFlow.emit(
            updatedList,
        )
    }

    private suspend fun updateFlowWithCategories(
        sectionsFlow: MutableStateFlow<List<SectionInternal>>,
        updatedList: List<DiscoverRow>,
        replacements: Map<String, String>,
        interestCategoryNames: Set<String> = emptySet(),
    ) {
        val categories = updatedList
            .find { it.type is ListType.Categories }
            ?.let { row ->
                try {
                    repository
                        .getCategoriesList(row.source)
                        .map {
                            it.transformWithReplacements(
                                replacements,
                                getApplication<Application>().resources,
                            )
                        }
                } catch (e: Exception) {
                    Timber.e(e)
                    null
                }
            } ?: emptyList()

        // Make network calls one at a time so the UI can load the initial sections as quickly
        // as possible, and to maintain the order of the sections
        categories
            .filter { !FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS) || (it as? DiscoverCategory)?.popularity != null }
            .sortedWith(
                compareByDescending<NetworkLoadableList> { it.title in interestCategoryNames }.thenBy {
                    if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS)) {
                        val index = interestCategoryNames.indexOf(it.title)
                        if (index != -1) {
                            index.toString()
                        } else {
                            (it as? DiscoverCategory)?.popularity?.toString() ?: it.title
                        }
                    } else {
                        it.title
                    }
                },
            )
            .forEach { category ->
                val source = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_RECOMMENDATIONS)) {
                    (category as? DiscoverCategory)?.onboardingRecommendationsSource ?: category.source
                } else {
                    category.source
                }
                runCatching {
                    repository.getListFeed(source)
                }
                    .onFailure { exception ->
                        Timber.e(exception, "Error getting list feed for category $source")
                    }
                    .getOrNull()
                    ?.podcasts
                    ?.let { podcasts ->
                        sectionsFlow.emit(
                            sectionsFlow.value + SectionInternal(
                                title = category.title.tryToLocalise(getApplication<Application>().resources),
                                sectionId = SectionId(category.title),
                                podcasts = podcasts,
                            ),
                        )
                    }
            }
    }

    companion object {
        private const val ONBOARDING_RECOMMENDATIONS = "onboarding_recommendations"

        private object AnalyticsProp {
            const val SUBSCRIPTIONS = "subscriptions"
            const val UUID = "uuid"
            const val SOURCE = "source"
            fun podcastSubscribeToggled(uuid: String) = mapOf(UUID to uuid, SOURCE to ONBOARDING_RECOMMENDATIONS)
        }

        const val NUM_TO_SHOW_DEFAULT = 6
        private const val NUM_TO_SHOW_INCREASE = 6
    }
}
