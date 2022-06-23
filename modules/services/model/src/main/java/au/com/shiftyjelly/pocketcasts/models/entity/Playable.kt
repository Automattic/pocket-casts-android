package au.com.shiftyjelly.pocketcasts.models.entity

import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import java.util.Date
import java.util.UUID

interface Playable {
    companion object {
        /**
         * Used to reduce the changes sent out by the media session.
         * Returns true if the objects are the same.
         */
        val isMediaSessionEqual: (t1: Playable?, t2: Playable?) -> Boolean = { t1, t2 ->
            t1 != null && t2 != null && t1.uuid == t2.uuid &&
                (
                    (t1 is Episode && t2 is Episode && t1.isStarred == t2.isStarred) ||
                        (t1 is UserEpisode && t2 is UserEpisode && t1.tintColorIndex == t2.tintColorIndex)
                    )
        }
    }

    var uuid: String
    var publishedDate: Date
    var episodeDescription: String
    var title: String
    var sizeInBytes: Long
    var episodeStatus: EpisodeStatusEnum
    var fileType: String?
    var duration: Double
    var downloadUrl: String?
    var playedUpTo: Double
    var playingStatus: EpisodePlayingStatus
    var addedDate: Date
    var autoDownloadStatus: Int
    var lastDownloadAttemptDate: Date?
    var isArchived: Boolean
    var downloadTaskId: String?
    var downloadedFilePath: String?
    var playingStatusModified: Long?
    var playedUpToModified: Long?
    var playErrorDetails: String?
    var downloadErrorDetails: String?

    // temporary variables
    var playing: Boolean

    val isFinished: Boolean
        get() = EpisodePlayingStatus.COMPLETED == playingStatus

    var durationMs: Int
        get() = (duration * 1000.0).toInt()
        set(durationMs) {
            this.duration = durationMs.toDouble() / 1000.0
        }

    var playedUpToMs: Int
        get() = (playedUpTo * 1000.0).toInt()
        set(playedUpToMs) {
            this.playedUpTo = playedUpToMs.toDouble() / 1000.0
        }

    val adapterId: Long
        get() = UUID.nameUUIDFromBytes(uuid.toByteArray()).mostSignificantBits

    val isQueued: Boolean
        get() = EpisodeStatusEnum.QUEUED == episodeStatus || EpisodeStatusEnum.WAITING_FOR_WIFI == episodeStatus || EpisodeStatusEnum.WAITING_FOR_POWER == episodeStatus

    val isDownloading: Boolean
        get() = EpisodeStatusEnum.DOWNLOADING == episodeStatus

    val isDownloaded: Boolean
        get() = EpisodeStatusEnum.DOWNLOADED == episodeStatus

    val isInProgress: Boolean
        get() = EpisodePlayingStatus.IN_PROGRESS == playingStatus

    val isVideo: Boolean
        get() = fileType?.startsWith("video/") ?: false

    val isAudio: Boolean
        get() = !isVideo

    val isManualDownloadOverridingWifiSettings: Boolean
        get() = autoDownloadStatus == Episode.AUTO_DOWNLOAD_STATUS_MANUAL_OVERRIDE_WIFI

    val isAutoDownloaded: Boolean
        get() = autoDownloadStatus == Episode.AUTO_DOWNLOAD_STATUS_AUTO_DOWNLOADED

    val isExemptFromAutoDownload: Boolean
        get() = autoDownloadStatus == Episode.AUTO_DOWNLOAD_STATUS_IGNORE

    // fall back to something that most podcasts are
    fun getFileExtension(): String {
        val fileType = fileType ?: return ".mp3"

        when {
            fileType.equals("video/3gpp", ignoreCase = true) -> return ".3gp"
            fileType.equals("video/3gpp2", ignoreCase = true) -> return ".3g2"
            fileType.equals("video/mp4", ignoreCase = true) -> return ".mp4"
            fileType.equals("video/x-mp4", ignoreCase = true) -> return ".mp4"
            fileType.equals("video/quicktime", ignoreCase = true) -> return ".mov"
            fileType.equals("video/mov", ignoreCase = true) -> return ".mov"
            fileType.equals("video/x-m4v", ignoreCase = true) -> return ".m4v"
            fileType.equals("video/m4v", ignoreCase = true) -> return ".m4v"
            fileType.equals("video/x-ms-wmv", ignoreCase = true) -> return ".wmv"
            fileType.equals("video/ms-wmv", ignoreCase = true) -> return ".wmv"
            fileType.equals("video/x-flv", ignoreCase = true) -> return ".flv"
            fileType.equals("video/flv", ignoreCase = true) -> return ".flv"
            fileType.equals("audio/3gpp", ignoreCase = true) -> return ".3gp"
            fileType.equals("audio/3gpp2", ignoreCase = true) -> return ".3g2"
            fileType.equals("audio/aiff", ignoreCase = true) -> return ".aiff"
            fileType.equals("audio/x-aiff", ignoreCase = true) -> return ".aiff"
            fileType.equals("audio/amr", ignoreCase = true) -> return ".amr"
            fileType.equals("audio/mp3", ignoreCase = true) -> return ".mp3"
            fileType.equals("audio/mpeg3", ignoreCase = true) -> return ".mp3"
            fileType.equals("audio/x-mp3", ignoreCase = true) -> return ".mp3"
            fileType.equals("audio/x-mpeg3", ignoreCase = true) -> return ".mp3"
            fileType.equals("audio/mp4", ignoreCase = true) -> return ".mp4"
            fileType.equals("audio/mpeg", ignoreCase = true) -> return ".mp3"
            fileType.equals("audio/x-mpeg", ignoreCase = true) -> return ".mp3"
            fileType.equals("audio/wav", ignoreCase = true) -> return ".wav"
            fileType.equals("audio/x-wav", ignoreCase = true) -> return ".wav"
            fileType.equals("audio/x-m4a", ignoreCase = true) -> return ".m4a"
            fileType.equals("audio/x-m4b", ignoreCase = true) -> return ".m4b"
            fileType.equals("audio/x-m4p", ignoreCase = true) -> return ".m4p"
            fileType.equals("audio/ogg", ignoreCase = true) -> return ".ogg"
            fileType.equals("audio/x-ms-wma", ignoreCase = true) -> return ".wma"
            else -> return if (fileType.startsWith("video/")) ".mp4" else ".mp3"
        }
    }

    fun lastPlaybackFailed(): Boolean {
        return !playErrorDetails.isNullOrEmpty()
    }

    fun displaySubtitle(podcast: Podcast? = null): String
}
