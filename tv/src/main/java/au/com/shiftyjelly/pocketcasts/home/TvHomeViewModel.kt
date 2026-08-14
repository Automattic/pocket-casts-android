package au.com.shiftyjelly.pocketcasts.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverEpisode
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedLoader
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class TvHomeViewModel @Inject constructor(
    private val discoverFeedLoader: TvDiscoverFeedLoader,
    private val playlistManager: PlaylistManager,
    private val podcastDao: PodcastDao,
    private val upNextDao: UpNextDao,
    private val syncManager: SyncManager,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TvHomeUiState>(TvHomeUiState.Loading)
    val uiState: StateFlow<TvHomeUiState> = _uiState.asStateFlow()

    private val _playStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playStarted: SharedFlow<Unit> = _playStarted.asSharedFlow()

    private val _playFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playFailures: SharedFlow<Unit> = _playFailures.asSharedFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            syncManager.isLoggedInObservable.asFlow()
                .distinctUntilChanged()
                .collect { load() }
        }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = TvHomeUiState.Loading
            _uiState.value = try {
                TvHomeUiState.Ready(loadRows())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to load TV home feed")
                TvHomeUiState.Error
            }
        }
    }

    private suspend fun loadRows(): List<TvDiscoverRow> = coroutineScope {
        val isLoggedIn = syncManager.isLoggedIn()
        val localRowsDeferred = async { loadLocalRows(isLoggedIn) }
        val discoverRowsDeferred = async {
            try {
                Result.success(discoverFeedLoader.load(isLoggedIn))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }
        val localRows = localRowsDeferred.await()
        val discoverRows = discoverRowsDeferred.await().getOrElse { exception ->
            // Keep the local rows on screen, surface the error only when there is nothing to show.
            if (localRows.isEmpty()) throw exception
            Timber.e(exception, "Failed to load TV discover rows")
            emptyList()
        }
        (localRows + discoverRows).distinctBy(TvDiscoverRow::id)
    }

    private suspend fun loadLocalRows(isLoggedIn: Boolean): List<TvDiscoverRow> = coroutineScope {
        val upNextEpisodesDeferred = async {
            upNextDao.getUpNextBaseEpisodes(limit = UP_NEXT_LIMIT + 1).filterIsInstance<PodcastEpisode>()
        }
        val newReleasesDeferred = async {
            if (isLoggedIn) {
                playlistManager.smartEpisodesFlow(NEW_RELEASES_RULES)
                    .first()
                    .take(NEW_RELEASES_LIMIT)
                    .map { it.episode }
            } else {
                emptyList()
            }
        }
        val upNextEpisodes = upNextEpisodesDeferred.await()
        val newReleases = newReleasesDeferred.await()
        val podcastTitles = findPodcastTitles(upNextEpisodes + newReleases)

        buildList {
            upNextEpisodes.firstOrNull()?.let { current ->
                add(
                    TvDiscoverRow.Episodes(
                        id = KEEP_LISTENING_ROW_ID,
                        title = context.getString(LR.string.tv_home_keep_listening),
                        episodes = listOf(current.toTvDiscoverEpisode(podcastTitles)),
                    ),
                )
            }
            if (isLoggedIn) {
                val queue = upNextEpisodes.drop(1)
                if (queue.isNotEmpty()) {
                    add(
                        TvDiscoverRow.Episodes(
                            id = UP_NEXT_ROW_ID,
                            title = context.getString(LR.string.up_next),
                            episodes = queue.map { it.toTvDiscoverEpisode(podcastTitles) },
                        ),
                    )
                }
                if (newReleases.isNotEmpty()) {
                    add(
                        TvDiscoverRow.Episodes(
                            id = NEW_RELEASES_ROW_ID,
                            title = context.getString(LR.string.filters_title_new_releases),
                            episodes = newReleases.map { it.toTvDiscoverEpisode(podcastTitles) },
                        ),
                    )
                }
            }
        }
    }

    fun playEpisode(episode: TvDiscoverEpisode) {
        viewModelScope.launch {
            try {
                val found = episodeManager.findByUuid(episode.episodeUuid)
                    ?: run {
                        podcastManager.findOrDownloadPodcastRxSingle(episode.podcastUuid).await()
                        episodeManager.findByUuid(episode.episodeUuid)
                    }
                if (found != null) {
                    playbackManager.playNowSuspend(episode = found, sourceView = SourceView.DISCOVER)
                    _playStarted.tryEmit(Unit)
                } else {
                    Timber.e("Episode %s not found to play from TV home", episode.episodeUuid)
                    _playFailures.tryEmit(Unit)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to play episode from TV home")
                _playFailures.tryEmit(Unit)
            }
        }
    }

    private suspend fun findPodcastTitles(episodes: List<PodcastEpisode>): Map<String, String> {
        val uuids = episodes.map(PodcastEpisode::podcastUuid).distinct()
        if (uuids.isEmpty()) return emptyMap()
        return podcastDao.findAllIn(uuids).associate { it.uuid to it.title }
    }

    private fun PodcastEpisode.toTvDiscoverEpisode(podcastTitles: Map<String, String>) = TvDiscoverEpisode(
        episodeUuid = uuid,
        episodeTitle = title,
        podcastUuid = podcastUuid,
        podcastTitle = podcastTitles[podcastUuid].orEmpty(),
    )

    companion object {
        const val KEEP_LISTENING_ROW_ID = "keep_listening"
        const val UP_NEXT_ROW_ID = "up_next"
        const val NEW_RELEASES_ROW_ID = "new_releases"

        private const val UP_NEXT_LIMIT = 12
        private const val NEW_RELEASES_LIMIT = 12

        private val NEW_RELEASES_RULES = SmartPlaylistDraft.NewReleases.rules.copy(
            episodeStatus = SmartRules.EpisodeStatusRule(unplayed = true, inProgress = false, completed = false),
        )
    }
}

sealed interface TvHomeUiState {
    data object Loading : TvHomeUiState
    data object Error : TvHomeUiState
    data class Ready(val rows: List<TvDiscoverRow>) : TvHomeUiState
}
