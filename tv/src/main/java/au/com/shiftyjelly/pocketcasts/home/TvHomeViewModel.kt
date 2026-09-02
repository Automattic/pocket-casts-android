package au.com.shiftyjelly.pocketcasts.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.TvCategoryPodcasts
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverBanner
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverEpisode
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedAnalytics
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedLoader
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverPodcast
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverPodcastAttribution
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.discover.TvOpenedCategory
import au.com.shiftyjelly.pocketcasts.discover.TvProgressCardStyle
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.HomeShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
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
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
    private val discoverPodcastAttribution: TvDiscoverPodcastAttribution,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TvHomeUiState>(TvHomeUiState.Loading)
    val uiState: StateFlow<TvHomeUiState> = _uiState.asStateFlow()

    private val _playStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playStarted: SharedFlow<Unit> = _playStarted.asSharedFlow()

    private val _playFailures = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val playFailures: SharedFlow<Unit> = _playFailures.asSharedFlow()

    private val discoverFeedAnalytics = TvDiscoverFeedAnalytics(eventHorizon, settings, SOURCE_HOME, LOCAL_ROW_IDS, discoverPodcastAttribution)

    private var loadJob: Job? = null

    @Volatile
    private var playbackStartedThisSession = false

    init {
        viewModelScope.launch {
            syncManager.isLoggedInObservable.asFlow()
                .distinctUntilChanged()
                .collect { load() }
        }
        observeLocalRowSignals()
    }

    @OptIn(FlowPreview::class)
    private fun observeLocalRowSignals() {
        viewModelScope.launch {
            merge(
                playbackManager.upNextQueue.changesObservable.asFlow().map {},
                playbackManager.playbackStateFlow
                    .onEach { if (it.isPlaying) playbackStartedThisSession = true }
                    .map { it.isPlaying to it.episodeUuid }
                    .distinctUntilChanged()
                    .map {},
                playlistManager.smartEpisodesFlow(NEW_RELEASES_RULES).distinctUntilChanged().map {},
            )
                .debounce(LOCAL_ROWS_DEBOUNCE_MS)
                .collect { refreshLocalRows() }
        }
    }

    private suspend fun refreshLocalRows() {
        if (_uiState.value !is TvHomeUiState.Ready) return
        val isLoggedIn = syncManager.isLoggedIn()
        val localRows = loadLocalRows(isLoggedIn)
        _uiState.update { current ->
            if (current is TvHomeUiState.Ready && isLoggedIn == syncManager.isLoggedIn()) {
                val nonLocalRows = current.rows.filterNot { it.id in LOCAL_ROW_IDS }
                TvHomeUiState.Ready((localRows + nonLocalRows).distinctBy(TvDiscoverRow::id))
            } else {
                current
            }
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
            if (!playbackStartedThisSession) {
                upNextEpisodes.firstOrNull()?.let { current ->
                    add(
                        TvDiscoverRow.Episodes(
                            id = KEEP_LISTENING_ROW_ID,
                            title = context.getString(LR.string.tv_home_keep_listening),
                            episodes = listOf(current.toTvDiscoverEpisode(podcastTitles)),
                            progressCardStyle = TvProgressCardStyle.Resume,
                        ),
                    )
                }
            }
            if (isLoggedIn) {
                val queue = upNextEpisodes.drop(1)
                if (queue.size >= UP_NEXT_ROW_MIN) {
                    add(
                        TvDiscoverRow.Episodes(
                            id = UP_NEXT_ROW_ID,
                            title = context.getString(LR.string.up_next),
                            episodes = queue.map { it.toTvDiscoverEpisode(podcastTitles) },
                            progressCardStyle = TvProgressCardStyle.Queue,
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

    fun retryDiscoverRow(row: TvDiscoverRow) {
        viewModelScope.launch {
            val reloaded = try {
                discoverFeedLoader.reloadHomeRow(row.id, syncManager.isLoggedIn()) ?: row
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to reload TV home row ${row.id}")
                row
            }
            replaceRow(row.id, reloaded)
        }
    }

    private fun replaceRow(rowId: String, newRow: TvDiscoverRow?) {
        val current = _uiState.value as? TvHomeUiState.Ready ?: return
        _uiState.value = TvHomeUiState.Ready(current.rows.mapNotNull { if (it.id == rowId) newRow else it })
    }

    suspend fun categoryPodcasts(categoryId: Int, source: String): TvCategoryPodcasts {
        return discoverFeedLoader.loadCategoryPodcasts(source, categoryId, syncManager.isLoggedIn())
    }

    suspend fun categoryCoverUrls(category: DiscoverCategory): List<String> {
        return discoverFeedLoader.loadCategoryCoverUrls(category.source)
    }

    fun trackHomeShown() {
        eventHorizon.track(HomeShownEvent)
    }

    fun trackDiscoverListShown(row: TvDiscoverRow) = discoverFeedAnalytics.trackListImpression(row)

    fun trackDiscoverPodcastTapped(row: TvDiscoverRow, podcast: TvDiscoverPodcast) = discoverFeedAnalytics.trackPodcastTapped(row, podcast)

    fun trackDiscoverEpisodePlayed(row: TvDiscoverRow, episode: TvDiscoverEpisode) = discoverFeedAnalytics.trackEpisodePlayed(row, episode)

    fun trackDiscoverEpisodePodcastTapped(row: TvDiscoverRow, episode: TvDiscoverEpisode) = discoverFeedAnalytics.trackEpisodePodcastTapped(row, episode)

    fun trackCategoryPodcastTapped(category: TvOpenedCategory, listId: String?, podcast: TvDiscoverPodcast) = discoverFeedAnalytics.trackCategoryPodcastTapped(category, listId, podcast)

    fun trackCategoryPillTapped(category: DiscoverCategory, index: Int) = discoverFeedAnalytics.trackCategoryPillTapped(category, index)

    fun trackBannerTapped(banner: TvDiscoverBanner) = discoverFeedAnalytics.trackBannerTapped(banner)

    fun isPlaying(): Boolean = playbackManager.isPlaying()

    fun playEpisode(episode: TvDiscoverEpisode) {
        viewModelScope.launch {
            try {
                val found = episodeManager.findByUuid(episode.episodeUuid)
                    ?: run {
                        podcastManager.findOrDownloadPodcastRxSingle(episode.podcastUuid).await()
                        episodeManager.findByUuid(episode.episodeUuid)
                            ?: episodeManager.downloadMissingPodcastEpisode(episode.episodeUuid, episode.podcastUuid)
                    }
                    ?: episode.toPlayableEpisode()?.let { playable ->
                        episodeManager.add(listOf(playable), playable.podcastUuid, downloadMetaData = false)
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

    fun playLatestEpisode(row: TvDiscoverRow, podcast: TvDiscoverPodcast) {
        viewModelScope.launch {
            try {
                val loadedPodcast = podcastManager.findOrDownloadPodcastRxSingle(podcast.uuid).await()
                podcastManager.refreshPodcast(loadedPodcast, playbackManager)
                val latest = episodeManager.findEpisodesByPodcastOrderedByPublishDate(loadedPodcast).firstOrNull()
                if (latest != null) {
                    discoverFeedAnalytics.trackEpisodePlayed(row, latest.toTvDiscoverEpisode(podcast))
                    playbackManager.playNowSuspend(episode = latest, sourceView = SourceView.DISCOVER)
                    _playStarted.tryEmit(Unit)
                } else {
                    Timber.e("No episode found to play from featured podcast %s on TV home", podcast.uuid)
                    _playFailures.tryEmit(Unit)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to play latest episode from TV home")
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
        episode = this,
    )

    private fun PodcastEpisode.toTvDiscoverEpisode(podcast: TvDiscoverPodcast) = TvDiscoverEpisode(
        episodeUuid = uuid,
        episodeTitle = title,
        podcastUuid = podcast.uuid,
        podcastTitle = podcast.title,
    )

    companion object {
        private const val SOURCE_HOME = "home"

        const val KEEP_LISTENING_ROW_ID = "keep_listening"
        const val UP_NEXT_ROW_ID = "up_next"
        const val NEW_RELEASES_ROW_ID = "new_releases"

        private val LOCAL_ROW_IDS = setOf(KEEP_LISTENING_ROW_ID, UP_NEXT_ROW_ID, NEW_RELEASES_ROW_ID)

        private const val UP_NEXT_LIMIT = 12
        private const val UP_NEXT_ROW_MIN = 2
        private const val NEW_RELEASES_LIMIT = 12
        private const val LOCAL_ROWS_DEBOUNCE_MS = 1000L

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
