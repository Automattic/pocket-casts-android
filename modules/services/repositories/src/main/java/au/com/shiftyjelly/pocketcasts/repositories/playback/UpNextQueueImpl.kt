package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextChange
import au.com.shiftyjelly.pocketcasts.models.entity.toUpNextEpisode
import au.com.shiftyjelly.pocketcasts.models.type.UpNextSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.UpNextSyncWorker
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.Relay
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class UpNextQueueImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val settings: Settings,
    private val episodeManager: EpisodeManager,
    private val syncManager: SyncManager,
    @ApplicationContext private val application: Context,
) : UpNextQueue,
    CoroutineScope {

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
        data class ReplaceAll(val episodes: List<BaseEpisode>) : UpNextAction({})
        data class Rearrange(val episodes: List<BaseEpisode>, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class Remove(val episode: BaseEpisode, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class RemoveAndShuffle(val episode: BaseEpisode, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        data class Import(val episodes: List<BaseEpisode>, val onAdd: (() -> Unit)? = null) : UpNextAction(onAdd)
        object ClearAll : UpNextAction(null)
        object ClearAllIncludingChanges : UpNextAction(null)
        object ClearUpNext : UpNextAction(null)
    }

    override fun setupBlocking() {
        val initState = updateStateBlocking()
        updateCurrentEpisodeState(initState)

        // listen for user changes and send to server
        changesObservable.observeOn(Schedulers.io())
            // send server changes in bulk
            .debounce(5, TimeUnit.SECONDS)
            .doOnNext { sendToServerBlocking() }
            .subscribeBy(onError = { Timber.e(it) })
            .addTo(disposables)
    }

    private fun updateStateBlocking(shouldShuffleUpNext: Boolean = false): UpNextQueue.State {
        val state: UpNextQueue.State
        val episodes: MutableList<BaseEpisode> = upNextDao.findAllEpisodesSortedBlocking().toMutableList()
        if (episodes.isEmpty()) {
            state = UpNextQueue.State.Empty
        } else {
            val index = if (shouldShuffleUpNext) Random.nextInt(episodes.size) else 0
            val episode: BaseEpisode = episodes.removeAt(index)
            val previousState: UpNextQueue.State = changesObservable.blockingFirst()
            val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else null
            val podcast: Podcast? = if (previousState is UpNextQueue.State.Loaded && previousState.podcast?.uuid == podcastUuid) {
                previousState.podcast
            } else if (podcastUuid != null) {
                podcastDao.findByUuidBlocking(podcastUuid)
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

    private fun saveChangesBlocking(action: UpNextAction) {
        when (action) {
            is UpNextAction.PlayNow -> insertUpNextEpisodeBlocking(episode = action.episode, position = 0)
            is UpNextAction.PlayNext -> insertUpNextEpisodeBlocking(episode = action.episode, position = 1)
            is UpNextAction.PlayLast -> insertUpNextEpisodeBlocking(episode = action.episode, position = -1)
            is UpNextAction.ReplaceAll -> appDatabase.runInTransaction {
                upNextDao.deleteAllBlocking()
                val episodes = action.episodes.mapIndexed { index, episode ->
                    episode.toUpNextEpisode(position = index)
                }
                upNextDao.insertAllBlocking(episodes)
            }
            is UpNextAction.Remove -> upNextDao.deleteByUuidBlocking(uuid = action.episode.uuid)
            is UpNextAction.RemoveAndShuffle -> upNextDao.deleteByUuidBlocking(uuid = action.episode.uuid)
            is UpNextAction.Rearrange -> upNextDao.saveAllBlocking(episodes = action.episodes)
            is UpNextAction.Import -> upNextDao.saveAllBlocking(episodes = action.episodes)
            is UpNextAction.ClearUpNext -> upNextDao.deleteAllNotCurrentBlocking()
            is UpNextAction.ClearAll -> upNextDao.deleteAllBlocking()
            is UpNextAction.ClearAllIncludingChanges -> {
                upNextDao.deleteAllBlocking()
                upNextChangeDao.deleteAllBlocking()
            }
        }

        // save changes to sync to the server
        if (syncManager.isLoggedIn()) {
            when (action) {
                is UpNextAction.PlayNow -> upNextChangeDao.savePlayNowBlocking(action.episode)
                is UpNextAction.PlayNext -> upNextChangeDao.savePlayNextBlocking(action.episode)
                is UpNextAction.PlayLast -> upNextChangeDao.savePlayLastBlocking(action.episode)
                is UpNextAction.ReplaceAll -> upNextChangeDao.saveReplace(action.episodes.map(BaseEpisode::uuid))
                is UpNextAction.Remove -> upNextChangeDao.saveRemoveBlocking(action.episode)
                is UpNextAction.RemoveAndShuffle -> upNextChangeDao.saveRemoveBlocking(action.episode)
                is UpNextAction.Rearrange -> upNextChangeDao.saveReplace(action.episodes.map { it.uuid })
                is UpNextAction.ClearUpNext -> upNextChangeDao.saveReplace(listOfNotNull(currentEpisode).map { it.uuid })
                is UpNextAction.ClearAll -> upNextChangeDao.saveReplace(emptyList())
                else -> {}
            }
        }

        val shouldShuffleUpNext = action is UpNextAction.RemoveAndShuffle
        val state = updateStateBlocking(shouldShuffleUpNext = shouldShuffleUpNext)
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
                settings.lastAutoPlaySource.set(value = it, updateModifiedAt = true)
            }
            saveChangesBlocking(UpNextAction.ClearAll)
        }
        saveChangesBlocking(UpNextAction.PlayNow(episode, onAdd))
        if (episode.isFinished) {
            episodeManager.markAsNotPlayedBlocking(episode)
        }
    }

    override suspend fun playNextBlocking(episode: BaseEpisode, downloadManager: DownloadManager, onAdd: (() -> Unit)?) {
        playNextNowBlocking(episode, downloadManager, onAdd)
    }

    private suspend fun playNextNowBlocking(episode: BaseEpisode, downloadManager: DownloadManager, onAdd: (() -> Unit)?) = withContext(coroutineContext) {
        saveChangesBlocking(UpNextAction.PlayNext(episode, onAdd))
        downloadIfPossible(episode, downloadManager)
        if (episode.isFinished) {
            episodeManager.markAsNotPlayedBlocking(episode)
        }
    }

    override suspend fun playAllNext(episodes: List<BaseEpisode>, downloadManager: DownloadManager) = withContext(coroutineContext) {
        appDatabase.runInTransaction {
            val current = currentEpisode
            val queued = queueEpisodes
            val newEpisodesUuids = episodes.mapTo(mutableSetOf(), BaseEpisode::uuid)
            val prependedEpisodes = buildList {
                if (current != null) {
                    add(current)
                }
                addAll(episodes)
                for (episode in queued) {
                    if (episode.uuid !in newEpisodesUuids) {
                        add(episode)
                    }
                }
            }
            replaceAll(prependedEpisodes, downloadManager)
        }
    }

    override suspend fun clearAndPlayAll(episodes: List<BaseEpisode>, downloadManager: DownloadManager) = withContext(coroutineContext) {
        clearAndPlayAllBlocking(episodes, downloadManager)
    }

    private fun clearAndPlayAllBlocking(episodes: List<BaseEpisode>, downloadManager: DownloadManager) {
        changeList(episodes)
        episodes.forEach { episode ->
            downloadIfPossible(episode, downloadManager)
            if (episode.isFinished) {
                episodeManager.markAsNotPlayedBlocking(episode)
            }
        }
    }

    private fun replaceAll(episodes: List<BaseEpisode>, downloadManager: DownloadManager) {
        saveChangesBlocking(UpNextAction.ReplaceAll(episodes))
        episodes.forEach { episode ->
            downloadIfPossible(episode, downloadManager)
            if (episode.isFinished) {
                episodeManager.markAsNotPlayedBlocking(episode)
            }
        }
    }

    override suspend fun playLast(episode: BaseEpisode, downloadManager: DownloadManager, onAdd: (() -> Unit)?) {
        withContext(coroutineContext) {
            playLastNowBlocking(episode, downloadManager, onAdd)
        }
    }

    override suspend fun playAllLast(episodes: List<BaseEpisode>, downloadManager: DownloadManager) = withContext(coroutineContext) {
        appDatabase.runInTransaction {
            val current = currentEpisode
            val queued = queueEpisodes
            val newEpisodesUuids = episodes.mapTo(mutableSetOf(), BaseEpisode::uuid)
            val appendedEpisodes = buildList {
                if (current != null) {
                    add(current)
                }
                for (episode in queued) {
                    if (episode.uuid !in newEpisodesUuids) {
                        add(episode)
                    }
                }
                addAll(episodes)
            }
            replaceAll(appendedEpisodes, downloadManager)
        }
    }

    private fun playLastNowBlocking(episode: BaseEpisode, downloadManager: DownloadManager, onAdd: (() -> Unit)?) {
        saveChangesBlocking(UpNextAction.PlayLast(episode, onAdd))
        downloadIfPossible(episode, downloadManager)
        if (episode.isFinished) {
            episodeManager.markAsNotPlayedBlocking(episode)
        }
    }

    override suspend fun removeEpisode(episode: BaseEpisode, shouldShuffleUpNext: Boolean) {
        if (contains(episode.uuid)) {
            if (shouldShuffleUpNext) {
                saveChangesBlocking(UpNextAction.RemoveAndShuffle(episode))
            } else {
                saveChangesBlocking(UpNextAction.Remove(episode))
            }
        }
    }

    override fun moveEpisode(from: Int, to: Int) {
        val episodes = queueEpisodes.toMutableList()
        Collections.swap(episodes, from, to)
        currentEpisode?.let { episodes.add(0, it) }
        saveChangesBlocking(UpNextAction.Rearrange(episodes))
    }

    override fun changeList(episodes: List<BaseEpisode>) {
        val mutableEpisodes = episodes.toMutableList()
        currentEpisode?.let { mutableEpisodes.add(0, it) }
        saveChangesBlocking(UpNextAction.Rearrange(mutableEpisodes))
    }

    /**
     * Removes only the episodes in the Up Next queue, not the playing episode.
     */
    override fun clearUpNext() {
        saveChangesBlocking(UpNextAction.ClearUpNext)
    }

    /**
     * Removes all episodes including the playing episode
     */
    override fun removeAll() {
        saveChangesBlocking(UpNextAction.ClearAll)
    }

    /**
     * Removes all episodes including the playing episode and any pending changes
     */
    override suspend fun removeAllIncludingChanges() {
        withContext(Dispatchers.IO) {
            saveChangesBlocking(UpNextAction.ClearAllIncludingChanges)
        }
    }

    override suspend fun importServerChangesBlocking(episodes: List<BaseEpisode>, playbackManager: PlaybackManager, downloadManager: DownloadManager) {
        // don't write over the local Up Next with the server version if we are playing an episode
        val playingEpisode = playbackManager.getCurrentEpisode()
        if (playbackManager.isPlaying() && playingEpisode != null) {
            val firstEpisode = episodes.firstOrNull()
            if (firstEpisode != null && firstEpisode.uuid == playingEpisode.uuid) {
                saveChangesBlocking(UpNextAction.Import(episodes))

                episodes.forEach { downloadIfPossible(it, downloadManager) }
            } else {
                // move the playing episode to the top
                val modifiedList = episodes.filterNot { it.uuid == playingEpisode.uuid }.toMutableList()
                modifiedList.add(0, playingEpisode)

                saveChangesBlocking(UpNextAction.Import(modifiedList))
                upNextChangeDao.savePlayNowBlocking(playingEpisode)

                modifiedList.forEach { downloadIfPossible(it, downloadManager) }
            }
        } else {
            saveChangesBlocking(UpNextAction.Import(episodes))

            episodes.forEach { downloadIfPossible(it, downloadManager) }
        }
    }

    override fun sortUpNext(sortType: UpNextSortType) {
        launch {
            val episodes = withContext(Dispatchers.Default) {
                buildList {
                    currentEpisode?.let(::add)
                    addAll(queueEpisodes.sortedWith(sortType))
                }
            }
            withContext(Dispatchers.IO) {
                saveChangesBlocking(UpNextAction.Rearrange(episodes))
            }
        }
    }

    private fun insertUpNextEpisodeBlocking(episode: BaseEpisode, position: Int) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Inserting ${episode.title} in to up next at $position")
        upNextDao.insertAtBlocking(upNextEpisode = episode.toUpNextEpisode(), position = position, replaceOneEpisode = false)
        if (episode.isArchived) {
            episodeManager.unarchiveBlocking(episode)
        }

        // clear last loaded uuid if anything gets added to the up next queue
        val hasQueuedItems = currentEpisode != null
        if (hasQueuedItems) {
            settings.trackingAutoPlaySource.set(AutoPlaySource.Predefined.None, updateModifiedAt = false)
            settings.lastAutoPlaySource.set(AutoPlaySource.Predefined.None, updateModifiedAt = true)
        }
    }

    private fun downloadIfPossible(episode: BaseEpisode, downloadManager: DownloadManager) {
        if (settings.autoDownloadUpNext.value) {
            DownloadHelper.addAutoDownloadedEpisodeToQueue(episode, "up next auto download", downloadManager, episodeManager, source = SourceView.UP_NEXT)
        }
    }

    private fun sendToServerBlocking() {
        val changes: List<UpNextChange> = upNextChangeDao.findAllBlocking()
        if (changes.isEmpty()) {
            return
        }
        UpNextSyncWorker.enqueue(syncManager, application)
    }
}
