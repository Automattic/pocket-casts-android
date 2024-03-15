package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextChange
import au.com.shiftyjelly.pocketcasts.models.entity.toUpNextEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
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
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class UpNextQueueImpl @Inject constructor(
    appDatabase: AppDatabase,
    private val settings: Settings,
    private val episodeManager: EpisodeManager,
    private val syncManager: SyncManager,
    @ApplicationContext private val application: Context,
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

    override val currentEpisode: BaseEpisode?
        get() = (changesObservable.blockingFirst() as? UpNextQueue.State.Loaded)?.episode

    override val queueEpisodes: List<BaseEpisode>
        get() = (changesObservable.blockingFirst() as? UpNextQueue.State.Loaded)?.queue ?: emptyList()

    override val isEmpty: Boolean
        get() = changesObservable.blockingFirst() is UpNextQueue.State.Empty

    sealed class UpNextAction(val _onAdd: (() -> Unit)?) {
        data class PlayNow(val episode: BaseEpisode, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class PlayNext(val episode: BaseEpisode, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class PlayLast(val episode: BaseEpisode, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class Rearrange(val episodes: List<BaseEpisode>, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class Remove(val episode: BaseEpisode, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class Import(val episodes: List<BaseEpisode>, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        object ClearAll : UpNextAction(null)
        object ClearAllIncludingChanges : UpNextAction(null)
        object ClearUpNext : UpNextAction(null)
    }

    override fun setup() {
        val initState = updateState()
        updateCurrentEpisodeState(initState)

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
        val episodes: MutableList<BaseEpisode> = upNextDao.findAllEpisodesSorted().toMutableList()
        if (episodes.isEmpty()) {
            state = UpNextQueue.State.Empty
        } else {
            val episode: BaseEpisode = episodes.removeAt(0)
            val previousState: UpNextQueue.State = changesObservable.blockingFirst()
            val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else null
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

    override fun updateCurrentEpisodeState(state: UpNextQueue.State) {
        (changesObservable as Relay).accept(state)
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
        if (syncManager.isLoggedIn()) {
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
        updateCurrentEpisodeState(state)

        action._onAdd?.invoke()
    }

    override fun isCurrentEpisode(episode: BaseEpisode): Boolean {
        return currentEpisode?.let { episode.uuid == it.uuid } ?: false
    }

    override fun contains(uuid: String): Boolean {
        return queueEpisodes.any { it.uuid == uuid } || (currentEpisode?.let { it.uuid == uuid } ?: false)
    }

    override suspend fun playNow(
        episode: BaseEpisode,
        automaticUpNextSource: AutoPlaySource?,
        onAdd: (() -> Unit)?,
    ) = withContext(coroutineContext) {
        // Don't build an Up Next if it is already empty
        if (queueEpisodes.isEmpty()) {
            // when the upNextQueue is empty, save the source for auto playing the next episode
            automaticUpNextSource?.let {
                settings.lastAutoPlaySource.set(value = it, needsSync = true)
            }
            saveChanges(UpNextAction.ClearAll)
        }
        saveChanges(UpNextAction.PlayNow(episode, onAdd))
        if (episode.isFinished) {
            episodeManager.markAsNotPlayed(episode)
        }
    }

    override suspend fun playNext(episode: BaseEpisode, downloadManager: DownloadManager, onAdd: (() -> Unit)?) {
        playNextNow(episode, downloadManager, onAdd)
    }

    private suspend fun playNextNow(episode: BaseEpisode, downloadManager: DownloadManager, onAdd: (() -> Unit)?) = withContext(coroutineContext) {
        saveChanges(UpNextAction.PlayNext(episode, onAdd))
        downloadIfPossible(episode, downloadManager)
        if (episode.isFinished) {
            episodeManager.markAsNotPlayed(episode)
        }
    }

    override suspend fun playAllNext(episodes: List<BaseEpisode>, downloadManager: DownloadManager) = withContext(coroutineContext) {
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

    override suspend fun clearAndPlayAll(episodes: List<BaseEpisode>, downloadManager: DownloadManager) = withContext(coroutineContext) {
        changeList(episodes)
        episodes.forEach { episode ->
            downloadIfPossible(episode, downloadManager)
            if (episode.isFinished) {
                episodeManager.markAsNotPlayed(episode)
            }
        }
    }

    override suspend fun playLast(episode: BaseEpisode, downloadManager: DownloadManager, onAdd: (() -> Unit)?) {
        playLastNow(episode, downloadManager, onAdd)
    }

    private suspend fun playLastNow(episode: BaseEpisode, downloadManager: DownloadManager, onAdd: (() -> Unit)?) = withContext(coroutineContext) {
        saveChanges(UpNextAction.PlayLast(episode, onAdd))
        downloadIfPossible(episode, downloadManager)
        if (episode.isFinished) {
            episodeManager.markAsNotPlayed(episode)
        }
    }

    override suspend fun playAllLast(episodes: List<BaseEpisode>, downloadManager: DownloadManager) = withContext(coroutineContext) {
        episodes.forEach { playLastNow(it, downloadManager, null) }
    }

    override suspend fun removeEpisode(episode: BaseEpisode) {
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

    override fun changeList(episodes: List<BaseEpisode>) {
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

    override fun importServerChanges(episodes: List<BaseEpisode>, playbackManager: PlaybackManager, downloadManager: DownloadManager): Completable {
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

    private fun insertUpNextEpisode(episode: BaseEpisode, position: Int) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Inserting ${episode.title} in to up next at $position")
        upNextDao.insertAt(upNextEpisode = episode.toUpNextEpisode(), position = position, replaceOneEpisode = false)
        if (episode.isArchived) {
            episodeManager.unarchive(episode)
        }

        // clear last loaded uuid if anything gets added to the up next queue
        val hasQueuedItems = currentEpisode != null
        if (hasQueuedItems) {
            settings.trackingAutoPlaySource.set(AutoPlaySource.None, needsSync = false)
            settings.lastAutoPlaySource.set(AutoPlaySource.None, needsSync = true)
        }
    }

    private fun downloadIfPossible(episode: BaseEpisode, downloadManager: DownloadManager) {
        if (settings.autoDownloadUpNext.value) {
            DownloadHelper.addAutoDownloadedEpisodeToQueue(episode, "up next auto download", downloadManager, episodeManager)
        }
    }

    private fun sendToServer() {
        val changes: List<UpNextChange> = upNextChangeDao.findAll()
        if (changes.isEmpty()) {
            return
        }
        UpNextSyncJob.run(syncManager, application)
    }
}
