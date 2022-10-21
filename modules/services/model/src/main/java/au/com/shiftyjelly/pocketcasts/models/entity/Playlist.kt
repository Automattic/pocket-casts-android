package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import au.com.shiftyjelly.pocketcasts.utils.extensions.splitIgnoreEmpty
import java.io.Serializable
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Entity(
    tableName = "filters",
    indices = [
        Index(name = "filters_uuid", value = arrayOf("uuid"))
    ]
)
data class Playlist(
    @PrimaryKey @ColumnInfo(name = "_id") var id: Long? = null,
    @ColumnInfo(name = "uuid") var uuid: String,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "sortPosition") var sortPosition: Int? = null,
    @ColumnInfo(name = "manual") var manual: Boolean = false,
    @ColumnInfo(name = "unplayed") var unplayed: Boolean = true,
    @ColumnInfo(name = "partiallyPlayed") var partiallyPlayed: Boolean = true,
    @ColumnInfo(name = "finished") var finished: Boolean = true,
    @ColumnInfo(name = "audioVideo") var audioVideo: Int = AUDIO_VIDEO_FILTER_ALL,
    @ColumnInfo(name = "allPodcasts") var allPodcasts: Boolean = true,
    @ColumnInfo(name = "podcastUuids") var podcastUuids: String? = null,
    @ColumnInfo(name = "downloaded") var downloaded: Boolean = true,
    @ColumnInfo(name = "downloading") var downloading: Boolean = true,
    @ColumnInfo(name = "notDownloaded") var notDownloaded: Boolean = true,
    @ColumnInfo(name = "autoDownload") var autoDownload: Boolean = false,
    @ColumnInfo(name = "autoDownloadWifiOnly") var autoDownloadUnmeteredOnly: Boolean = false,
    @ColumnInfo(name = "autoDownloadPowerOnly") var autoDownloadPowerOnly: Boolean = false,
    @ColumnInfo(name = "sortId") var sortId: Int = SortOrder.NEWEST_TO_OLDEST.value,
    @ColumnInfo(name = "iconId") var iconId: Int = 0,
    @ColumnInfo(name = "filterHours") var filterHours: Int = 0,
    @ColumnInfo(name = "starred") var starred: Boolean = false,
    @ColumnInfo(name = "deleted") var deleted: Boolean = false,
    @ColumnInfo(name = "syncStatus") var syncStatus: Int = SYNC_STATUS_SYNCED,
    @ColumnInfo(name = "autoDownloadLimit") var autodownloadLimit: Int = 10,
    @ColumnInfo(name = "filterDuration") var filterDuration: Boolean = false,
    @ColumnInfo(name = "longerThan") var longerThan: Int = 20,
    @ColumnInfo(name = "shorterThan") var shorterThan: Int = 40,
    @ColumnInfo(name = "draft") var draft: Boolean = false // Used when creating a new filter
) : Serializable {

    constructor() : this(uuid = "")

    companion object {
        const val PLAYLIST_ID_SYSTEM_DOWNLOADS: Long = -100L

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

    @Ignore
    var episodeUuids: String? = null

    @Ignore
    var episodeCount: Int = 0

    val isSystemDownloadsFilter: Boolean
        get() = id != null && id == PLAYLIST_ID_SYSTEM_DOWNLOADS

    val isAudioOnly: Boolean
        get() = audioVideo == AUDIO_VIDEO_FILTER_AUDIO_ONLY

    val isVideoOnly: Boolean
        get() = audioVideo == AUDIO_VIDEO_FILTER_VIDEO_ONLY

    var podcastUuidList: List<String>
        get() = podcastUuids?.splitIgnoreEmpty(",") ?: emptyList()
        set(value) {
            podcastUuids = value.joinToString(separator = ",")
        }

    val episodeOptionStringIds: List<Int>
        get() {
            val list = mutableListOf<Int>()
            if (unplayed && partiallyPlayed && finished) {
                return emptyList()
            }

            if (unplayed) {
                list.add(LR.string.unplayed)
            }
            if (partiallyPlayed) {
                list.add(LR.string.in_progress)
            }
            if (finished) {
                list.add(LR.string.played)
            }
            return list.toList()
        }

    val downloadOptionStrings: List<Int>
        get() {
            val list = mutableListOf<Int>()
            if (downloaded && notDownloaded) {
                return emptyList()
            }

            if (downloaded) {
                list.add(LR.string.downloaded)
            }
            if (notDownloaded) {
                list.add(LR.string.not_downloaded)
            }
            return list.toList()
        }

    val audioOptionStrings: List<Int>
        get() {
            val list = mutableListOf<Int>()
            if (audioVideo == AUDIO_VIDEO_FILTER_ALL) {
                return emptyList()
            }
            if (isAudioOnly) {
                list.add(LR.string.audio)
            }
            if (isVideoOnly) {
                list.add(LR.string.video)
            }
            return list.toList()
        }

    val stringForFilterHours: Int
        get() = when (filterHours) {
            ANYTIME -> LR.string.filters_release_date
            LAST_24_HOURS -> LR.string.filters_time_24_hours
            LAST_3_DAYS -> LR.string.filters_time_3_days
            LAST_WEEK -> LR.string.filters_time_week
            LAST_2_WEEKS -> LR.string.filters_time_2_weeks
            LAST_MONTH -> LR.string.filters_time_month
            else -> LR.string.filters_release_date
        }

    val isAllEpisodes: Boolean
        get() = unplayed && partiallyPlayed && finished && notDownloaded && downloaded && audioVideo == AUDIO_VIDEO_FILTER_ALL && allPodcasts && !filterDuration && filterHours == 0 && !starred

    fun sortOrder() = SortOrder.fromInt(sortId)

    enum class SortOrder(val value: Int) {
        NEWEST_TO_OLDEST(0),
        OLDEST_TO_NEWEST(1),
        SHORTEST_TO_LONGEST(2),
        LONGEST_TO_SHORTEST(3),
        LAST_DOWNLOAD_ATTEMPT_DATE(100);

        companion object {
            fun fromInt(value: Int) =
                SortOrder.values().find { it.value == value }
        }
    }
}
