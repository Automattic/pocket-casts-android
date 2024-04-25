package au.com.shiftyjelly.pocketcasts.discover.viewmodel

import android.content.res.Resources
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.view.CategoryPill
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.TrendingPodcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverFeedImage
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverFeedTintColors
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.servers.model.SponsoredPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.io.InvalidObjectException
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    val repository: ListRepository,
    val settings: Settings,
    val podcastManager: PodcastManager,
    val episodeManager: EpisodeManager,
    val playbackManager: PlaybackManager,
    val userManager: UserManager,
    val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {
    private val disposables = CompositeDisposable()
    private val sourceView = SourceView.DISCOVER
    val state = MutableLiveData<DiscoverState>().apply { value = DiscoverState.Loading }
    var currentRegionCode: String? = settings.discoverCountryCode.value
    private var replacements = emptyMap<String, String>()
    private var adsForCategoryView = emptyList<DiscoverRow>()
    private var isFragmentChangingConfigurations: Boolean = false

    fun onShown() {
        if (!isFragmentChangingConfigurations) {
            analyticsTracker.track(AnalyticsEvent.DISCOVER_SHOWN)
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun loadData(resources: Resources) {
        val feed =
            if (FeatureFlag.isEnabled(Feature.CATEGORIES_REDESIGN)) {
                repository.getDiscoverFeedWithCategoriesAtTheTop()
            } else {
                repository.getDiscoverFeed()
            }

        feed.toFlowable()
            .subscribeBy(
                onNext = {
                    val region = it.regions[currentRegionCode ?: it.defaultRegionCode] ?: it.regions[it.defaultRegionCode]
                    if (region == null) {
                        val message = "Could not get region $currentRegionCode"
                        Timber.e(message)
                        state.value = DiscoverState.Error(IllegalStateException(message))
                        return@subscribeBy
                    }

                    if (currentRegionCode == null) {
                        currentRegionCode = it.defaultRegionCode
                    }

                    replacements = mapOf(
                        it.regionCodeToken to region.code,
                        it.regionNameToken to region.name,
                    )

                    // Update the list with the correct region substituted in where needed
                    val updatedList = it.layout.transformWithRegion(region, replacements, resources)

                    // Save ads to display in category view
                    adsForCategoryView = updatedList.filter { discoverRow -> discoverRow.categoryId != null }

                    state.postValue(DiscoverState.DataLoaded(updatedList, region, it.regions.values.toList()))
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    state.postValue(DiscoverState.Error(throwable))
                },
            )
            .addTo(disposables)
    }

    fun changeRegion(region: DiscoverRegion, resources: Resources) {
        settings.discoverCountryCode.set(region.code, updateModifiedAt = false)
        currentRegionCode = region.code
        loadData(resources)
    }

    fun loadPodcastList(source: String, categoryId: String): Flowable<PodcastList> {
        return repository.getListFeed(source)
            .map {
                PodcastList(
                    podcasts = it.podcasts ?: emptyList(),
                    episodes = it.episodes ?: emptyList(),
                    title = it.title,
                    subtitle = it.subtitle,
                    description = it.description,
                    collectionImageUrl = it.collectionImageUrl,
                    tintColors = it.tintColors,
                    images = it.collageImages,
                    listId = it.listId,
                )
            }
            .doOnSuccess { podcastList ->
                if (categoryId == NetworkLoadableList.TRENDING) {
                    viewModelScope.launch {
                        val podcastIds = podcastList.podcasts.map { TrendingPodcast(it.uuid, it.title.orEmpty()) }
                        podcastManager.replaceTrendingPodcasts(podcastIds)
                    }
                }
            }
            .flatMapPublisher { addSubscriptionStateToPodcasts(it) }
            .flatMap {
                addPlaybackStateToList(it)
            }
    }
    fun filterPodcasts(source: String, categoryId: String, onPodcastsLoaded: (PodcastList) -> Unit) {
        state.postValue(DiscoverState.FilteringPodcastsByCategory)

        loadPodcastList(source, categoryId).subscribeBy(
            onNext = {
                state.postValue(DiscoverState.PodcastsFilteredByCategory)
                onPodcastsLoaded(it)
            },
            onError = {
                state.postValue(DiscoverState.Error(it))
                Timber.e(it)
            },
        ).addTo(disposables)
    }

    fun loadCarouselSponsoredPodcasts(
        sponsoredPodcastList: List<SponsoredPodcast>,
        categoryId: String,
    ): Flowable<List<CarouselSponsoredPodcast>> {
        val sponsoredPodcastsSources = sponsoredPodcastList
            .filter {
                val isInvalidSponsoredSource = it.source == null || it.position == null
                if (isInvalidSponsoredSource) {
                    val message = "Invalid sponsored source found."
                    Timber.e(message)
                    SentryHelper.recordException(InvalidObjectException(message))
                }
                !isInvalidSponsoredSource
            }
            .map { sponsoredPodcast ->
                loadPodcastList(sponsoredPodcast.source as String, categoryId)
                    .filter {
                        it.podcasts.isNotEmpty() && it.listId != null
                    }
                    .map {
                        CarouselSponsoredPodcast(
                            podcast = it.podcasts.first(),
                            position = sponsoredPodcast.position as Int,
                            listId = it.listId as String,
                        )
                    }
            }

        return if (sponsoredPodcastsSources.isNotEmpty()) {
            Flowable.zip(sponsoredPodcastsSources) {
                it.toList().filterIsInstance<CarouselSponsoredPodcast>()
            }
        } else {
            Flowable.just(emptyList())
        }
    }
    fun loadCategories(
        url: String,
        onSuccess: (List<CategoryPill>) -> Unit,
    ) {
        val categoriesList = repository.getCategoriesList(url)

        categoriesList.subscribeBy(
            onSuccess = {
                val categoryPills = it.map { discoverCategory ->
                    convertCategoryToPill(discoverCategory)
                }
                onSuccess(categoryPills)
            },
            onError = {
                Timber.e(it)
            },
        ).addTo(disposables)
    }
    fun loadCategories(
        url: String,
    ): Flowable<List<CategoryPill>> {
        val categoriesList: Flowable<List<DiscoverCategory>> = repository.getCategoriesList(url)
            .toFlowable()

        return categoriesList.map { discoverCategories ->
            discoverCategories.map { discoverCategory ->
                convertCategoryToPill(discoverCategory)
            }
        }
    }
    private fun convertCategoryToPill(category: DiscoverCategory): CategoryPill = CategoryPill(
        discoverCategory = DiscoverCategory(
            id = category.id,
            name = category.name,
            icon = category.icon,
            curated = category.curated,
            source = category.source,
        ),
    )

    private fun addSubscriptionStateToPodcasts(list: PodcastList): Flowable<PodcastList> {
        return podcastManager.getSubscribedPodcastUuids().toFlowable() // Get the current subscribed list
            .mergeWith(podcastManager.observePodcastSubscriptions()) // Get updated when it changes
            .map { subscribedList ->
                val updatedPodcasts = list.podcasts.map { podcast ->
                    val isSubscribed = subscribedList.contains(podcast.uuid)
                    if (podcast.isSubscribed != isSubscribed) { // Check if there's a change in isSubscribed state
                        podcast.copy(isSubscribed = isSubscribed)
                    } else {
                        podcast // If there's no change, keep the podcast unchanged
                    }
                }
                list.copy(podcasts = updatedPodcasts)
            }
            .distinctUntilChanged() // Emit only if there's a change in the list of podcasts
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }
    private fun addPlaybackStateToList(list: PodcastList): Flowable<PodcastList> {
        return Flowable.just(list)
            .combineLatest(
                // monitor the playing episode
                playbackManager
                    .playbackStateRelay
                    .toFlowable(BackpressureStrategy.LATEST)
                    // ignore the episode progress
                    .distinctUntilChanged { t1, t2 -> t1.episodeUuid == t2.episodeUuid && t1.isPlaying == t2.isPlaying },
            )
            .map { (list, playbackState) ->
                val updatedEpisodes = list.episodes.map { episode -> episode.copy(isPlaying = playbackState.isPlaying && playbackState.episodeUuid == episode.uuid) }
                list.copy(episodes = updatedEpisodes)
            }
    }

    fun subscribeToPodcast(podcast: DiscoverPodcast) {
        if (podcastManager.isSubscribingToPodcast(podcast.uuid)) { // Watch out for double taps on the button
            return
        }

        if (!podcast.isSubscribed) {
            podcastManager.subscribeToPodcast(podcast.uuid, sync = true)
        }
    }

    fun transformNetworkLoadableList(list: NetworkLoadableList, resources: Resources): NetworkLoadableList {
        return list.transformWithReplacements(replacements, resources)
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun findOrDownloadEpisode(discoverEpisode: DiscoverEpisode, success: (episode: PodcastEpisode) -> Unit) {
        podcastManager.findOrDownloadPodcastRx(discoverEpisode.podcast_uuid)
            .flatMapMaybe {
                @Suppress("DEPRECATION")
                episodeManager.findByUuidRx(discoverEpisode.uuid)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { episode ->
                    if (episode != null) {
                        success(episode)
                    }
                },
                onError = { throwable ->
                    Timber.e(throwable)
                },
            )
            .addTo(disposables)
    }

    fun playEpisode(episode: PodcastEpisode) {
        playbackManager.playNow(episode = episode, forceStream = true, sourceView = sourceView)
    }

    fun stopPlayback() {
        playbackManager.stopAsync(sourceView = sourceView)
    }
    fun getAdForCategoryView(categoryInt: Int): DiscoverRow? {
        return adsForCategoryView.firstOrNull { it.categoryId == categoryInt }
    }
}

sealed class DiscoverState {
    data object Loading : DiscoverState()
    data class DataLoaded(val data: List<DiscoverRow>, val selectedRegion: DiscoverRegion, val regionList: List<DiscoverRegion>) : DiscoverState()
    data object FilteringPodcastsByCategory : DiscoverState()
    data object PodcastsFilteredByCategory : DiscoverState()
    data class Error(val error: Throwable) : DiscoverState()
}

data class PodcastList(
    val podcasts: List<DiscoverPodcast>,
    val episodes: List<DiscoverEpisode>,
    val title: String?,
    val subtitle: String?,
    val description: String?,
    val collectionImageUrl: String?,
    val tintColors: DiscoverFeedTintColors?,
    val images: List<DiscoverFeedImage>?,
    val listId: String? = null,
)

data class CarouselSponsoredPodcast(
    val podcast: DiscoverPodcast,
    val position: Int,
    val listId: String,
)
