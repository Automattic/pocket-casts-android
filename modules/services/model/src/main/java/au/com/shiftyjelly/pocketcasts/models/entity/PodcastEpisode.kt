package au.com.shiftyjelly.pocketcasts.models.entity

import android.content.res.Resources
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import java.io.Serializable
import java.util.Date

@Entity(
    tableName = "podcast_episodes",
    indices = [
        Index(name = "episode_last_download_attempt_date", value = arrayOf("last_download_attempt_date")),
        Index(name = "episode_podcast_id", value = arrayOf("podcast_id")),
        Index(name = "episode_published_date", value = arrayOf("published_date"))
    ]
)
data class PodcastEpisode(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name = "uuid") override var uuid: String,
    @ColumnInfo(name = "episode_description") override var episodeDescription: String = "",
    @ColumnInfo(name = "published_date") override var publishedDate: Date,
    @ColumnInfo(name = "title") override var title: String = "",
    @ColumnInfo(name = "size_in_bytes") override var sizeInBytes: Long = 0,
    @ColumnInfo(name = "episode_status") override var episodeStatus: EpisodeStatusEnum = EpisodeStatusEnum.NOT_DOWNLOADED,
    @ColumnInfo(name = "file_type") override var fileType: String? = null,
    @ColumnInfo(name = "duration") override var duration: Double = 0.0,
    @ColumnInfo(name = "download_url") override var downloadUrl: String? = null,
    @ColumnInfo(name = "downloaded_file_path") override var downloadedFilePath: String? = null,
    @ColumnInfo(name = "downloaded_error_details") override var downloadErrorDetails: String? = null,
    @ColumnInfo(name = "play_error_details") override var playErrorDetails: String? = null,
    @ColumnInfo(name = "played_up_to") override var playedUpTo: Double = 0.0,
    @ColumnInfo(name = "playing_status") override var playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED,
    @ColumnInfo(name = "podcast_id") var podcastUuid: String = "",
    @ColumnInfo(name = "added_date") override var addedDate: Date = Date(),
    @ColumnInfo(name = "auto_download_status") override var autoDownloadStatus: Int = 0,
    @ColumnInfo(name = "starred") var isStarred: Boolean = false,
    @ColumnInfo(name = "thumbnail_status") var thumbnailStatus: Int = THUMBNAIL_STATUS_UNKNOWN,
    @ColumnInfo(name = "last_download_attempt_date") override var lastDownloadAttemptDate: Date? = null,
    @ColumnInfo(name = "playing_status_modified") override var playingStatusModified: Long? = null,
    @ColumnInfo(name = "played_up_to_modified") override var playedUpToModified: Long? = null,
    @ColumnInfo(name = "duration_modified") var durationModified: Long? = null,
    @ColumnInfo(name = "starred_modified") var starredModified: Long? = null,
    @ColumnInfo(name = "archived") override var isArchived: Boolean = false,
    @ColumnInfo(name = "archived_modified") var archivedModified: Long? = null,
    @ColumnInfo(name = "season") var season: Long? = null,
    @ColumnInfo(name = "number") var number: Long? = null,
    @ColumnInfo(name = "type") var type: String? = null,
    // Removed but leaving the column until we definitely aren't using it again
    @ColumnInfo(name = "cleanTitle") var cleanTitle: String? = title,
    @ColumnInfo(name = "last_playback_interaction_date") var lastPlaybackInteraction: Long? = null,
    @ColumnInfo(name = "last_playback_interaction_sync_status") var lastPlaybackInteractionSyncStatus: Long = LAST_PLAYBACK_INTERACTION_SYNCED,
    @ColumnInfo(name = "exclude_from_episode_limit") var excludeFromEpisodeLimit: Boolean = false,
    @ColumnInfo(name = "download_task_id") override var downloadTaskId: String? = null,
    @ColumnInfo(name = "last_archive_interaction_date") var lastArchiveInteraction: Long? = null
) : Episode, Serializable {

    sealed class EpisodeType {
        object Regular : EpisodeType()
        object Bonus : EpisodeType()
        object Trailer : EpisodeType()

        companion object {
            private const val EPISODE_TYPE_REGULAR = "episode"
            private const val EPISODE_TYPE_BONUS = "bonus"
            private const val EPISODE_TYPE_TRAILER = "trailer"

            fun fromString(value: String?): EpisodeType {
                return when (value) {
                    EPISODE_TYPE_BONUS -> Bonus
                    EPISODE_TYPE_TRAILER -> Trailer
                    else -> Regular
                }
            }
        }
    }

    companion object {

        /**
         * Auto Download Status
         *
         * AUTO_DOWNLOAD_STATUS_IGNORE
         * - user swipes to mark as played (with delete file on)
         * - user deletes file from episode popup card
         * - user bulk deletes file
         * - clean up deletes the file
         *
         * AUTO_DOWNLOAD_STATUS_AUTO_DOWNLOADED
         * - episode is either in auto download settings for the podcast or a playlist
         *
         * AUTO_DOWNLOAD_STATUS_MANUALLY_DOWNLOADED
         * - user pressed later on the dialog warning they are not on WiFi
         * - user pressed download all on a page
         * - user pressed retry download
         * - user pressed add to Up Next
         *
         * AUTO_DOWNLOAD_STATUS_MANUAL_OVERRIDE_WIFI
         * - user pressed download now and use my mobile data
         */
        const val AUTO_DOWNLOAD_STATUS_NOT_SPECIFIED = 0
        const val AUTO_DOWNLOAD_STATUS_IGNORE = 1
        const val AUTO_DOWNLOAD_STATUS_AUTO_DOWNLOADED = 2
        const val AUTO_DOWNLOAD_STATUS_MANUALLY_DOWNLOADED = 3
        const val AUTO_DOWNLOAD_STATUS_MANUAL_OVERRIDE_WIFI = 4

        const val THUMBNAIL_STATUS_UNKNOWN = 0
        const val THUMBNAIL_STATUS_EMBEDDED_AVAILABLE = 1
        const val THUMBNAIL_STATUS_EMBEDDED_NOT_AVAILABLE = 2

        const val LAST_PLAYBACK_INTERACTION_NOT_SYNCED = 0L
        const val LAST_PLAYBACK_INTERACTION_SYNCED = 1L

        private const val MIN_BYTES_FOR_PLAYBACK_DURING_DOWNLOAD: Long = 15360

        fun seasonPrefix(episodeType: EpisodeType, season: Long?, number: Long?, resources: Resources): String? {
            return if (episodeType !is EpisodeType.Bonus && (season ?: 0 > 0 || number ?: 0 > 0)) {
                return if (season ?: 0 > 0 && number ?: 0 > 0) {
                    resources.getString(R.string.episode_short_season_episode, season, number)
                } else if (season ?: 0 > 0) {
                    resources.getString(R.string.episode_short_season, season)
                } else if (number ?: 0 > 0) {
                    resources.getString(R.string.episode_short_episode, number)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    // temporary variables
    @Ignore
    override var playing: Boolean = false

    val playedPercentage: Int
        get() {
            return if (isFinished) {
                100
            } else {
                ((playedUpTo / duration) * 100).toInt()
            }
        }

    val isUnplayed: Boolean
        get() = EpisodePlayingStatus.NOT_PLAYED == playingStatus

    val episodeType: EpisodeType
        get() = EpisodeType.fromString(type)

    val lastPlaybackInteractionDate: Date?
        get() = lastPlaybackInteraction?.let { Date(it) }

    fun setPlayingStatusInt(status: Int) {
        this.playingStatus = when (status) {
            2 -> EpisodePlayingStatus.IN_PROGRESS
            3 -> EpisodePlayingStatus.COMPLETED
            else -> EpisodePlayingStatus.NOT_PLAYED
        }
    }

    override fun displaySubtitle(podcast: Podcast?): String {
        return podcast?.title ?: ""
    }
}
