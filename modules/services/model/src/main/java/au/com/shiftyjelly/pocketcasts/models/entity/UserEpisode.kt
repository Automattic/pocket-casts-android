package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import au.com.shiftyjelly.pocketcasts.models.db.helper.UserEpisodePodcastSubstitute
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import java.io.Serializable
import java.util.Date

@Entity(
    tableName = "user_episodes",
    indices = [
        Index(name = "user_episode_last_download_attempt_date", value = arrayOf("last_download_attempt_date")),
        Index(name = "user_episode_published_date", value = arrayOf("published_date"))
    ]
)
data class UserEpisode(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "uuid") override var uuid: String,
    @ColumnInfo(name = "published_date") override var publishedDate: Date,
    @ColumnInfo(name = "episode_description") override var episodeDescription: String = "",
    @ColumnInfo(name = "title") override var title: String = "",
    @ColumnInfo(name = "size_in_bytes") override var sizeInBytes: Long = 0,
    @ColumnInfo(name = "episode_status") override var episodeStatus: EpisodeStatusEnum = EpisodeStatusEnum.NOT_DOWNLOADED,
    @ColumnInfo(name = "file_type") override var fileType: String? = null,
    @ColumnInfo(name = "duration") override var duration: Double = 0.0,
    @ColumnInfo(name = "download_url") override var downloadUrl: String? = null,
    @ColumnInfo(name = "played_up_to") override var playedUpTo: Double = 0.0,
    @ColumnInfo(name = "playing_status") override var playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED,
    @ColumnInfo(name = "added_date") override var addedDate: Date = Date(),
    @ColumnInfo(name = "auto_download_status") override var autoDownloadStatus: Int = 0,
    @ColumnInfo(name = "last_download_attempt_date") override var lastDownloadAttemptDate: Date? = null,
    @ColumnInfo(name = "archived") override var isArchived: Boolean = false,
    @ColumnInfo(name = "download_task_id") override var downloadTaskId: String? = null,
    @ColumnInfo(name = "downloaded_file_path") override var downloadedFilePath: String? = null,
    @ColumnInfo(name = "playing_status_modified") override var playingStatusModified: Long? = null,
    @ColumnInfo(name = "played_up_to_modified") override var playedUpToModified: Long? = null,
    @ColumnInfo(name = "artwork_url") var artworkUrl: String? = null,
    @ColumnInfo(name = "play_error_details") override var playErrorDetails: String? = null,
    @ColumnInfo(name = "server_status") var serverStatus: UserEpisodeServerStatus = UserEpisodeServerStatus.LOCAL,
    @ColumnInfo(name = "upload_error_details") val uploadErrorDetails: String? = null,
    @ColumnInfo(name = "downloaded_error_details") override var downloadErrorDetails: String? = null,
    @ColumnInfo(name = "tint_color_index") var tintColorIndex: Int = 0,
    @ColumnInfo(name = "has_custom_image") var hasCustomImage: Boolean = false,
    @ColumnInfo(name = "upload_task_id") var uploadTaskId: String? = null
) : BaseEpisode, Serializable {
    // temporary variables
    @Ignore
    override var playing: Boolean = false

    @Ignore
    var hasBookmark: Boolean = false

    override fun displaySubtitle(podcast: Podcast?): String {
        return UserEpisodePodcastSubstitute.substituteTitle
    }

    val isUploading: Boolean
        get() = UserEpisodeServerStatus.UPLOADING == serverStatus

    val isQueuedForUpload: Boolean
        get() = UserEpisodeServerStatus.QUEUED == serverStatus

    val isUploaded: Boolean
        get() = UserEpisodeServerStatus.UPLOADED == serverStatus
}
