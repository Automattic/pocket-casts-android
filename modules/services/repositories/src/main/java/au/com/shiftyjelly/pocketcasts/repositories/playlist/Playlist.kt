package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.ManualEpisode
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import kotlin.time.Duration

sealed interface Playlist {
    val uuid: String
    val title: String
    val settings: Settings
    val metadata: Metadata

    data class Settings(
        val sortType: PlaylistEpisodeSortType,
        val isAutoDownloadEnabled: Boolean,
        val autoDownloadLimit: Int,
    )

    data class Metadata(
        val playbackDurationLeft: Duration,
        val artworkUuids: List<String>,
        val totalEpisodeCount: Int,
        val displayedEpisodeCount: Int,
        val displayedAvailableEpisodeCount: Int,
    )

    enum class Type {
        Manual,
        Smart,
    }

    companion object {
        const val NEW_RELEASES_UUID = "2797DCF8-1C93-4999-B52A-D1849736FA2C"
        const val IN_PROGRESS_UUID = "D89A925C-5CE1-41A4-A879-2751838CE5CE"
    }
}

data class SmartPlaylist(
    override val uuid: String,
    override val title: String,
    override val settings: Playlist.Settings,
    override val metadata: Playlist.Metadata,
    val smartRules: SmartRules,
    val episodes: List<PodcastEpisode>,
) : Playlist

data class ManualPlaylist(
    override val uuid: String,
    override val title: String,
    override val settings: Playlist.Settings,
    override val metadata: Playlist.Metadata,
    val episodes: List<ManualEpisode>,
) : Playlist
