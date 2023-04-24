package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
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
    val currentEpisode: BaseEpisode?
    val queueEpisodes: List<BaseEpisode>
    val size: Int
        get() = queueEpisodes.size

    val allEpisodes get(): List<BaseEpisode> = currentEpisode?.let { listOf(it) + queueEpisodes } ?: queueEpisodes
    fun isCurrentEpisode(episode: BaseEpisode): Boolean
    suspend fun playNow(episode: BaseEpisode, onAdd: (() -> Unit)?)
    suspend fun playNext(episode: BaseEpisode, downloadManager: DownloadManager, onAdd: (() -> Unit)?)
    suspend fun playLast(episode: BaseEpisode, downloadManager: DownloadManager, onAdd: (() -> Unit)?)
    suspend fun playAllNext(episodes: List<BaseEpisode>, downloadManager: DownloadManager)
    suspend fun playAllLast(episodes: List<BaseEpisode>, downloadManager: DownloadManager)
    suspend fun removeEpisode(episode: BaseEpisode)
    suspend fun clearAndPlayAll(episodes: List<BaseEpisode>, downloadManager: DownloadManager)
    fun moveEpisode(from: Int, to: Int)
    fun changeList(episodes: List<BaseEpisode>)
    fun clearUpNext()
    fun removeAll()
    suspend fun removeAllIncludingChanges()
    fun importServerChanges(episodes: List<BaseEpisode>, playbackManager: PlaybackManager, downloadManager: DownloadManager): Completable
    fun contains(uuid: String): Boolean

    sealed class State {
        object Empty : State()

        // Loaded state includes the current episode as episode and the episodes in up next in queue. Queue does not include the currently playing episode
        data class Loaded(val episode: BaseEpisode, val podcast: Podcast?, val queue: List<BaseEpisode>) : State()

        fun queueSize(): Int {
            return if (this is Loaded) queue.size else 0
        }

        companion object {
            fun isEqualWithEpisodeCompare(stateOne: State, stateTwo: State, isPlayingEpisodeEqual: (BaseEpisode, BaseEpisode) -> Boolean): Boolean {
                return when {
                    stateOne is Empty && stateTwo is Empty -> true
                    stateOne is Loaded && stateTwo is Loaded -> {
                        stateOne.queue.map { it.uuid } == stateTwo.queue.map { it.uuid } &&
                            isPlayingEpisodeEqual(stateOne.episode, stateTwo.episode)
                    }
                    else -> false
                }
            }
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
                    episodeManager.observeEpisodeByUuid(state.episode.uuid)
                        .combineLatest(podcastManager.observePodcastByUuid(state.podcast.uuid).distinctUntilChanged { t1, t2 -> t1.isUsingEffects == t2.isUsingEffects })
                        .map { State.Loaded(it.first, it.second, state.queue) }
                        .toObservable()
                } else {
                    episodeManager.observeEpisodeByUuid(state.episode.uuid)
                        .map { State.Loaded(it, state.podcast, state.queue) }
                        .toObservable()
                }
            } else {
                Observable.just(state)
            }
        }
    }
}

enum class UpNextSource(val analyticsValue: String) {
    MINI_PLAYER("mini_player"),
    PLAYER("player"),
    NOW_PLAYING("now_playing"),
    UP_NEXT_SHORTCUT("up_next_shortcut"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(string: String) = UpNextSource.values().find { it.analyticsValue == string } ?: UNKNOWN
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
