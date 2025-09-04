package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.ManualEpisode
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import kotlin.time.Duration

sealed interface Playlist {
    companion object {
        const val NEW_RELEASES_UUID = "2797DCF8-1C93-4999-B52A-D1849736FA2C"
        const val IN_PROGRESS_UUID = "D89A925C-5CE1-41A4-A879-2751838CE5CE"
    }
}

data class SmartPlaylist(
    val uuid: String,
    val title: String,
    val smartRules: SmartRules,
    val episodes: List<PodcastEpisode>,
    val episodeSortType: PlaylistEpisodeSortType,
    val isAutoDownloadEnabled: Boolean,
    val autoDownloadLimit: Int,
    val totalEpisodeCount: Int,
    val playbackDurationLeft: Duration,
    val artworkPodcastUuids: List<String>,
) : Playlist

data class ManualPlaylist(
    val uuid: String,
    val title: String,
    val episodes: List<ManualEpisode>,
    val totalEpisodeCount: Int,
    val playbackDurationLeft: Duration,
    val artworkPodcastUuids: List<String>,
) : Playlist
