package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.db.dao.PlaylistDao
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import java.time.Clock
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PlaylistManagerImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val clock: Clock,
) : PlaylistManager {
    override fun observePlaylistsPreview(): Flow<List<PlaylistPreview>> {
        return playlistDao
            .observeSmartPlaylists()
            .flatMapLatest { playlists ->
                if (playlists.isEmpty()) {
                    flowOf(emptyList<PlaylistPreview>())
                } else {
                    combine(playlists.toPreviewFlows()) { previewArray -> previewArray.toList() }
                }
            }
            // Add a small debounce to synchronize updates between episode count and podcasts.
            // When the database is updated, both flows emit events almost simultaneously.
            // Without debouncing, this can briefly cause inconsistent data. For example, showing an inccorect count
            // before the updated episodes are received. This is rather imperceptible to the user,
            // but adding a short debounce helps avoid these inconsistencies and prevents redundant downstream emissions.
            .debounce(50.milliseconds)
    }

    override suspend fun deletePlaylist(uuid: String) {
        playlistDao.markPlaylistAsDeleted(uuid)
    }

    private fun List<SmartPlaylist>.toPreviewFlows() = map { playlist ->
        val podcastsFlow = playlistDao.observeSmartPlaylistPodcasts(
            clock = clock,
            smartRules = playlist.smartRules,
            sortType = playlist.sortType,
            limit = PLAYLIST_ARTWORK_EPISODE_LIMIT,
        )
        val episodeCountFlow = playlistDao.observeSmartPlaylistEpisodeCount(
            clock = clock,
            smartRules = playlist.smartRules,
        )
        combine(podcastsFlow, episodeCountFlow) { podcasts, count ->
            PlaylistPreview(
                uuid = playlist.uuid,
                title = playlist.title,
                podcasts = podcasts,
                episodeCount = count,
            )
        }.distinctUntilChanged()
    }

    private val SmartPlaylist.smartRules
        get() = SmartRules(
            episodeStatus = SmartRules.EpisodeStatusRule(
                unplayed = unplayed,
                inProgress = partiallyPlayed,
                completed = finished,
            ),
            downloadStatus = when {
                downloaded && notDownloaded -> SmartRules.DownloadStatusRule.Any
                downloaded -> SmartRules.DownloadStatusRule.Downloaded
                notDownloaded -> SmartRules.DownloadStatusRule.NotDownloaded
                else -> SmartRules.DownloadStatusRule.Any
            },
            mediaType = when (audioVideo) {
                SmartPlaylist.AUDIO_VIDEO_FILTER_AUDIO_ONLY -> SmartRules.MediaTypeRule.Audio
                SmartPlaylist.AUDIO_VIDEO_FILTER_VIDEO_ONLY -> SmartRules.MediaTypeRule.Video
                else -> SmartRules.MediaTypeRule.Any
            },
            releaseDate = when (filterHours) {
                SmartPlaylist.LAST_24_HOURS -> SmartRules.ReleaseDateRule.Last24Hours
                SmartPlaylist.LAST_3_DAYS -> SmartRules.ReleaseDateRule.Last3Days
                SmartPlaylist.LAST_WEEK -> SmartRules.ReleaseDateRule.LastWeek
                SmartPlaylist.LAST_2_WEEKS -> SmartRules.ReleaseDateRule.Last2Weeks
                SmartPlaylist.LAST_MONTH -> SmartRules.ReleaseDateRule.LastMonth
                else -> SmartRules.ReleaseDateRule.AnyTime
            },
            starred = if (starred) {
                SmartRules.StarredRule.Starred
            } else {
                SmartRules.StarredRule.Any
            },
            podcastsRule = if (podcastUuidList.isEmpty()) {
                SmartRules.PodcastsRule.Any
            } else {
                SmartRules.PodcastsRule.Selected(podcastUuidList)
            },
            episodeDuration = if (filterDuration) {
                SmartRules.EpisodeDurationRule.Constrained(
                    longerThan = longerThan.minutes,
                    shorterThan = shorterThan.minutes + 59.seconds,
                )
            } else {
                SmartRules.EpisodeDurationRule.Any
            },
        )

    private companion object {
        const val PLAYLIST_ARTWORK_EPISODE_LIMIT = 4
    }
}
