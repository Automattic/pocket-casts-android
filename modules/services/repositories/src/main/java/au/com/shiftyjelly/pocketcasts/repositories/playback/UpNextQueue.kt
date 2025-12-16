package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.UpNextSortType
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import io.reactivex.Observable
import io.reactivex.rxkotlin.combineLatest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlow

interface UpNextQueue {
    val isEmpty: Boolean
    val changesObservable: Observable<State>
    val currentEpisode: BaseEpisode?
    val queueEpisodes: List<BaseEpisode>
    val size: Int
        get() = queueEpisodes.size

    val allEpisodes get(): List<BaseEpisode> = currentEpisode?.let { listOf(it) + queueEpisodes } ?: queueEpisodes
    fun isCurrentEpisode(episode: BaseEpisode): Boolean
    suspend fun playNow(episode: BaseEpisode, automaticUpNextSource: AutoPlaySource?, changeSource: UpNextChangeSource, onAdd: (() -> Unit)?)
    suspend fun playNextBlocking(episode: BaseEpisode, downloadManager: DownloadManager, changeSource: UpNextChangeSource, onAdd: (() -> Unit)?)
    suspend fun playLast(episode: BaseEpisode, downloadManager: DownloadManager, changeSource: UpNextChangeSource, onAdd: (() -> Unit)?)
    suspend fun playAllNext(episodes: List<BaseEpisode>, downloadManager: DownloadManager, changeSource: UpNextChangeSource)
    suspend fun playAllLast(episodes: List<BaseEpisode>, downloadManager: DownloadManager, changeSource: UpNextChangeSource)
    suspend fun removeEpisode(episode: BaseEpisode, shouldShuffleUpNext: Boolean = false, changeSource: UpNextChangeSource)
    suspend fun clearAndPlayAll(episodes: List<BaseEpisode>, downloadManager: DownloadManager, changeSource: UpNextChangeSource)
    fun moveEpisode(from: Int, to: Int, changeSource: UpNextChangeSource)
    fun changeList(episodes: List<BaseEpisode>, changeSource: UpNextChangeSource)
    fun clearUpNext(changeSource: UpNextChangeSource)
    fun removeAll(changeSource: UpNextChangeSource)
    suspend fun removeAllIncludingChanges(changeSource: UpNextChangeSource)
    suspend fun importServerChangesBlocking(episodes: List<BaseEpisode>, playbackManager: PlaybackManager, downloadManager: DownloadManager)
    fun contains(uuid: String): Boolean
    fun updateCurrentEpisodeState(state: State)
    fun sortUpNext(sortType: UpNextSortType, changeSource: UpNextChangeSource)

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

    fun setupBlocking()

    /**
     * getChangesObservableWithLiveCurrentEpisode(episodeManager: EpisodeManager)
     * @param episodeManager: The EpisodeManager singleton
     * @return Returns an up next changes observable that keeps the current episode up to date
     * when certain metadata changes
     */
    fun getChangesObservableWithLiveCurrentEpisode(episodeManager: EpisodeManager, podcastManager: PodcastManager): Observable<State> {
        // the debounce prevents too many events being generated and just returns the latest
        return changesObservable.debounce(100, TimeUnit.MILLISECONDS).switchMap { state ->
            if (state is State.Loaded) {
                if (state.podcast != null) {
                    // If we have a podcast we need to observe its effects state as well to ensure it updates when the global override changes
                    episodeManager.findEpisodeByUuidRxFlowable(state.episode.uuid)
                        .combineLatest(podcastManager.podcastByUuidRxFlowable(state.podcast.uuid).distinctUntilChanged { t1, t2 -> t1.isUsingEffects == t2.isUsingEffects })
                        .map<State> { State.Loaded(it.first, it.second, state.queue) }
                        .onErrorReturn { State.Empty }
                        .toObservable()
                } else {
                    episodeManager.findEpisodeByUuidRxFlowable(state.episode.uuid)
                        .map<State> { State.Loaded(it, state.podcast, state.queue) }
                        .onErrorReturn { State.Empty }
                        .toObservable()
                }
            } else {
                Observable.just(state)
            }
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    fun getChangesFlowWithLiveCurrentEpisode(episodeManager: EpisodeManager, podcastManager: PodcastManager): Flow<State> {
        return changesObservable.asFlow().debounce(100).flatMapLatest { state ->
            if (state is State.Loaded) {
                if (state.podcast != null) {
                    episodeManager.findEpisodeByUuidFlow(state.episode.uuid)
                        .combine<BaseEpisode, Podcast, State>(
                            podcastManager
                                .podcastByUuidFlow(state.podcast.uuid)
                                .distinctUntilChanged { t1, t2 -> t1.isUsingEffects == t2.isUsingEffects },
                        ) { episode, podcast ->
                            val loadedState = State.Loaded(episode, podcast, state.queue)
                            updateCurrentEpisodeStateIfNeeded(episode, loadedState)
                            loadedState
                        }
                        .catch { emit(State.Empty) }
                } else {
                    episodeManager.findEpisodeByUuidFlow(state.episode.uuid)
                        .map<BaseEpisode, State> {
                            val loadedState = State.Loaded(it, state.podcast, state.queue)
                            updateCurrentEpisodeStateIfNeeded(it, loadedState)
                            loadedState
                        }
                        .catch { emit(State.Empty) }
                }
            } else {
                flowOf(state)
            }
        }
    }

    fun updateCurrentEpisodeStateIfNeeded(episodeFromDb: BaseEpisode, state: State) {
        currentEpisode?.let { currentEpisode ->
            if (episodeFromDb.uuid == currentEpisode.uuid &&
                episodeFromDb.deselectedChapters.sorted() != currentEpisode.deselectedChapters.sorted()
            ) {
                updateCurrentEpisodeState(state)
            }
        }
    }
}

enum class UpNextPageSource(val analyticsValue: String) {
    MiniPlayer("mini_player"),
    Player("player"),
    NowPlaying("now_playing"),
    UpNextShortcut("up_next_shortcut"),
    UpNextTab("up_next_tab"),
    Unknown("unknown"),
    ;

    companion object {
        fun fromString(string: String) = entries.find { it.analyticsValue == string } ?: Unknown
    }
}

/**
 * Used to track which code path triggered an Up Next change, helping diagnose issues
 * where episodes are unexpectedly added, removed, or reordered.
 *
 * @property value Human readable description of the change source for logging
 */
enum class UpNextChangeSource(val value: String) {
    AutoArchiveAfterPlaying("Auto archive after playing"),
    AutoArchiveInactive("Auto archive inactive"),
    AutoPlay("Auto play"),
    BookmarkPlayButton("Bookmark play button"),
    Discover("Discover"),
    DiscoverPodcastList("Discover podcast list"),
    Chapter("Chapter"),
    ChromecastEventPlay("Chromecast event play"),
    ClearUpNextDialog("Clear dialog"),
    EpisodeCompleted("Episode completed"),
    EpisodeDialog("Episode dialog"),
    EpisodeLimit("Episode limit"),
    Playlist("Playlist"),
    MediaSession("Media session"),
    MiniPlayer("Mini player"),
    MultiSelect("Multi select"),
    Notification("Notification"),
    PlayAllButton("Play all button"),
    PlayAllButtonAppend("Play all button append"),
    PlayButton("Play button"),
    PlayLastButton("Play last button"),
    PlayNextButton("Play next button"),
    Player("Player"),
    PlayerBroadcast("Player broadcast"),
    PodcastPageArchiveAll("Podcast page archive all"),
    PodcastPageArchivePlayed("Podcast page archive played"),
    RefreshAutoAdd("Refresh auto add"),
    RestoreHistory("Restore history"),
    ServerImport("Server import"),
    ServerImportEmpty("Server import empty"),
    SignOutClearData("Sign out and clear data"),
    SkipLast("Skip last"),
    SleepTimerEnd("Sleep timer end"),
    Swipe("Swipe"),
    Tasker("Tasker"),
    TranscriptPlayButton("Transcript play button"),
    UpNextDragAndDrop("Up next drag and drop"),
    UpNextSort("Up next sort"),
    UserEpisode("User episode"),
    UserEpisodePlaybackFailed("User episode playback failed"),
    UserSyncArchive("User sync archive"),
    UserSyncMarkAsPlayed("User sync mark as played"),
    VersionMigrations("Version migrations"),
    WarningPlayButton("Warning play button"),
    Widget("Widget"),
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
