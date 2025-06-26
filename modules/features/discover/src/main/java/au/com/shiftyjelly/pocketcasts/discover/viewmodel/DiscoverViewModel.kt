package au.com.shiftyjelly.pocketcasts.discover.viewmodel

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.view.CategoryAdRow
import au.com.shiftyjelly.pocketcasts.discover.view.ChangeRegionRow
import au.com.shiftyjelly.pocketcasts.discover.view.MostPopularPodcastsByCategoryRow
import au.com.shiftyjelly.pocketcasts.discover.view.RemainingPodcastsByCategoryRow
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverFeedImage
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverFeedTintColors
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.servers.model.SponsoredPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.io.InvalidObjectException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.rxMaybe
import kotlinx.coroutines.rx2.rxSingle
import timber.log.Timber

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    val repository: ListRepository,
    val settings: Settings,
    val podcastManager: PodcastManager,
    val episodeManager: EpisodeManager,
    val playbackManager: PlaybackManager,
    val userManager: UserManager,
    val categoriesManager: CategoriesManager,
    val analyticsTracker: AnalyticsTracker,
    val crashLogging: CrashLogging,
    val syncManager: SyncManager,
) : ViewModel() {
    private val disposables = CompositeDisposable()
    private val sourceView = SourceView.DISCOVER
    var currentRegionCode: String? = settings.discoverCountryCode.value
    private var replacements = emptyMap<String, String>()
    private var adsForCategoryView = emptyList<DiscoverRow>()
    private var isFragmentChangingConfigurations: Boolean = false

    private val _state = MutableStateFlow(
        DiscoverState(
            discoverFeed = null,
            categoryFeed = null,
            isLoading = true,
            isError = false,
        ),
    )
    internal val state = _state.asStateFlow()

    fun onShown() {
        if (!isFragmentChangingConfigurations) {
            analyticsTracker.track(AnalyticsEvent.DISCOVER_SHOWN)
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun loadFeed(resources: Resources) {
        val loggedInObservable = syncManager.isLoggedInObservable
        Observables.combineLatest(loadDiscoverFeedRxSingle(resources).toObservable(), loggedInObservable)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { (discoverFeed: DiscoverFeed, isLoggedIn: Boolean) ->
                // remove authenticated lists if the user's not logged in
                if (!isLoggedIn) {
                    discoverFeed.copy(
                        data = discoverFeed.data.filterNot { it.authenticated ?: false },
                    )
                } else {
                    discoverFeed
                }
            }
            .subscribeBy(
                onNext = { discoverFeed ->
                    _state.update {
                        it.copy(discoverFeed = discoverFeed, categoryFeed = null, isLoading = false, isError = false)
                    }
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    _state.update { it.copy(isLoading = false, isError = true) }
                },
            )
            .addTo(disposables)
    }

    private fun loadDiscoverFeedRxSingle(resources: Resources): Single<DiscoverFeed> = rxSingle {
        val discover = repository.getDiscoverFeed()
        val defaultRegion = discover.defaultRegionCode
        val region = discover.regions[currentRegionCode ?: defaultRegion] ?: discover.regions[defaultRegion]
        if (region == null) {
            error("Could not get region $currentRegionCode")
        }

        if (currentRegionCode == null) {
            currentRegionCode = defaultRegion
        }
        replacements = mapOf(
            discover.regionCodeToken to region.code,
            discover.regionNameToken to region.name,
        )

        // Update the list with the correct region substituted in where needed
        val updatedList = discover.layout.transformWithRegion(region, replacements, resources)

        // Save ads to display in category view
        adsForCategoryView = updatedList.filter { discoverRow -> discoverRow.categoryId != null }
        DiscoverFeed(updatedList, region, discover.regions.values.toList())
    }

    fun changeRegion(region: DiscoverRegion, resources: Resources) {
        settings.discoverCountryCode.set(region.code, updateModifiedAt = false)
        currentRegionCode = region.code
        loadFeed(resources)
    }

    fun loadPodcastList(source: String, authenticated: Boolean?): Flowable<PodcastList> {
        return rxMaybe { repository.getListFeed(source, authenticated) }
            .toSingle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                PodcastList(
                    podcasts = it.podcasts ?: emptyList(),
                    episodes = it.episodes ?: emptyList(),
                    title = it.title,
                    subtitle = it.subtitle,
                    description = it.description,
                    shortDescription = it.shortDescription,
                    collectionImageUrl = it.collectionImageUrl,
                    collectionRectangleImageUrl = it.collectionRectangleImageUrl,
                    featureImage = it.featureImage,
                    tintColors = it.tintColors,
                    images = it.collageImages,
                    listId = it.listId,
                    date = it.date,
                )
            }
            .flatMapPublisher { addSubscriptionStateToPodcasts(it) }
            .flatMap {
                addPlaybackStateToList(it)
            }
    }

    fun loadCategory(category: DiscoverCategory, resources: Resources) {
        _state.update { it.copy(isLoading = true) }

        val initialDiscoverFeed = state.value.discoverFeed
        val feedInitialization = if (initialDiscoverFeed == null) {
            loadDiscoverFeedRxSingle(resources)
        } else {
            Single.just(initialDiscoverFeed)
        }

        feedInitialization
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap { discoverFeed ->
                val categoryUrl = transformNetworkLoadableList(category, resources).source
                loadPodcastList(source = categoryUrl, authenticated = false)
                    .firstOrError()
                    .map { discoverFeed to it }
            }
            .subscribeBy(
                onSuccess = { (discoverFeed, podcasts) ->
                    val categoryFeed = CategoryFeed(
                        category = category,
                        podcastList = podcasts,
                        adRow = getCategoryAdRow(category),
                    )
                    _state.update { it.copy(isLoading = false, discoverFeed = discoverFeed, categoryFeed = categoryFeed) }
                },
                onError = {
                    _state.update { it.copy(isLoading = false, isError = true) }
                    categoriesManager.dismissSelectedCategory()
                    Timber.e(it)
                },
            )
            .addTo(disposables)
    }

    fun loadCarouselSponsoredPodcasts(
        sponsoredPodcastList: List<SponsoredPodcast>,
    ): Flowable<List<CarouselSponsoredPodcast>> {
        val sponsoredPodcastsSources = sponsoredPodcastList
            .filter {
                val isInvalidSponsoredSource = it.source == null || it.position == null
                if (isInvalidSponsoredSource) {
                    val message = "Invalid sponsored source found."
                    Timber.e(message)
                    crashLogging.sendReport(InvalidObjectException(message))
                }
                !isInvalidSponsoredSource
            }
            .map { sponsoredPodcast ->
                loadPodcastList(source = sponsoredPodcast.source as String, authenticated = false)
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

    private fun addSubscriptionStateToPodcasts(list: PodcastList): Flowable<PodcastList> {
        return podcastManager.getSubscribedPodcastUuidsRxSingle().toFlowable() // Get the current subscribed list
            .mergeWith(podcastManager.podcastSubscriptionsRxFlowable()) // Get updated when it changes
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
        podcastManager.findOrDownloadPodcastRxSingle(discoverEpisode.podcast_uuid)
            .flatMapMaybe {
                @Suppress("DEPRECATION")
                episodeManager.findByUuidRxMaybe(discoverEpisode.uuid)
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

    internal fun getCategoryAdRow(category: DiscoverCategory): CategoryAdRow? {
        return adsForCategoryView.firstOrNull { it.categoryId == category.id }?.let { row ->
            CategoryAdRow(
                categoryId = category.id,
                categoryName = category.name,
                region = currentRegionCode,
                discoverRow = row,
            )
        }
    }
}

internal data class DiscoverState(
    val discoverFeed: DiscoverFeed?,
    val categoryFeed: CategoryFeed?,
    val isLoading: Boolean,
    val isError: Boolean,
) {
    val rows: List<Any>? get() = discoverFeed?.let { discover ->
        if (categoryFeed == null) {
            discover.data.filter { it.categoryId == null } + ChangeRegionRow(discover.selectedRegion)
        } else {
            buildList {
                val categoriesRow = discover.data.find { it.type is ListType.Categories }
                if (categoriesRow != null) {
                    add(categoriesRow)
                }

                add(categoryFeed.mostPopularPodcasts)
                if (categoryFeed.adRow != null) {
                    add(categoryFeed.adRow)
                }
                add(categoryFeed.remainingPodcasts)
            }
        }
    }
}

internal data class DiscoverFeed(
    val data: List<DiscoverRow>,
    val selectedRegion: DiscoverRegion,
    val regionList: List<DiscoverRegion>,
)

internal data class CategoryFeed(
    val category: DiscoverCategory,
    val podcastList: PodcastList,
    val adRow: CategoryAdRow?,
) {
    val mostPopularPodcasts = MostPopularPodcastsByCategoryRow(
        listId = podcastList.listId,
        category = podcastList.title,
        podcasts = podcastList.podcasts.take(5),
    )
    val remainingPodcasts = RemainingPodcastsByCategoryRow(
        listId = podcastList.listId,
        category = podcastList.title,
        podcasts = podcastList.podcasts.drop(5),
    )
}

data class PodcastList(
    val podcasts: List<DiscoverPodcast>,
    val episodes: List<DiscoverEpisode>,
    val title: String?,
    val subtitle: String?,
    val description: String?,
    val shortDescription: String?,
    val collectionImageUrl: String?,
    val collectionRectangleImageUrl: String?,
    val featureImage: String?,
    val tintColors: DiscoverFeedTintColors?,
    val images: List<DiscoverFeedImage>?,
    val listId: String? = null,
    val date: String? = null,
)

data class CarouselSponsoredPodcast(
    val podcast: DiscoverPodcast,
    val position: Int,
    val listId: String,
)
