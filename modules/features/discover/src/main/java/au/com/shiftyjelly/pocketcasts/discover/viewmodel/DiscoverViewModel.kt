package au.com.shiftyjelly.pocketcasts.discover.viewmodel

import android.content.res.Resources
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverFeedImage
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverFeedTintColors
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

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
    private val playbackSource = AnalyticsSource.DISCOVER
    val state = MutableLiveData<DiscoverState>().apply { value = DiscoverState.Loading }
    var currentRegionCode: String? = settings.getDiscoveryCountryCode()
    var replacements = emptyMap<String, String>()
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
        val feed = repository.getDiscoverFeed()

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
                        it.regionNameToken to region.name
                    )

                    // Update the list with the correct region substituted in where needed
                    val updatedList = it.layout.transformWithRegion(region, replacements, resources)

                    state.postValue(DiscoverState.DataLoaded(updatedList, region, it.regions.values.toList()))
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    state.postValue(DiscoverState.Error(throwable))
                }
            )
            .addTo(disposables)
    }

    fun changeRegion(region: DiscoverRegion, resources: Resources) {
        settings.setDiscoveryCountryCode(region.code)
        currentRegionCode = region.code
        loadData(resources)
    }

    fun loadPodcastList(source: String): Flowable<PodcastList> {
        return repository.getListFeed(source)
            .map { PodcastList(it.podcasts ?: emptyList(), it.episodes ?: emptyList(), it.title, it.subtitle, it.description, it.collectionImageUrl, it.tintColors, it.collageImages) }
            .flatMapPublisher { addSubscriptionStateToPodcasts(it) }
            .flatMap {
                addPlaybackStateToList(it)
            }
    }

    private fun addSubscriptionStateToPodcasts(list: PodcastList): Flowable<PodcastList> {
        return podcastManager.getSubscribedPodcastUuids().toFlowable() // Get the current subscribed list
            .mergeWith(podcastManager.observePodcastSubscriptions()) // Get updated when it changes
            .flatMap { subscribedList ->
                val newPodcastList = list.podcasts.map { podcast -> // Update the podcast list with each podcasts subscription status
                    podcast.updateIsSubscribed(subscribedList.contains(podcast.uuid))
                }

                return@flatMap Flowable.just(list.copy(podcasts = newPodcastList))
            }
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
                    .distinctUntilChanged { t1, t2 -> t1.episodeUuid == t2.episodeUuid && t1.isPlaying == t2.isPlaying }
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

    fun findOrDownloadEpisode(discoverEpisode: DiscoverEpisode, success: (episode: Episode) -> Unit) {
        podcastManager.findOrDownloadPodcastRx(discoverEpisode.podcast_uuid)
            .flatMapMaybe { episodeManager.findByUuidRx(discoverEpisode.uuid) }
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
                }
            )
            .addTo(disposables)
    }

    fun playEpisode(episode: Episode) {
        playbackManager.playNow(episode = episode, forceStream = true, playbackSource = playbackSource)
    }

    fun stopPlayback() {
        playbackManager.stopAsync(playbackSource = playbackSource)
    }
}

sealed class DiscoverState {
    object Loading : DiscoverState()
    data class DataLoaded(val data: List<DiscoverRow>, val selectedRegion: DiscoverRegion, val regionList: List<DiscoverRegion>) : DiscoverState()
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
    val images: List<DiscoverFeedImage>?
)
