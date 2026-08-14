package au.com.shiftyjelly.pocketcasts.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.discover.TvCategoryPodcasts
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverBanner
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverEpisode
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverFeedLoader
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverPodcast
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.discover.TvOpenedCategory
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
import com.automattic.eventhorizon.BannerRowTappedEvent
import com.automattic.eventhorizon.DiscoverAdCategoryTappedEvent
import com.automattic.eventhorizon.DiscoverCategoriesPillTappedEvent
import com.automattic.eventhorizon.DiscoverFeaturedPodcastTappedEvent
import com.automattic.eventhorizon.DiscoverListEpisodePlayEvent
import com.automattic.eventhorizon.DiscoverListEpisodeTappedEvent
import com.automattic.eventhorizon.DiscoverListImpressionEvent
import com.automattic.eventhorizon.DiscoverListPodcastTappedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.HomeShownEvent
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
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
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

    suspend fun categoryPodcasts(categoryId: Int, source: String): TvCategoryPodcasts {
        return discoverFeedLoader.loadCategoryPodcasts(source, categoryId, syncManager.isLoggedIn())
    }

    fun trackHomeShown() {
        eventHorizon.track(HomeShownEvent)
    }

    fun trackDiscoverListShown(row: TvDiscoverRow) {
        val listId = row.discoverListId() ?: return
        eventHorizon.track(DiscoverListImpressionEvent(listId = listId, source = SOURCE_HOME))
    }

    fun trackDiscoverPodcastTapped(row: TvDiscoverRow, podcast: TvDiscoverPodcast) {
        val listId = row.discoverListId() ?: return
        eventHorizon.track(DiscoverListPodcastTappedEvent(listId = listId, podcastUuid = podcast.uuid, source = SOURCE_HOME))
        if (row is TvDiscoverRow.FeaturedPodcasts) {
            eventHorizon.track(DiscoverFeaturedPodcastTappedEvent(podcastUuid = podcast.uuid))
        } else if (podcast.isSponsored) {
            trackSponsoredPodcastTapped(podcast.uuid)
        }
    }

    fun trackDiscoverEpisodePlayed(row: TvDiscoverRow, episode: TvDiscoverEpisode) {
        val listId = row.discoverListId() ?: return
        eventHorizon.track(DiscoverListEpisodeTappedEvent(listId = listId, podcastUuid = episode.podcastUuid, episodeUuid = episode.episodeUuid, source = SOURCE_HOME))
        eventHorizon.track(DiscoverListEpisodePlayEvent(listId = listId, podcastUuid = episode.podcastUuid))
    }

    fun trackDiscoverEpisodePodcastTapped(row: TvDiscoverRow, episode: TvDiscoverEpisode) {
        val listId = row.discoverListId() ?: return
        eventHorizon.track(DiscoverListPodcastTappedEvent(listId = listId, podcastUuid = episode.podcastUuid, source = SOURCE_HOME))
    }

    fun trackCategoryPodcastTapped(category: TvOpenedCategory, listId: String?, podcast: TvDiscoverPodcast) {
        if (listId != null) {
            eventHorizon.track(DiscoverListPodcastTappedEvent(listId = listId, podcastUuid = podcast.uuid, source = SOURCE_HOME))
        }
        if (podcast.isSponsored) {
            eventHorizon.track(
                DiscoverAdCategoryTappedEvent(name = category.name, region = discoverRegion(), id = category.id.toLong(), podcastId = podcast.uuid),
            )
        }
    }

    fun trackCategoryPillTapped(category: DiscoverCategory, index: Int) {
        eventHorizon.track(
            DiscoverCategoriesPillTappedEvent(
                name = category.name,
                region = discoverRegion(),
                index = index.toLong(),
                visits = category.totalVisits.toLong(),
                sponsored = category.isSponsored ?: false,
                source = SOURCE_HOME,
            ),
        )
    }

    fun trackBannerTapped(banner: TvDiscoverBanner) {
        eventHorizon.track(BannerRowTappedEvent(type = banner.id))
    }

    private fun trackSponsoredPodcastTapped(podcastUuid: String) {
        eventHorizon.track(
            DiscoverAdCategoryTappedEvent(name = UNKNOWN_VALUE, region = discoverRegion(), id = 0, podcastId = podcastUuid),
        )
    }

    private fun discoverRegion(): String = settings.discoverCountryCode.value

    private fun TvDiscoverRow.discoverListId(): String? = when (this) {
        is TvDiscoverRow.FeaturedPodcasts,
        is TvDiscoverRow.SinglePodcast,
        is TvDiscoverRow.Podcasts,
        is TvDiscoverRow.Episodes,
        -> id.takeUnless { it in LOCAL_ROW_IDS }

        is TvDiscoverRow.Banner,
        is TvDiscoverRow.Categories,
        -> null
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
        private const val UNKNOWN_VALUE = "unknown"
        private const val SOURCE_HOME = "home"

        const val KEEP_LISTENING_ROW_ID = "keep_listening"
        const val UP_NEXT_ROW_ID = "up_next"
        const val NEW_RELEASES_ROW_ID = "new_releases"

        private val LOCAL_ROW_IDS = setOf(KEEP_LISTENING_ROW_ID, UP_NEXT_ROW_ID, NEW_RELEASES_ROW_ID)

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
