package au.com.shiftyjelly.pocketcasts.repositories.playlist

import androidx.room.withTransaction
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistPodcastSource
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.ANYTIME
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.AUDIO_VIDEO_FILTER_ALL
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.AUDIO_VIDEO_FILTER_AUDIO_ONLY
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.AUDIO_VIDEO_FILTER_VIDEO_ONLY
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_24_HOURS
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_2_WEEKS
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_3_DAYS
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_MONTH
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_WEEK
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.SYNC_STATUS_NOT_SYNCED
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisodeMetadata
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistPreviewForEpisodeEntity
import au.com.shiftyjelly.pocketcasts.models.to.toPodcastEpisodes
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager.Companion.MANUAL_PLAYLIST_EPISODE_LIMIT
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager.Companion.PLAYLIST_ARTWORK_EPISODE_LIMIT
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager.Companion.SMART_PLAYLIST_EPISODE_LIMIT
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PlaylistManagerImpl(
    private val appDatabase: AppDatabase,
    private val settings: Settings,
    private val clock: Clock,
    private val smartEpisodeLimit: Int,
    private val manualEpisodeLimit: Int,
) : PlaylistManager {
    @Inject
    constructor(
        appDatabase: AppDatabase,
        settings: Settings,
        clock: Clock,
    ) : this(
        appDatabase = appDatabase,
        settings = settings,
        clock = clock,
        smartEpisodeLimit = SMART_PLAYLIST_EPISODE_LIMIT,
        manualEpisodeLimit = MANUAL_PLAYLIST_EPISODE_LIMIT,
    )

    private val playlistDao = appDatabase.playlistDao()
    private val episodeDao = appDatabase.episodeDao()
    private val podcastDao = appDatabase.podcastDao()

    override fun playlistPreviewsFlow(): Flow<List<PlaylistPreview>> {
        return playlistDao
            .allPlaylistsFlow()
            .flatMapLatest { playlists ->
                if (playlists.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    createPreviewsFlow(playlists)
                }
            }
            .keepPodcastEpisodesSynced()
    }

    override suspend fun getAutoDownloadEpisodes(): List<PodcastEpisode> {
        return appDatabase.withTransaction {
            val playlists = playlistDao.getAllAutoDownloadPlaylists()
            withContext(Dispatchers.Default) {
                val useManual = FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)
                playlists
                    .let { playlists ->
                        if (useManual) {
                            playlists
                        } else {
                            playlists.filterNot(PlaylistEntity::manual)
                        }
                    }
                    .flatMap { playlist ->
                        val playlistFlow = if (playlist.manual) {
                            manualPlaylistFlow(playlist.uuid)
                        } else {
                            smartPlaylistFlow(playlist.uuid)
                        }
                        playlistFlow.first()
                            ?.episodes
                            ?.toPodcastEpisodes()
                            ?.take(playlist.autodownloadLimit)
                            .orEmpty()
                    }
                    .distinctBy(PodcastEpisode::uuid)
            }
        }
    }

    override suspend fun sortPlaylists(sortedUuids: List<String>) {
        appDatabase.withTransaction {
            var missingPlaylistIndex = sortedUuids.size
            playlistDao.getAllPlaylistUuids().forEach { playlistUuid ->
                val position = sortedUuids.indexOf(playlistUuid).takeIf { it != -1 } ?: missingPlaylistIndex++
                playlistDao.updateSortPosition(playlistUuid, position)
            }
        }
    }

    override suspend fun updateName(uuid: String, name: String) {
        playlistDao.updateName(uuid, name)
    }

    override suspend fun updateSortType(uuid: String, type: PlaylistEpisodeSortType) {
        playlistDao.updateSortType(uuid, type)
    }

    override suspend fun updateAutoDownload(uuids: Collection<String>, isEnabled: Boolean) {
        playlistDao.updateAutoDownload(uuids, isEnabled)
    }

    override suspend fun updateAutoDownloadLimit(uuid: String, limit: Int) {
        playlistDao.updateAutoDownloadLimit(uuid, limit)
    }

    override suspend fun toggleShowArchived(uuid: String) {
        playlistDao.toggleIsShowingArchived(uuid)
    }

    override suspend fun deletePlaylist(uuid: String) {
        playlistDao.markPlaylistAsDeleted(uuid)
    }

    override suspend fun createSmartPlaylist(draft: SmartPlaylistDraft): String {
        return createPlaylist(
            entity = draft.toPlaylistEntity(),
            uuid = if (draft === SmartPlaylistDraft.NewReleases) {
                Playlist.NEW_RELEASES_UUID
            } else if (draft === SmartPlaylistDraft.InProgress) {
                Playlist.IN_PROGRESS_UUID
            } else {
                null
            },
        )
    }

    override fun smartPlaylistFlow(uuid: String, searchTerm: String?): Flow<SmartPlaylist?> {
        return playlistDao
            .smartPlaylistFlow(uuid)
            .flatMapLatest { playlist ->
                if (playlist == null) {
                    flowOf(null)
                } else {
                    val smartRules = playlist.smartRules
                    val podcastsFlow = smartPlaylistArtworkPodcastsFlow(playlist)
                    val episodesFlow = smartEpisodesFlow(smartRules, playlist.sortType, searchTerm)
                    val metadataFlow = playlistDao.smartPlaylistMetadataFlow(clock, smartRules)

                    combine(podcastsFlow, episodesFlow, metadataFlow) { podcasts, episodes, metadata ->
                        SmartPlaylist(
                            uuid = playlist.uuid,
                            title = playlist.title,
                            smartRules = smartRules,
                            episodes = episodes,
                            settings = Playlist.Settings(
                                sortType = playlist.sortType,
                                isAutoDownloadEnabled = playlist.autoDownload,
                                autoDownloadLimit = playlist.autodownloadLimit,
                            ),
                            metadata = Playlist.Metadata(
                                playbackDurationLeft = metadata.timeLeftSeconds.seconds,
                                artworkUuids = podcasts,
                                isShowingArchived = false,
                                totalEpisodeCount = metadata.episodeCount,
                                archivedEpisodeCount = metadata.archivedEpisodeCount,
                                displayedEpisodeCount = episodes.size,
                                displayedAvailableEpisodeCount = episodes.size,
                            ),
                        )
                    }
                }
            }
            .keepPodcastEpisodesSynced()
            .distinctUntilChanged()
    }

    override fun smartEpisodesFlow(rules: SmartRules, sortType: PlaylistEpisodeSortType, searchTerm: String?): Flow<List<PlaylistEpisode.Available>> {
        return playlistDao
            .smartEpisodesFlow(
                clock = clock,
                smartRules = rules,
                sortType = sortType,
                limit = smartEpisodeLimit,
                searchTerm = searchTerm,
            )
            .distinctUntilChanged()
    }

    override fun smartEpisodesMetadataFlow(rules: SmartRules): Flow<PlaylistEpisodeMetadata> {
        return playlistDao.smartPlaylistMetadataFlow(clock, rules)
    }

    override suspend fun updateSmartRules(uuidToRulesMap: Map<String, SmartRules>) {
        appDatabase.withTransaction {
            val playlists = playlistDao.getAllPlaylistsIn(uuidToRulesMap.keys)
                .map { playlist ->
                    val rules = uuidToRulesMap[playlist.uuid] ?: playlist.smartRules
                    playlist.applySmartRules(rules).copy(syncStatus = SYNC_STATUS_NOT_SYNCED)
                }
            if (playlists.isNotEmpty()) {
                playlistDao.upsertAllPlaylists(playlists)
            }
        }
    }

    override suspend fun createManualPlaylist(name: String): String {
        return createPlaylist(
            entity = PlaylistEntity(
                title = name,
                manual = true,
                sortType = PlaylistEpisodeSortType.DragAndDrop,
                syncStatus = SYNC_STATUS_NOT_SYNCED,
            ),
        )
    }

    override fun manualPlaylistFlow(uuid: String, searchTerm: String?): Flow<ManualPlaylist?> {
        return playlistDao.manualPlaylistFlow(uuid)
            .flatMapLatest { playlist ->
                if (playlist == null) {
                    flowOf(null)
                } else {
                    val podcastsFlow = manualPlaylistArtworkPodcastsFlow(playlist.uuid)
                    val episodesFlow = playlistDao.manualEpisodesFlow(playlist.uuid, searchTerm.orEmpty())
                    val metadataFlow = playlistDao.manualPlaylistMetadataFlow(playlist.uuid)

                    combine(podcastsFlow, episodesFlow, metadataFlow) { podcasts, episodes, metadata ->
                        ManualPlaylist(
                            uuid = playlist.uuid,
                            title = playlist.title,
                            episodes = episodes,
                            settings = Playlist.Settings(
                                sortType = playlist.sortType,
                                isAutoDownloadEnabled = playlist.autoDownload,
                                autoDownloadLimit = playlist.autodownloadLimit,
                            ),
                            metadata = Playlist.Metadata(
                                playbackDurationLeft = metadata.timeLeftSeconds.seconds,
                                artworkUuids = podcasts,
                                isShowingArchived = playlist.showArchivedEpisodes,
                                totalEpisodeCount = metadata.episodeCount,
                                archivedEpisodeCount = metadata.archivedEpisodeCount,
                                displayedEpisodeCount = episodes.size,
                                displayedAvailableEpisodeCount = episodes.count { it is PlaylistEpisode.Available },
                            ),
                        )
                    }
                }
            }
            .keepPodcastEpisodesSynced()
            .distinctUntilChanged()
    }

    override fun playlistPreviewsForEpisodeFlow(episodeUuid: String, searchTerm: String?): Flow<List<PlaylistPreviewForEpisode>> {
        return playlistDao
            .playlistPreviewsForEpisodeFlow(episodeUuid, searchTerm.orEmpty())
            .flatMapLatest { playlists ->
                if (playlists.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    createPreviewsForEpisode(playlists)
                }
            }
            .keepPodcastEpisodesSynced()
    }

    override suspend fun getManualEpisodeSources(searchTerm: String?): List<ManualPlaylistEpisodeSource> {
        val isSubscriber = settings.cachedSubscription.value != null
        return playlistDao.getManualPlaylistEpisodeSources(
            useFolders = isSubscriber,
            searchTerm = searchTerm.orEmpty(),
        )
    }

    override suspend fun getManualEpisodeSourcesForFolder(folderUuid: String, searchTerm: String?): List<ManualPlaylistPodcastSource> {
        return playlistDao.getPodcastPlaylistSourcesForFolder(
            folderUuid = folderUuid,
            searchTerm = searchTerm.orEmpty(),
        )
    }

    override fun notAddedManualEpisodesFlow(playlistUuid: String, podcastUuid: String, searchTerm: String?): Flow<List<PodcastEpisode>> {
        return playlistDao
            .notAddedManualEpisodesFlow(
                playlistUuid = playlistUuid,
                podcastUuid = podcastUuid,
                searchTerm = searchTerm.orEmpty(),
            )
            .distinctUntilChanged()
    }

    override suspend fun addManualEpisode(playlistUuid: String, episodeUuid: String): Boolean {
        return appDatabase.withTransaction {
            val episodes = playlistDao.getManualPlaylistEpisodes(playlistUuid)
            val episodeUuids = episodes.map(ManualPlaylistEpisode::episodeUuid)
            if (episodeUuid in episodeUuids) {
                return@withTransaction true
            }

            if (episodeUuids.size >= manualEpisodeLimit) {
                return@withTransaction false
            }

            val podcastEpisode = episodeDao.findByUuid(episodeUuid)
            if (podcastEpisode == null) {
                return@withTransaction false
            }

            val podcast = podcastDao.findPodcastByUuid(podcastEpisode.podcastUuid)
            val newEpisode = ManualPlaylistEpisode(
                playlistUuid = playlistUuid,
                episodeUuid = episodeUuid,
                podcastUuid = podcastEpisode.podcastUuid,
                title = podcastEpisode.title,
                addedAt = clock.instant(),
                publishedAt = podcastEpisode.publishedDate.toInstant(),
                downloadUrl = podcastEpisode.downloadUrl,
                episodeSlug = podcastEpisode.slug,
                podcastSlug = podcast?.slug.orEmpty(),
                sortPosition = episodes.lastOrNull()?.sortPosition?.plus(1) ?: 0,
                isSynced = false,
            )
            playlistDao.upsertManualEpisode(newEpisode)
            playlistDao.markPlaylistAsNotSynced(playlistUuid)
            true
        }
    }

    override suspend fun sortManualEpisodes(playlistUuid: String, episodeUuids: List<String>) {
        appDatabase.withTransaction {
            var missingEpisodeIndex = episodeUuids.size
            val episodes = playlistDao.getManualPlaylistEpisodes(playlistUuid).map { episode ->
                val newPosition = episodeUuids.indexOf(episode.episodeUuid).takeIf { it != -1 } ?: missingEpisodeIndex++
                episode.copy(sortPosition = newPosition)
            }
            playlistDao.upsertManualEpisodes(episodes)
            playlistDao.updateSortType(playlistUuid, PlaylistEpisodeSortType.DragAndDrop)
            playlistDao.markPlaylistAsNotSynced(playlistUuid)
        }
    }

    override suspend fun deleteManualEpisodes(playlistUuid: String, episodeUuids: Collection<String>) {
        appDatabase.withTransaction {
            playlistDao.deleteAllManualEpisodesIn(playlistUuid, episodeUuids)
            playlistDao.markPlaylistAsNotSynced(playlistUuid)
        }
    }

    private fun createPreviewsFlow(playlists: List<PlaylistEntity>) = combine(
        playlists.map { playlist ->
            val flow = if (playlist.manual) {
                manualPlaylistPreviewsFlow(playlist)
            } else {
                smartPlaylistPreviewsFlow(playlist)
            }
            flow.distinctUntilChanged()
        },
    ) { array -> array.toList() }

    private fun createPreviewsForEpisode(playlists: List<PlaylistPreviewForEpisodeEntity>) = combine(
        playlists.map { playlist ->
            manualPlaylistArtworkPodcastsFlow(playlist.uuid)
                .map { podcasts ->
                    PlaylistPreviewForEpisode(
                        uuid = playlist.uuid,
                        title = playlist.title,
                        episodeCount = playlist.episodeCount,
                        artworkPodcastUuids = podcasts,
                        hasEpisode = playlist.hasEpisode,
                        episodeLimit = manualEpisodeLimit,
                    )
                }
                .distinctUntilChanged()
        },
    ) { array -> array.toList() }

    private fun manualPlaylistPreviewsFlow(playlist: PlaylistEntity): Flow<PlaylistPreview> {
        val podcastsFlow = manualPlaylistArtworkPodcastsFlow(playlist.uuid)
        val episodeMetadataFlow = playlistDao.manualPlaylistMetadataFlow(playlist.uuid)
        return combine(podcastsFlow, episodeMetadataFlow) { podcasts, metadata ->
            ManualPlaylistPreview(
                uuid = playlist.uuid,
                title = playlist.title,
                artworkPodcastUuids = podcasts,
                episodeCount = metadata.episodeCount,
                settings = Playlist.Settings(
                    sortType = playlist.sortType,
                    isAutoDownloadEnabled = playlist.autoDownload,
                    autoDownloadLimit = playlist.autodownloadLimit,
                ),
                icon = playlist.icon,
            )
        }
    }

    private fun smartPlaylistPreviewsFlow(playlist: PlaylistEntity): Flow<PlaylistPreview> {
        val podcastsFlow = smartPlaylistArtworkPodcastsFlow(playlist)
        val episodeMetadataFlow = playlistDao.smartPlaylistMetadataFlow(clock, playlist.smartRules)
        val smartRules = playlist.smartRules
        return combine(podcastsFlow, episodeMetadataFlow) { podcasts, metadata ->
            SmartPlaylistPreview(
                uuid = playlist.uuid,
                title = playlist.title,
                artworkPodcastUuids = podcasts,
                episodeCount = metadata.episodeCount,
                settings = Playlist.Settings(
                    sortType = playlist.sortType,
                    isAutoDownloadEnabled = playlist.autoDownload,
                    autoDownloadLimit = playlist.autodownloadLimit,
                ),
                smartRules = smartRules,
                icon = playlist.icon,
            )
        }
    }

    private fun manualPlaylistArtworkPodcastsFlow(playlistUuid: String) = playlistDao
        .manualPlaylistArtworkPodcastsFlow(playlistUuid)
        .map { uuids -> uuids.toArtworkUuids() }

    private fun smartPlaylistArtworkPodcastsFlow(playlist: PlaylistEntity) = playlistDao
        .smartPlaylistArtworkPodcastsFlow(
            clock = clock,
            smartRules = playlist.smartRules,
            sortType = playlist.sortType,
            limit = smartEpisodeLimit,
        )
        .map { uuids -> uuids.toArtworkUuids() }

    private suspend fun createPlaylist(entity: PlaylistEntity, uuid: String? = null): String {
        return appDatabase.withTransaction {
            val uuids = playlistDao.getAllPlaylistUuids()
            val finalUuid = uuid?.takeIf { it !in uuids } ?: generateUniqueUuid(uuids)
            val finalEntity = entity.copy(uuid = finalUuid, sortPosition = 0)

            playlistDao.upsertPlaylist(finalEntity)
            uuids.forEachIndexed { index, uuid ->
                playlistDao.updateSortPosition(uuid, index + 1)
            }
            finalUuid
        }
    }
}

private tailrec fun generateUniqueUuid(uuids: List<String>): String {
    val uuid = UUID.randomUUID().toString()
    return if (uuids.none { it.equals(uuid, ignoreCase = true) }) {
        uuid
    } else {
        generateUniqueUuid(uuids)
    }
}

private val PlaylistEntity.smartRules
    get() = SmartRules(
        episodeStatus = SmartRules.EpisodeStatusRule(
            unplayed = unplayed,
            inProgress = partiallyPlayed,
            completed = finished,
        ),
        downloadStatus = when {
            downloaded && notDownloaded -> DownloadStatusRule.Any
            downloaded -> DownloadStatusRule.Downloaded
            notDownloaded -> DownloadStatusRule.NotDownloaded
            else -> DownloadStatusRule.Any
        },
        mediaType = when (audioVideo) {
            AUDIO_VIDEO_FILTER_AUDIO_ONLY -> MediaTypeRule.Audio
            AUDIO_VIDEO_FILTER_VIDEO_ONLY -> MediaTypeRule.Video
            else -> MediaTypeRule.Any
        },
        releaseDate = when (filterHours) {
            LAST_24_HOURS -> ReleaseDateRule.Last24Hours
            LAST_3_DAYS -> ReleaseDateRule.Last3Days
            LAST_WEEK -> ReleaseDateRule.LastWeek
            LAST_2_WEEKS -> ReleaseDateRule.Last2Weeks
            LAST_MONTH -> ReleaseDateRule.LastMonth
            else -> ReleaseDateRule.AnyTime
        },
        starred = if (starred) {
            StarredRule.Starred
        } else {
            StarredRule.Any
        },
        podcasts = if (podcastUuidList.isEmpty()) {
            PodcastsRule.Any
        } else {
            PodcastsRule.Selected(podcastUuidList.toSet())
        },
        episodeDuration = if (filterDuration) {
            EpisodeDurationRule.Constrained(
                longerThan = longerThan.minutes,
                shorterThan = shorterThan.minutes + 59.seconds,
            )
        } else {
            EpisodeDurationRule.Any
        },
    )

private fun SmartPlaylistDraft.toPlaylistEntity() = PlaylistEntity(
    uuid = "",
    title = title,
    // We use referential equality so only predefined playlists use preset icons
    iconId = if (this === SmartPlaylistDraft.NewReleases) {
        10 // Red clock
    } else if (this === SmartPlaylistDraft.InProgress) {
        23 // Purple play
    } else {
        0
    },
    sortPosition = 1,
    manual = false,
    draft = false,
    deleted = false,
    syncStatus = SYNC_STATUS_NOT_SYNCED,
).applySmartRules(rules)

private fun PlaylistEntity.applySmartRules(rules: SmartRules) = copy(
    unplayed = rules.episodeStatus.unplayed,
    partiallyPlayed = rules.episodeStatus.inProgress,
    finished = rules.episodeStatus.completed,
    downloaded = rules.downloadStatus in listOf(DownloadStatusRule.Downloaded, DownloadStatusRule.Any),
    notDownloaded = rules.downloadStatus in listOf(DownloadStatusRule.NotDownloaded, DownloadStatusRule.Any),
    audioVideo = when (rules.mediaType) {
        MediaTypeRule.Any -> AUDIO_VIDEO_FILTER_ALL
        MediaTypeRule.Audio -> AUDIO_VIDEO_FILTER_AUDIO_ONLY
        MediaTypeRule.Video -> AUDIO_VIDEO_FILTER_VIDEO_ONLY
    },
    filterHours = when (rules.releaseDate) {
        ReleaseDateRule.AnyTime -> ANYTIME
        ReleaseDateRule.Last24Hours -> LAST_24_HOURS
        ReleaseDateRule.Last3Days -> LAST_3_DAYS
        ReleaseDateRule.LastWeek -> LAST_WEEK
        ReleaseDateRule.Last2Weeks -> LAST_2_WEEKS
        ReleaseDateRule.LastMonth -> LAST_MONTH
    },
    starred = when (rules.starred) {
        StarredRule.Any -> false
        StarredRule.Starred -> true
    },
    allPodcasts = when (rules.podcasts) {
        is PodcastsRule.Any -> true
        is PodcastsRule.Selected -> false
    },
    podcastUuids = when (val rule = rules.podcasts) {
        is PodcastsRule.Any -> null
        is PodcastsRule.Selected -> rule.uuids.joinToString(separator = ",")
    },
    filterDuration = when (rules.episodeDuration) {
        is EpisodeDurationRule.Any -> false
        is EpisodeDurationRule.Constrained -> true
    },
    longerThan = when (val rule = rules.episodeDuration) {
        is EpisodeDurationRule.Any -> 20
        is EpisodeDurationRule.Constrained -> rule.longerThan.inWholeMinutes.toInt()
    },
    shorterThan = when (val rule = rules.episodeDuration) {
        is EpisodeDurationRule.Any -> 40
        is EpisodeDurationRule.Constrained -> rule.shorterThan.inWholeMinutes.toInt()
    },
)

private fun List<String>.toArtworkUuids(): List<String> {
    return if (!isEmpty()) {
        val distinctUuids = LinkedHashSet<String>(PLAYLIST_ARTWORK_EPISODE_LIMIT)
        for (uuid in this) {
            distinctUuids += uuid
            if (distinctUuids.size == PLAYLIST_ARTWORK_EPISODE_LIMIT) {
                break
            }
        }
        return distinctUuids.toList()
    } else {
        this
    }
}

// Add a small debounce to synchronize updates between episode count and podcasts.
// When the database is updated, both flows emit events almost simultaneously.
// Without debouncing, this can briefly cause inconsistent data. For example, showing an incorrect count
// before the updated episodes are received. This is rather imperceptible to the user,
// but adding a short debounce helps avoid these inconsistencies and prevents redundant downstream emissions.
@OptIn(FlowPreview::class)
private fun <T> Flow<T>.keepPodcastEpisodesSynced() = debounce(50)
