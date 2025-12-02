package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.utils.extensions.splitIgnoreEmpty
import au.com.shiftyjelly.pocketcasts.utils.extensions.unidecode
import java.io.Serializable

@Entity(
    tableName = "playlists",
)
data class PlaylistEntity(
    @PrimaryKey @ColumnInfo(name = "uuid") var uuid: String,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "iconId") var iconId: Int = 0,
    @ColumnInfo(name = "sortPosition") var sortPosition: Int? = null,
    @ColumnInfo(name = "sortId") var sortType: PlaylistEpisodeSortType = PlaylistEpisodeSortType.NewestToOldest,
    @ColumnInfo(name = "manual") var manual: Boolean = false,
    @ColumnInfo(name = "deleted") var deleted: Boolean = false,
    @ColumnInfo(name = "syncStatus") var syncStatus: Int = SYNC_STATUS_SYNCED,
    // Auto download configuration
    @ColumnInfo(name = "autoDownload") var autoDownload: Boolean = false,
    @ColumnInfo(name = "autoDownloadLimit") var autodownloadLimit: Int = 10,
    // Smart playlist configuration
    @ColumnInfo(name = "unplayed") var unplayed: Boolean = true,
    @ColumnInfo(name = "partiallyPlayed") var partiallyPlayed: Boolean = true,
    @ColumnInfo(name = "finished") var finished: Boolean = true,
    @ColumnInfo(name = "downloaded") var downloaded: Boolean = true,
    @ColumnInfo(name = "notDownloaded") var notDownloaded: Boolean = true,
    @ColumnInfo(name = "audioVideo") var audioVideo: Int = AUDIO_VIDEO_FILTER_ALL,
    @ColumnInfo(name = "filterHours") var filterHours: Int = 0,
    @ColumnInfo(name = "starred") var starred: Boolean = false,
    @ColumnInfo(name = "allPodcasts") var allPodcasts: Boolean = true,
    @ColumnInfo(name = "podcastUuids") var podcastUuids: String? = null,
    @ColumnInfo(name = "filterDuration") var filterDuration: Boolean = false,
    @ColumnInfo(name = "longerThan") var longerThan: Int = 20,
    @ColumnInfo(name = "shorterThan") var shorterThan: Int = 40,
    // Manual playlist configuration
    @ColumnInfo(name = "showArchivedEpisodes") var showArchivedEpisodes: Boolean = false,
) : Serializable {
    companion object {
        const val AUDIO_VIDEO_FILTER_ALL = 0
        const val AUDIO_VIDEO_FILTER_AUDIO_ONLY = 1
        const val AUDIO_VIDEO_FILTER_VIDEO_ONLY = 2

        const val SYNC_STATUS_NOT_SYNCED = 0
        const val SYNC_STATUS_SYNCED = 1

        const val ANYTIME = 0
        const val LAST_24_HOURS = 24
        const val LAST_3_DAYS = 3 * 24
        const val LAST_WEEK = 7 * 24
        const val LAST_2_WEEKS = 14 * 24
        const val LAST_MONTH = 31 * 24
    }

    @ColumnInfo(name = "clean_title")
    var cleanTitle: String = ""
        get() = title.unidecode()
        internal set

    val icon get() = PlaylistIcon(iconId)

    var podcastUuidList: List<String>
        get() = podcastUuids?.splitIgnoreEmpty(",") ?: emptyList()
        set(value) {
            podcastUuids = value.joinToString(separator = ",")
        }
}
