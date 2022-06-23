package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.combineLatest
import java.util.concurrent.TimeUnit

interface UpNextQueue {
    val isEmpty: Boolean
    val changesObservable: Observable<State>
    val currentEpisode: Playable?
    val queueEpisodes: List<Playable>
    val size: Int
        get() = queueEpisodes.size

    fun isCurrentEpisode(episode: Playable): Boolean
    suspend fun playNow(episode: Playable, onAdd: (() -> Unit)?)
    suspend fun playNext(episode: Playable, downloadManager: DownloadManager, onAdd: (() -> Unit)?)
    suspend fun playLast(episode: Playable, downloadManager: DownloadManager, onAdd: (() -> Unit)?)
    suspend fun playAllNext(episodes: List<Playable>, downloadManager: DownloadManager)
    suspend fun playAllLast(episodes: List<Playable>, downloadManager: DownloadManager)
    suspend fun removeEpisode(episode: Playable)
    suspend fun clearAndPlayAll(episodes: List<Playable>, downloadManager: DownloadManager)
    fun moveEpisode(from: Int, to: Int)
    fun changeList(episodes: List<Playable>)
    fun clearUpNext()
    fun removeAll()
    suspend fun removeAllEpisodes(episodes: List<Playable>)
    fun importServerChanges(episodes: List<Playable>, playbackManager: PlaybackManager, downloadManager: DownloadManager): Completable
    fun contains(uuid: String): Boolean

    sealed class State {
        object Empty : State()

        // Loaded state includes the current episode as episode and the episodes in up next in queue. Queue does not include the currently playing episode
        data class Loaded(val episode: Playable, val podcast: Podcast?, val queue: List<Playable>) : State()

        fun queueSize(): Int {
            return if (this is Loaded) queue.size else 0
        }
    }

    fun setup()

    /**
     * getChangesObservableWithLiveCurrentEpisode(episodeManager: EpisodeManager)
     * @param episodeManager: The EpisodeManager singleton
     * @return Returns an up next changes observable that keeps the current episode up to date
     * when certain metadata changes
     */
    fun getChangesObservableWithLiveCurrentEpisode(episodeManager: EpisodeManager, podcastManager: PodcastManager): Observable<State> {
        return changesObservable.debounce(100, TimeUnit.MILLISECONDS).switchMap { state ->
            if (state is State.Loaded) {
                if (state.podcast != null) {
                    // If we have a podcast we need to observe its effects state as well to ensure it updates when the global override changes
                    episodeManager.observePlayableByUuid(state.episode.uuid)
                        .combineLatest(podcastManager.observePodcastByUuid(state.podcast.uuid).distinctUntilChanged { t1, t2 -> t1.isUsingEffects == t2.isUsingEffects })
                        .map { State.Loaded(it.first, it.second, state.queue) }
                        .toObservable()
                } else {
                    episodeManager.observePlayableByUuid(state.episode.uuid)
                        .map { State.Loaded(it, state.podcast, state.queue) }
                        .toObservable()
                }
            } else {
                Observable.just(state)
            }
        }
    }
}

fun Observable<UpNextQueue.State>.containsUuid(uuid: String): Observable<Boolean> {
    return this.switchMap { state ->
        if (state is UpNextQueue.State.Loaded) {
            val inUpNext = state.queue.map { it.uuid }.contains(uuid) || state.episode.uuid == uuid
            Observable.just(inUpNext)
        } else {
            Observable.just(false)
        }
    }
}
