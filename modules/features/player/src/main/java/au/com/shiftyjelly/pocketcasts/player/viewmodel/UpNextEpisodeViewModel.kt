package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Maybe
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class UpNextEpisodeViewModel
@Inject constructor(
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager
) : ViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val disposables = CompositeDisposable()
    private var episodeUuid: String? = null
    val episode = MutableLiveData<Playable>()
    val podcast = MutableLiveData<Podcast>()
    val isNextEpisode: LiveData<Boolean> =
        playbackManager.upNextQueue.changesObservable.map { upNext ->
            if (upNext is UpNextQueue.State.Loaded) {
                upNext.queue.indexOfFirst { it.uuid == episodeUuid } == 0
            } else {
                false
            }
        }.toFlowable(BackpressureStrategy.LATEST)
            .toLiveData()

    val isPlayingEpisode: LiveData<Boolean> =
        playbackManager.upNextQueue.changesObservable.map { upNext ->
            if (upNext is UpNextQueue.State.Loaded) {
                upNext.episode.uuid == episodeUuid
            } else {
                false
            }
        }.toFlowable(BackpressureStrategy.LATEST)
            .toLiveData()

    fun loadEpisode(episodeUuid: String) {
        this.episodeUuid = episodeUuid
        episodeManager.observePlayableByUuid(episodeUuid)
            .subscribeOn(Schedulers.io())
            .firstElement()
            .doOnSuccess { episode.postValue(it) }
            .flatMap {
                if (it is Episode) {
                    podcastManager.findPodcastByUuidRx(it.podcastUuid)
                } else {
                    Maybe.empty()
                }
            }
            .doOnSuccess { podcast.postValue(it) }
            .subscribeBy(onError = { Timber.e(it) })
            .addTo(disposables)
    }

    fun playNow() {
        episode.value?.let { playbackManager.playNow(episode = it) }
    }

    fun playNext() {
        episode.value?.let { launch { playbackManager.playNext(episode = it, source = AnalyticsSource.UP_NEXT) } }
    }

    fun removeFromUpNext() {
        episode.value?.let { playbackManager.removeEpisode(episodeToRemove = it, source = AnalyticsSource.UP_NEXT) }
    }

    fun markPlayed() {
        launch {
            episode.value?.let { episodeManager.markAsPlayed(episode = it, playbackManager = playbackManager, podcastManager = podcastManager) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }
}
