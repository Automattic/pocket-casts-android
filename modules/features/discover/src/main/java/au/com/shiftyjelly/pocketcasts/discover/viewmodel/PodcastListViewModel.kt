package au.com.shiftyjelly.pocketcasts.discover.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.repositories.colors.ColorManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager.PlaybackSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PodcastListViewModel @Inject constructor(
    val listRepository: ListRepository,
    val colorManager: ColorManager,
    val podcastManager: PodcastManager,
    val userManager: UserManager,
    val episodeManager: EpisodeManager,
    val playbackManager: PlaybackManager
) : ViewModel() {
    val state: MutableLiveData<PodcastListViewState> = MutableLiveData()
    val disposables: CompositeDisposable = CompositeDisposable()

    init {
        state.value = PodcastListViewState.Loading()
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun load(sourceUrl: String?, listStyle: ExpandedStyle) {
        if (sourceUrl == null) {
            state.value = PodcastListViewState.Error(IllegalStateException("Must provide a source url"))
            return
        }

        listRepository.getListFeed(sourceUrl)
            .flatMap {
                return@flatMap if (listStyle is ExpandedStyle.RankedList) {
                    addColorsToFeed(it)
                } else {
                    Single.just(it)
                }
            }
            .toFlowable()
            .switchMap { feed ->
                addSubscriptionStateToFeed(feed)
            }
            .flatMap { feed ->
                addPlaybackStateToList(feed)
            }
            .subscribeBy(
                onNext = {
                    state.postValue(PodcastListViewState.ListLoaded(it))
                },
                onError = {
                    state.postValue(PodcastListViewState.Error(it))
                }
            )
            .addTo(disposables)
    }

    private fun addPlaybackStateToList(list: ListFeed): Flowable<ListFeed> {
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
                val updatedEpisodes = list.episodes?.map { episode -> episode.copy(isPlaying = playbackState.isPlaying && playbackState.episodeUuid == episode.uuid) }
                list.copy(episodes = updatedEpisodes)
            }
    }

    private fun addColorsToFeed(feed: ListFeed): Single<ListFeed> {
        val podcast = feed.podcasts?.firstOrNull()
        val podcastUuid = podcast?.uuid ?: return Single.just(feed)
        return colorManager.downloadColors(podcastUuid)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .flatMap {
                it.ifPresent {
                    podcast.color = it.background
                }

                return@flatMap Single.just(feed)
            }
    }

    private fun addSubscriptionStateToFeed(feed: ListFeed): Flowable<ListFeed> {
        return podcastManager.getSubscribedPodcastUuids().toFlowable() // Get the current subscribed list
            .mergeWith(podcastManager.observePodcastSubscriptions()) // Get updated when it changes
            .flatMap { subscribedList ->
                val newPodcastList = feed.podcasts?.map { podcast ->
                    podcast.updateIsSubscribed(subscribedList.contains(podcast.uuid))
                }

                feed.podcasts = newPodcastList

                val newPromotion = feed.promotion?.let {
                    it.copy(isSubscribed = subscribedList.contains(it.podcastUuid))
                }

                feed.promotion = newPromotion

                return@flatMap Flowable.just(feed)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
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
        playbackManager.playbackSource = PlaybackSource.DISCOVER_PODCAST_LIST
        playbackManager.playNow(episode, forceStream = true)
    }

    fun stopPlayback() {
        playbackManager.playbackSource = PlaybackSource.DISCOVER_PODCAST_LIST
        playbackManager.stopAsync()
    }
}

sealed class PodcastListViewState {
    class Loading : PodcastListViewState()
    data class ListLoaded(val feed: ListFeed) : PodcastListViewState()
    data class Error(val error: Throwable) : PodcastListViewState()
}
