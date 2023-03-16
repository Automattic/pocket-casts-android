package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextChange
import au.com.shiftyjelly.pocketcasts.models.entity.toUpNextEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.UpNextSyncJob
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.Relay
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class UpNextQueueImpl @Inject constructor(
    appDatabase: AppDatabase,
    private val settings: Settings,
    private val episodeManager: EpisodeManager,
    @ApplicationContext private val application: Context
) : UpNextQueue, CoroutineScope {

    private val upNextDao = appDatabase.upNextDao()
    private val upNextChangeDao = appDatabase.upNextChangeDao()
    private val podcastDao = appDatabase.podcastDao()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override val changesObservable: Observable<UpNextQueue.State> by lazy {
        val relay = BehaviorRelay.create<UpNextQueue.State>().toSerialized()
        relay.accept(UpNextQueue.State.Empty)
        return@lazy relay
    }

    private val disposables = CompositeDisposable()

    override val currentEpisode: Playable?
        get() = (changesObservable.blockingFirst() as? UpNextQueue.State.Loaded)?.episode

    override val queueEpisodes: List<Playable>
        get() = (changesObservable.blockingFirst() as? UpNextQueue.State.Loaded)?.queue ?: emptyList()

    override val isEmpty: Boolean
        get() = changesObservable.blockingFirst() is UpNextQueue.State.Empty

    sealed class UpNextAction(val _onAdd: (() -> Unit)?) {
        data class PlayNow(val episode: Playable, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class PlayNext(val episode: Playable, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class PlayLast(val episode: Playable, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class Rearrange(val episodes: List<Playable>, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class Remove(val episode: Playable, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class Import(val episodes: List<Playable>, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        object ClearAll : UpNextAction(null)
        object ClearAllIncludingChanges : UpNextAction(null)
        object ClearUpNext : UpNextAction(null)
    }

    override fun setup() {
        val initState = updateState()
        (changesObservable as Relay).accept(initState)

        // listen for user changes and send to server
        changesObservable.observeOn(Schedulers.io())
            // send server changes in bulk
            .debounce(5, TimeUnit.SECONDS)
            .doOnNext { sendToServer() }
            .subscribeBy(onError = { Timber.e(it) })
            .addTo(disposables)
    }

    private fun updateState(): UpNextQueue.State {
        val state: UpNextQueue.State
        val episodes: MutableList<Playable> = upNextDao.findAllPlayablesSorted().toMutableList()
        if (episodes.isEmpty()) {
            state = UpNextQueue.State.Empty
        } else {
            val episode: Playable = episodes.removeAt(0)
            val previousState: UpNextQueue.State = changesObservable.blockingFirst()
            val podcastUuid = if (episode is Episode) episode.podcastUuid else null
            val podcast: Podcast? = if (previousState is UpNextQueue.State.Loaded && previousState.podcast?.uuid == podcastUuid) {
                previousState.podcast
            } else if (podcastUuid != null) {
                podcastDao.findByUuid(podcastUuid)
            } else {
                null
            }

            state = UpNextQueue.State.Loaded(episode, podcast, episodes)
        }
        return state
    }

    private fun saveChanges(action: UpNextAction) {
        when (action) {
            is UpNextAction.PlayNow -> insertUpNextEpisode(episode = action.episode, position = 0)
            is UpNextAction.PlayNext -> insertUpNextEpisode(episode = action.episode, position = 1)
            is UpNextAction.PlayLast -> insertUpNextEpisode(episode = action.episode, position = -1)
            is UpNextAction.Remove -> upNextDao.deleteByUuid(uuid = action.episode.uuid)
            is UpNextAction.Rearrange -> upNextDao.saveAll(episodes = action.episodes)
            is UpNextAction.Import -> upNextDao.saveAll(episodes = action.episodes)
            is UpNextAction.ClearUpNext -> upNextDao.deleteAllNotCurrent()
            is UpNextAction.ClearAll -> upNextDao.deleteAll()
            is UpNextAction.ClearAllIncludingChanges -> {
                upNextDao.deleteAll()
                upNextChangeDao.deleteAll()
            }
        }

        // save changes to sync to the server
        if (settings.isLoggedIn()) {
            when (action) {
                is UpNextAction.PlayNow -> upNextChangeDao.savePlayNow(action.episode)
                is UpNextAction.PlayNext -> upNextChangeDao.savePlayNext(action.episode)
                is UpNextAction.PlayLast -> upNextChangeDao.savePlayLast(action.episode)
                is UpNextAction.Remove -> upNextChangeDao.saveRemove(action.episode)
                is UpNextAction.Rearrange -> upNextChangeDao.saveReplace(action.episodes.map { it.uuid })
                is UpNextAction.ClearUpNext -> upNextChangeDao.saveReplace(listOfNotNull(currentEpisode).map { it.uuid })
                is UpNextAction.ClearAll -> upNextChangeDao.saveReplace(emptyList())
                else -> {}
            }
        }

        val state = updateState()
        (changesObservable as Relay).accept(state)

        action._onAdd?.invoke()
    }

    override fun isCurrentEpisode(episode: Playable): Boolean {
        return currentEpisode?.let { episode.uuid == it.uuid } ?: false
    }

    override fun contains(uuid: String): Boolean {
        return queueEpisodes.any { it.uuid == uuid } || (currentEpisode?.let { it.uuid == uuid } ?: false)
    }

    override suspend fun playNow(episode: Playable, onAdd: (() -> Unit)?) = withContext(coroutineContext) {
        // Don't build an Up Next if it is already empty
        if (queueEpisodes.isEmpty()) {
            saveChanges(UpNextAction.ClearAll)
        }
        saveChanges(UpNextAction.PlayNow(episode, onAdd))
        if (episode.isFinished) {
            episodeManager.markAsNotPlayed(episode)
        }
    }

    override suspend fun playNext(episode: Playable, downloadManager: DownloadManager, onAdd: (() -> Unit)?) {
        playNextNow(episode, downloadManager, onAdd)
    }

    private suspend fun playNextNow(episode: Playable, downloadManager: DownloadManager, onAdd: (() -> Unit)?) = withContext(coroutineContext) {
        saveChanges(UpNextAction.PlayNext(episode, onAdd))
        downloadIfPossible(episode, downloadManager)
        if (episode.isFinished) {
            episodeManager.markAsNotPlayed(episode)
        }
    }

    override suspend fun playAllNext(episodes: List<Playable>, downloadManager: DownloadManager) = withContext(coroutineContext) {
        if (episodes.isEmpty()) {
            return@withContext
        }

        val mutableList = episodes.toMutableList()
        if (isEmpty) {
            playNextNow(mutableList.first(), downloadManager, null)
            mutableList.removeFirst()
        }

        mutableList.asReversed().forEach {
            playNextNow(it, downloadManager, null)
        }
    }

    override suspend fun clearAndPlayAll(episodes: List<Playable>, downloadManager: DownloadManager) = withContext(coroutineContext) {
        changeList(episodes)
        episodes.forEach { episode ->
            downloadIfPossible(episode, downloadManager)
            if (episode.isFinished) {
                episodeManager.markAsNotPlayed(episode)
            }
        }
    }

    override suspend fun playLast(episode: Playable, downloadManager: DownloadManager, onAdd: (() -> Unit)?) {
        playLastNow(episode, downloadManager, onAdd)
    }

    private suspend fun playLastNow(episode: Playable, downloadManager: DownloadManager, onAdd: (() -> Unit)?) = withContext(coroutineContext) {
        saveChanges(UpNextAction.PlayLast(episode, onAdd))
        downloadIfPossible(episode, downloadManager)
        if (episode.isFinished) {
            episodeManager.markAsNotPlayed(episode)
        }
    }

    override suspend fun playAllLast(episodes: List<Playable>, downloadManager: DownloadManager) = withContext(coroutineContext) {
        episodes.forEach { playLastNow(it, downloadManager, null) }
    }

    override suspend fun removeEpisode(episode: Playable) {
        if (contains(episode.uuid)) {
            saveChanges(UpNextAction.Remove(episode))
        }
    }

    override fun moveEpisode(from: Int, to: Int) {
        val episodes = queueEpisodes.toMutableList()
        Collections.swap(episodes, from, to)
        currentEpisode?.let { episodes.add(0, it) }
        saveChanges(UpNextAction.Rearrange(episodes))
    }

    override fun changeList(episodes: List<Playable>) {
        val mutableEpisodes = episodes.toMutableList()
        currentEpisode?.let { mutableEpisodes.add(0, it) }
        saveChanges(UpNextAction.Rearrange(mutableEpisodes))
    }

    /**
     * Removes only the episodes in the Up Next queue, not the playing episode.
     */
    override fun clearUpNext() {
        saveChanges(UpNextAction.ClearUpNext)
    }

    /**
     * Removes all episodes including the playing episode
     */
    override fun removeAll() {
        saveChanges(UpNextAction.ClearAll)
    }

    /**
     * Removes all episodes including the playing episode and any pending changes
     */
    override suspend fun removeAllIncludingChanges() {
        withContext(Dispatchers.IO) {
            saveChanges(UpNextAction.ClearAllIncludingChanges)
        }
    }

    override fun importServerChanges(episodes: List<Playable>, playbackManager: PlaybackManager, downloadManager: DownloadManager): Completable {
        return Completable.fromAction {
            // don't write over the local Up Next with the server version if we are playing an episode
            val playingEpisode = playbackManager.getCurrentEpisode()
            if (playbackManager.isPlaying() && playingEpisode != null) {
                val firstEpisode = episodes.firstOrNull()
                if (firstEpisode != null && firstEpisode.uuid == playingEpisode.uuid) {
                    saveChanges(UpNextAction.Import(episodes))

                    episodes.forEach { downloadIfPossible(it, downloadManager) }
                } else {
                    // move the playing episode to the top
                    val modifiedList = episodes.filterNot { it.uuid == playingEpisode.uuid }.toMutableList()
                    modifiedList.add(0, playingEpisode)

                    saveChanges(UpNextAction.Import(modifiedList))
                    upNextChangeDao.savePlayNow(playingEpisode)

                    modifiedList.forEach { downloadIfPossible(it, downloadManager) }
                }
            } else {
                saveChanges(UpNextAction.Import(episodes))

                episodes.forEach { downloadIfPossible(it, downloadManager) }
            }
        }
    }

    private fun insertUpNextEpisode(episode: Playable, position: Int) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Inserting ${episode.title} in to up next at $position")
        upNextDao.insertAt(upNextEpisode = episode.toUpNextEpisode(), position = position, replaceOneEpisode = false)
        if (episode.isArchived) {
            episodeManager.unarchive(episode)
        }
    }

    private fun downloadIfPossible(episode: Playable, downloadManager: DownloadManager) {
        if (settings.isUpNextAutoDownloaded()) {
            DownloadHelper.addAutoDownloadedEpisodeToQueue(episode, "up next auto download", downloadManager, episodeManager)
        }
    }

    private fun sendToServer() {
        val changes: List<UpNextChange> = upNextChangeDao.findAll()
        if (changes.isEmpty()) {
            return
        }
        UpNextSyncJob.run(settings, application)
    }
}
