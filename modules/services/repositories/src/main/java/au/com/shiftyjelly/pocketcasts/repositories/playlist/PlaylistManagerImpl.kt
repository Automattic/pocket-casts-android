package au.com.shiftyjelly.pocketcasts.repositories.playlist

import androidx.room.withTransaction
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
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
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.SYNC_STATUS_SYNCED
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisodeMetadata
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PlaylistManagerImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val settings: Settings,
    private val clock: Clock,
) : PlaylistManager {
    private val playlistDao = appDatabase.playlistDao()

    override fun observePlaylistsPreview(): Flow<List<PlaylistPreview>> {
        return playlistDao
            .observePlaylists()
            .flatMapLatest { playlists ->
                if (playlists.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(playlists.toPreviewFlows()) { previewArray -> previewArray.toList() }
                }
            }
            .keepPodcastEpisodesSynced()
    }

    override fun observeSmartPlaylist(
        uuid: String,
        episodeSearchTerm: String?,
    ): Flow<SmartPlaylist?> {
        return playlistDao
            .observeSmartPlaylist(uuid)
            .flatMapLatest { playlist ->
                if (playlist == null) {
                    flowOf(null)
                } else {
                    val smartRules = playlist.smartRules
                    val podcastsFlow = playlistDao.observeSmartPlaylistPodcasts(
                        clock = clock,
                        smartRules = smartRules,
                        sortType = playlist.sortType,
                        limit = PLAYLIST_ARTWORK_EPISODE_LIMIT,
                    )
                    val episodesFlow = observeSmartEpisodes(smartRules, playlist.sortType, episodeSearchTerm)
                    val metadataFlow = playlistDao.observeSmartEpisodeMetadata(
                        clock = clock,
                        smartRules = smartRules,
                    )
                    combine(podcastsFlow, episodesFlow, metadataFlow) { podcasts, episodes, metadata ->
                        SmartPlaylist(
                            uuid = playlist.uuid,
                            title = playlist.title,
                            smartRules = smartRules,
                            episodes = episodes,
                            episodeSortType = playlist.sortType,
                            isAutoDownloadEnabled = playlist.autoDownload,
                            autoDownloadLimit = playlist.autodownloadLimit,
                            totalEpisodeCount = metadata.episodeCount,
                            playbackDurationLeft = metadata.timeLeftSeconds.seconds,
                            artworkPodcastUuids = podcasts,
                        )
                    }.keepPodcastEpisodesSynced()
                }
            }
            .distinctUntilChanged()
    }

    override fun observeManualPlaylist(
        uuid: String,
    ): Flow<ManualPlaylist?> {
        return playlistDao.observeManualPlaylist(uuid)
            .flatMapLatest { playlist ->
                if (playlist == null) {
                    flowOf(null)
                } else {
                    val podcastsFlow = playlistDao.observeManualPlaylistPodcasts(playlist.uuid)
                    val metadataFlow = playlistDao.observeManualEpisodeMetadata(playlist.uuid)
                    combine(podcastsFlow, metadataFlow) { podcasts, metadata ->
                        ManualPlaylist(
                            uuid = playlist.uuid,
                            title = playlist.title,
                            totalEpisodeCount = metadata.episodeCount,
                            playbackDurationLeft = metadata.timeLeftSeconds.seconds,
                            artworkPodcastUuids = podcasts,
                        )
                    }.keepPodcastEpisodesSynced()
                }
            }
            .distinctUntilChanged()
    }

    override fun observeSmartEpisodes(
        rules: SmartRules,
        sortType: PlaylistEpisodeSortType,
        searchTerm: String?,
    ): Flow<List<PodcastEpisode>> {
        return playlistDao.observeSmartPlaylistEpisodes(
            clock = clock,
            smartRules = rules,
            sortType = sortType,
            limit = SMART_PLAYLIST_EPISODE_LIMIT,
            searchTerm = searchTerm,
        ).distinctUntilChanged()
    }

    override fun observeEpisodeMetadata(rules: SmartRules): Flow<PlaylistEpisodeMetadata> {
        return playlistDao.observeSmartEpisodeMetadata(clock, rules)
    }

    override suspend fun updateSmartRules(uuid: String, rules: SmartRules) {
        appDatabase.withTransaction {
            val playlist = playlistDao
                .observeSmartPlaylist(uuid)
                .first()
                ?.applySmartRules(rules)
                ?.copy(syncStatus = SYNC_STATUS_NOT_SYNCED)
            if (playlist != null) {
                playlistDao.upsertPlaylist(playlist)
            }
        }
    }

    override suspend fun updateSortType(uuid: String, sortType: PlaylistEpisodeSortType) {
        playlistDao.updateSortType(uuid, sortType)
    }

    override suspend fun updateAutoDownload(uuid: String, isEnabled: Boolean) {
        playlistDao.updateAutoDownload(uuid, isEnabled)
    }

    override suspend fun updateAutoDownloadLimit(uuid: String, limit: Int) {
        playlistDao.updateAutoDownloadLimit(uuid, limit)
    }

    override suspend fun updateName(uuid: String, name: String) {
        playlistDao.updateName(uuid, name)
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

    override suspend fun createManualPlaylist(name: String): String {
        return createPlaylist(
            entity = PlaylistEntity(title = name, manual = true),
        )
    }

    private suspend fun createPlaylist(entity: PlaylistEntity, uuid: String? = null): String {
        return appDatabase.withTransaction {
            val uuids = playlistDao.getAllPlaylistUuids()
            val finalUuid = uuid?.takeIf { it !in uuids } ?: generateUniqueUuid(uuids)
            val finalEntity = entity.copy(uuid = finalUuid, sortPosition = 1)

            playlistDao.upsertPlaylist(finalEntity)
            uuids.forEachIndexed { index, uuid ->
                playlistDao.updateSortPosition(uuid, index + 2)
            }
            finalUuid
        }
    }

    override suspend fun updatePlaylistsOrder(sortedUuids: List<String>) {
        appDatabase.withTransaction {
            var missingPlaylistIndex = sortedUuids.size
            playlistDao.getSmartPlaylists().forEach { playlist ->
                val position = sortedUuids.indexOf(playlist.uuid).takeIf { it != -1 } ?: missingPlaylistIndex++
                playlistDao.updateSortPosition(playlist.uuid, position)
            }
        }
    }

    override suspend fun getManualPlaylistEpisodeSources(): List<ManualPlaylistEpisodeSource> {
        val isSubscriber = settings.cachedSubscription.value != null
        return playlistDao.getManualPlaylistEpisodeSources(useFolders = isSubscriber)
    }

    override fun observeManualPlaylistAvailableEpisodes(playlistUuid: String, podcastUuid: String): Flow<List<PodcastEpisode>> {
        return playlistDao
            .observeManualPlaylistAvailableEpisodes(playlistUuid, podcastUuid)
            .distinctUntilChanged()
    }

    private fun List<PlaylistEntity>.toPreviewFlows() = map { playlist ->
        val type = if (playlist.manual) {
            PlaylistPreview.Type.Manual
        } else {
            PlaylistPreview.Type.Smart
        }
        val (podcastsFlow, episodeMetadataFlow) = when (type) {
            PlaylistPreview.Type.Manual -> {
                val podcastsFlow = playlistDao.observeManualPlaylistPodcasts(playlist.uuid)
                val episodeMetadataFlow = playlistDao.observeManualEpisodeMetadata(playlist.uuid)
                podcastsFlow to episodeMetadataFlow
            }

            PlaylistPreview.Type.Smart -> {
                val podcastsFlow = playlistDao.observeSmartPlaylistPodcasts(
                    clock = clock,
                    smartRules = playlist.smartRules,
                    sortType = playlist.sortType,
                    limit = PLAYLIST_ARTWORK_EPISODE_LIMIT,
                )
                val episodeMetadataFlow = playlistDao.observeSmartEpisodeMetadata(
                    clock = clock,
                    smartRules = playlist.smartRules,
                )
                podcastsFlow to episodeMetadataFlow
            }
        }

        combine(podcastsFlow, episodeMetadataFlow) { podcasts, metadata ->
            PlaylistPreview(
                uuid = playlist.uuid,
                title = playlist.title,
                artworkPodcastUuids = podcasts,
                episodeCount = metadata.episodeCount,
                type = type,
            )
        }.distinctUntilChanged()
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
                PodcastsRule.Selected(podcastUuidList)
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
        // We use referential equality so only predefined playlists are synced by default
        syncStatus = if (this === SmartPlaylistDraft.NewReleases || this === SmartPlaylistDraft.InProgress) {
            SYNC_STATUS_SYNCED
        } else {
            SYNC_STATUS_NOT_SYNCED
        },
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

    private tailrec fun generateUniqueUuid(uuids: List<String>): String {
        val uuid = UUID.randomUUID().toString()
        return if (uuids.none { it.equals(uuid, ignoreCase = true) }) {
            uuid
        } else {
            generateUniqueUuid(uuids)
        }
    }

    private companion object {
        const val PLAYLIST_ARTWORK_EPISODE_LIMIT = 4
        const val SMART_PLAYLIST_EPISODE_LIMIT = 1000
    }
}

// Add a small debounce to synchronize updates between episode count and podcasts.
// When the database is updated, both flows emit events almost simultaneously.
// Without debouncing, this can briefly cause inconsistent data. For example, showing an incorrect count
// before the updated episodes are received. This is rather imperceptible to the user,
// but adding a short debounce helps avoid these inconsistencies and prevents redundant downstream emissions.
@OptIn(FlowPreview::class)
private fun <T> Flow<T>.keepPodcastEpisodesSynced() = debounce(50)
