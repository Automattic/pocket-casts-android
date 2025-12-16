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

    sealed class UpNextAction(val log: String, val onAddInternal: (() -> Unit)?) {
        data class PlayNow(val episode: BaseEpisode, val onAdd: (() -> Unit)? = null) : UpNextAction(log = "Play now ${episode.uuid}", onAddInternal = onAdd)
        data class PlayNext(val episode: BaseEpisode, val onAdd: (() -> Unit)? = null) : UpNextAction(log = "Play next ${episode.uuid}", onAddInternal = onAdd)
        data class PlayLast(val episode: BaseEpisode, val onAdd: (() -> Unit)? = null) : UpNextAction(log = "Play last ${episode.uuid}", onAddInternal = onAdd)
        data class ReplaceAll(val episodes: List<BaseEpisode>) : UpNextAction(log = "Replace all [${logEpisodeUuids(episodes)}]", onAddInternal = {})
        data class Rearrange(val episodes: List<BaseEpisode>, val onAdd: (() -> Unit)? = null) : UpNextAction(log = "Rearrange [${logEpisodeUuids(episodes)}]", onAddInternal = onAdd)
        data class Remove(val episode: BaseEpisode, val onAdd: (() -> Unit)? = null) : UpNextAction(log = "Remove ${episode.uuid}", onAddInternal = onAdd)
        data class RemoveAndShuffle(val episode: BaseEpisode, val onAdd: (() -> Unit)? = null) : UpNextAction(log = "Remove and shuffle ${episode.uuid}", onAddInternal = onAdd)
        data class Import(val episodes: List<BaseEpisode>, val onAdd: (() -> Unit)? = null) : UpNextAction(log = "Import [${logEpisodeUuids(episodes)}]", onAddInternal = onAdd)
        object ClearAll : UpNextAction(log = "Clear all", onAddInternal = null)
        object ClearAllIncludingChanges : UpNextAction(log = "Clear all including changes", onAddInternal = null)
        object ClearUpNext : UpNextAction(log = "Clear up next", onAddInternal = null)
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

    private fun saveChangesBlocking(action: UpNextAction, source: UpNextChangeSource) {
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Up Next change. From: ${source.value}. Action: ${action.log}")
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

        action.onAddInternal?.invoke()
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
        changeSource: UpNextChangeSource,
        onAdd: (() -> Unit)?,
    ) = withContext(coroutineContext) {
        // Don't build an Up Next if it is already empty
        if (queueEpisodes.isEmpty()) {
            // when the upNextQueue is empty, save the source for auto playing the next episode
            automaticUpNextSource?.let {
                settings.lastAutoPlaySource.set(value = it, updateModifiedAt = true)
            }
            saveChangesBlocking(action = UpNextAction.ClearAll, source = changeSource)
        }
        saveChangesBlocking(action = UpNextAction.PlayNow(episode, onAdd), source = changeSource)
        if (episode.isFinished) {
            episodeManager.markAsNotPlayedBlocking(episode)
        }
    }

    override suspend fun playNextBlocking(episode: BaseEpisode, downloadManager: DownloadManager, changeSource: UpNextChangeSource, onAdd: (() -> Unit)?) {
        playNextNowBlocking(episode = episode, downloadManager = downloadManager, source = changeSource, onAdd = onAdd)
    }

    private suspend fun playNextNowBlocking(episode: BaseEpisode, downloadManager: DownloadManager, source: UpNextChangeSource, onAdd: (() -> Unit)?) = withContext(coroutineContext) {
        saveChangesBlocking(action = UpNextAction.PlayNext(episode, onAdd), source = source)
        downloadIfPossible(episode, downloadManager)
        if (episode.isFinished) {
            episodeManager.markAsNotPlayedBlocking(episode)
        }
    }

    override suspend fun playAllNext(episodes: List<BaseEpisode>, downloadManager: DownloadManager, changeSource: UpNextChangeSource) = withContext(coroutineContext) {
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
            replaceAll(prependedEpisodes, downloadManager, changeSource)
        }
    }

    override suspend fun clearAndPlayAll(episodes: List<BaseEpisode>, downloadManager: DownloadManager, changeSource: UpNextChangeSource) = withContext(coroutineContext) {
        clearAndPlayAllBlocking(episodes, downloadManager, changeSource)
    }

    private fun clearAndPlayAllBlocking(episodes: List<BaseEpisode>, downloadManager: DownloadManager, source: UpNextChangeSource) {
        changeList(episodes, source)
        episodes.forEach { episode ->
            downloadIfPossible(episode, downloadManager)
            if (episode.isFinished) {
                episodeManager.markAsNotPlayedBlocking(episode)
            }
        }
    }

    private fun replaceAll(episodes: List<BaseEpisode>, downloadManager: DownloadManager, source: UpNextChangeSource) {
        saveChangesBlocking(action = UpNextAction.ReplaceAll(episodes), source = source)
        episodes.forEach { episode ->
            downloadIfPossible(episode, downloadManager)
            if (episode.isFinished) {
                episodeManager.markAsNotPlayedBlocking(episode)
            }
        }
    }

    override suspend fun playLast(episode: BaseEpisode, downloadManager: DownloadManager, changeSource: UpNextChangeSource, onAdd: (() -> Unit)?) {
        withContext(coroutineContext) {
            playLastNowBlocking(episode, downloadManager, changeSource, onAdd)
        }
    }

    override suspend fun playAllLast(episodes: List<BaseEpisode>, downloadManager: DownloadManager, changeSource: UpNextChangeSource) = withContext(coroutineContext) {
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
            replaceAll(appendedEpisodes, downloadManager, changeSource)
        }
    }

    private fun playLastNowBlocking(episode: BaseEpisode, downloadManager: DownloadManager, source: UpNextChangeSource, onAdd: (() -> Unit)?) {
        saveChangesBlocking(action = UpNextAction.PlayLast(episode, onAdd), source = source)
        downloadIfPossible(episode, downloadManager)
        if (episode.isFinished) {
            episodeManager.markAsNotPlayedBlocking(episode)
        }
    }

    override suspend fun removeEpisode(episode: BaseEpisode, shouldShuffleUpNext: Boolean, changeSource: UpNextChangeSource) {
        if (contains(episode.uuid)) {
            if (shouldShuffleUpNext) {
                saveChangesBlocking(action = UpNextAction.RemoveAndShuffle(episode), source = changeSource)
            } else {
                saveChangesBlocking(action = UpNextAction.Remove(episode), source = changeSource)
            }
        }
    }

    override fun moveEpisode(from: Int, to: Int, changeSource: UpNextChangeSource) {
        val episodes = queueEpisodes.toMutableList()
        Collections.swap(episodes, from, to)
        currentEpisode?.let { episodes.add(0, it) }
        saveChangesBlocking(action = UpNextAction.Rearrange(episodes), source = changeSource)
    }

    override fun changeList(episodes: List<BaseEpisode>, changeSource: UpNextChangeSource) {
        val mutableEpisodes = episodes.toMutableList()
        currentEpisode?.let { mutableEpisodes.add(0, it) }
        saveChangesBlocking(action = UpNextAction.Rearrange(mutableEpisodes), source = changeSource)
    }

    /**
     * Removes only the episodes in the Up Next queue, not the playing episode.
     */
    override fun clearUpNext(changeSource: UpNextChangeSource) {
        saveChangesBlocking(action = UpNextAction.ClearUpNext, source = changeSource)
    }

    /**
     * Removes all episodes including the playing episode
     */
    override fun removeAll(changeSource: UpNextChangeSource) {
        saveChangesBlocking(action = UpNextAction.ClearAll, source = changeSource)
    }

    /**
     * Removes all episodes including the playing episode and any pending changes
     */
    override suspend fun removeAllIncludingChanges(changeSource: UpNextChangeSource) {
        withContext(Dispatchers.IO) {
            saveChangesBlocking(action = UpNextAction.ClearAllIncludingChanges, source = changeSource)
        }
    }

    override suspend fun importServerChangesBlocking(episodes: List<BaseEpisode>, playbackManager: PlaybackManager, downloadManager: DownloadManager) {
        // don't write over the local Up Next with the server version if we are playing an episode
        val playingEpisode = playbackManager.getCurrentEpisode()
        if (playbackManager.isPlaying() && playingEpisode != null) {
            val firstEpisode = episodes.firstOrNull()
            if (firstEpisode != null && firstEpisode.uuid == playingEpisode.uuid) {
                saveChangesBlocking(action = UpNextAction.Import(episodes), source = UpNextChangeSource.ServerImport)

                episodes.forEach { downloadIfPossible(it, downloadManager) }
            } else {
                // move the playing episode to the top
                val modifiedList = episodes.filterNot { it.uuid == playingEpisode.uuid }.toMutableList()
                modifiedList.add(0, playingEpisode)

                saveChangesBlocking(action = UpNextAction.Import(modifiedList), source = UpNextChangeSource.ServerImport)
                upNextChangeDao.savePlayNowBlocking(playingEpisode)

                modifiedList.forEach { downloadIfPossible(it, downloadManager) }
            }
        } else {
            saveChangesBlocking(action = UpNextAction.Import(episodes), source = UpNextChangeSource.ServerImport)

            episodes.forEach { downloadIfPossible(it, downloadManager) }
        }
    }

    override fun sortUpNext(sortType: UpNextSortType, changeSource: UpNextChangeSource) {
        launch {
            val episodes = withContext(Dispatchers.Default) {
                buildList {
                    currentEpisode?.let(::add)
                    addAll(queueEpisodes.sortedWith(sortType))
                }
            }
            withContext(Dispatchers.IO) {
                saveChangesBlocking(action = UpNextAction.Rearrange(episodes), source = changeSource)
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

private fun logEpisodeUuids(episodes: List<BaseEpisode>): String {
    return if (episodes.size > 10) {
        "${episodes.take(10).joinToString { it.uuid }}... (${episodes.size} total)"
    } else {
        episodes.joinToString { it.uuid }
    }
}
