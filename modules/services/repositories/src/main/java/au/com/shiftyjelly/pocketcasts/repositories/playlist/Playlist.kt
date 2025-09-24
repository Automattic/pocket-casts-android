package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import kotlin.time.Duration

sealed interface Playlist {
    val uuid: String
    val title: String
    val episodes: List<PlaylistEpisode>
    val settings: Settings
    val metadata: Metadata
    val type: Type

    data class Settings(
        val sortType: PlaylistEpisodeSortType,
        val isAutoDownloadEnabled: Boolean,
        val autoDownloadLimit: Int,
    ) {
        companion object {
            val ForPreview = Settings(
                sortType = PlaylistEpisodeSortType.DragAndDrop,
                isAutoDownloadEnabled = false,
                autoDownloadLimit = 10,
            )
        }
    }

    data class Metadata(
        val playbackDurationLeft: Duration,
        val artworkUuids: List<String>,
        val isShowingArchived: Boolean,
        val totalEpisodeCount: Int,
        val displayedEpisodeCount: Int,
        val displayedAvailableEpisodeCount: Int,
        val archivedEpisodeCount: Int,
    )

    enum class Type(
        val analyticsValue: String,
    ) {
        Manual(
            analyticsValue = "manual",
        ),
        Smart(
            analyticsValue = "smart",
        ),
        ;

        companion object {
            fun fromValue(value: String) = entries.firstOrNull { it.analyticsValue == value }
        }
    }

    companion object {
        const val NEW_RELEASES_UUID = "2797DCF8-1C93-4999-B52A-D1849736FA2C"
        const val IN_PROGRESS_UUID = "D89A925C-5CE1-41A4-A879-2751838CE5CE"

        val PREDEFINED_UUIDS = setOf(NEW_RELEASES_UUID, IN_PROGRESS_UUID)
    }
}

data class SmartPlaylist(
    override val uuid: String,
    override val title: String,
    val smartRules: SmartRules,
    override val episodes: List<PlaylistEpisode.Available>,
    override val settings: Playlist.Settings,
    override val metadata: Playlist.Metadata,
) : Playlist {
    override val type get() = Playlist.Type.Smart
}

data class ManualPlaylist(
    override val uuid: String,
    override val title: String,
    override val episodes: List<PlaylistEpisode>,
    override val settings: Playlist.Settings,
    override val metadata: Playlist.Metadata,
) : Playlist {
    override val type get() = Playlist.Type.Manual
}
