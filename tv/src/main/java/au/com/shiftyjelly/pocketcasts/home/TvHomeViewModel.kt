package au.com.shiftyjelly.pocketcasts.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextDao
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class TvHomeViewModel @Inject constructor(
    private val listRepository: ListRepository,
    private val playlistManager: PlaylistManager,
    private val podcastDao: PodcastDao,
    private val upNextDao: UpNextDao,
    private val settings: Settings,
    private val syncManager: SyncManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TvHomeUiState>(TvHomeUiState.Loading)
    val uiState: StateFlow<TvHomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
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

    private suspend fun loadRows(): List<TvHomeRow> = coroutineScope {
        val isLoggedIn = syncManager.isLoggedIn()
        val localRows = async { loadLocalRows(isLoggedIn) }
        val discoverRows = async { loadDiscoverRows(isLoggedIn) }
        (localRows.await() + discoverRows.await()).distinctBy(TvHomeRow::id)
    }

    private suspend fun loadDiscoverRows(isLoggedIn: Boolean): List<TvHomeRow> = coroutineScope {
        val discover = listRepository.getDiscoverFeed()
        val region = discover.regions[settings.discoverCountryCode.value]
            ?: discover.regions[discover.defaultRegionCode]
            ?: error("Could not resolve discover region")
        val replacements = mapOf(
            discover.regionCodeToken to region.code,
            discover.regionNameToken to region.name,
        )

        discover.layout
            .transformWithRegion(region, replacements, context.resources)
            .filter { isLoggedIn || it.authenticated != true }
            .map { row -> async { loadRow(row) } }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun loadLocalRows(isLoggedIn: Boolean): List<TvHomeRow> = coroutineScope {
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
                    TvHomeRow.Episodes(
                        id = KEEP_LISTENING_ROW_ID,
                        title = context.getString(LR.string.tv_home_keep_listening),
                        episodes = listOf(current.toTvHomeEpisode(podcastTitles)),
                    ),
                )
            }
            if (isLoggedIn) {
                val queue = upNextEpisodes.drop(1)
                if (queue.isNotEmpty()) {
                    add(
                        TvHomeRow.Episodes(
                            id = UP_NEXT_ROW_ID,
                            title = context.getString(LR.string.up_next),
                            episodes = queue.map { it.toTvHomeEpisode(podcastTitles) },
                        ),
                    )
                }
                if (newReleases.isNotEmpty()) {
                    add(
                        TvHomeRow.Episodes(
                            id = NEW_RELEASES_ROW_ID,
                            title = context.getString(LR.string.filters_title_new_releases),
                            episodes = newReleases.map { it.toTvHomeEpisode(podcastTitles) },
                        ),
                    )
                }
            }
        }
    }

    private suspend fun findPodcastTitles(episodes: List<PodcastEpisode>): Map<String, String> {
        val uuids = episodes.map(PodcastEpisode::podcastUuid).distinct()
        if (uuids.isEmpty()) return emptyMap()
        return podcastDao.findAllIn(uuids).associate { it.uuid to it.title }
    }

    private fun PodcastEpisode.toTvHomeEpisode(podcastTitles: Map<String, String>) = TvHomeEpisode(
        episodeUuid = uuid,
        episodeTitle = title,
        podcastUuid = podcastUuid,
        podcastTitle = podcastTitles[podcastUuid].orEmpty(),
    )

    private suspend fun loadRow(row: DiscoverRow): TvHomeRow? {
        return when (row.type) {
            is ListType.PodcastList -> loadPodcastsRow(row)
            is ListType.EpisodeList -> loadEpisodesRow(row)
            is ListType.Categories, is ListType.Unknown -> null
        }
    }

    private suspend fun loadPodcastsRow(row: DiscoverRow): TvHomeRow? {
        val feed = listRepository.getListFeed(row.source, row.authenticated) ?: return null
        val podcasts = feed.podcasts.orEmpty()
            .distinctBy(DiscoverPodcast::uuid)
            .map { it.toTvHomePodcast(isSponsored = row.sponsored) }
        if (podcasts.isEmpty()) return null
        val title = feed.title?.takeIf { it.isNotBlank() } ?: row.title
        return when (row.displayStyle) {
            is DisplayStyle.Carousel, is DisplayStyle.SinglePodcast -> {
                TvHomeRow.FeaturedPodcasts(id = row.rowId(), title = title, podcasts = podcasts)
            }

            else -> TvHomeRow.Podcasts(id = row.rowId(), title = title, podcasts = podcasts)
        }
    }

    private suspend fun loadEpisodesRow(row: DiscoverRow): TvHomeRow? {
        val feed = listRepository.getListFeed(row.source, row.authenticated) ?: return null
        val episodes = feed.episodes.orEmpty()
            .distinctBy(DiscoverEpisode::uuid)
            .map { episode ->
                TvHomeEpisode(
                    episodeUuid = episode.uuid,
                    episodeTitle = episode.title.orEmpty(),
                    podcastUuid = episode.podcast_uuid,
                    podcastTitle = episode.podcast_title.orEmpty(),
                )
            }
        if (episodes.isEmpty()) return null
        val title = feed.title?.takeIf { it.isNotBlank() } ?: row.title
        return TvHomeRow.Episodes(id = row.rowId(), title = title, episodes = episodes)
    }

    private fun DiscoverRow.rowId() = listUuid ?: id ?: title

    private fun DiscoverPodcast.toTvHomePodcast(isSponsored: Boolean) = TvHomePodcast(
        uuid = uuid,
        title = title.orEmpty(),
        description = description.orEmpty(),
        isSponsored = isSponsored,
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
    data class Ready(val rows: List<TvHomeRow>) : TvHomeUiState
}

sealed interface TvHomeRow {
    val id: String
    val title: String

    data class FeaturedPodcasts(
        override val id: String,
        override val title: String,
        val podcasts: List<TvHomePodcast>,
    ) : TvHomeRow

    data class Podcasts(
        override val id: String,
        override val title: String,
        val podcasts: List<TvHomePodcast>,
    ) : TvHomeRow

    data class Episodes(
        override val id: String,
        override val title: String,
        val episodes: List<TvHomeEpisode>,
    ) : TvHomeRow
}

data class TvHomePodcast(
    val uuid: String,
    val title: String,
    val description: String,
    val isSponsored: Boolean = false,
) {
    val artworkUrl: String = PodcastImage.getMediumArtworkUrl(uuid)
}

data class TvHomeEpisode(
    val episodeUuid: String,
    val episodeTitle: String,
    val podcastUuid: String,
    val podcastTitle: String,
) {
    val thumbnailUrl: String = PodcastImage.getArtworkUrl(size = 960, uuid = podcastUuid, isWearOS = false)
    val podcastArtworkUrl: String = PodcastImage.getArtworkUrl(size = 200, uuid = podcastUuid, isWearOS = false)
}
