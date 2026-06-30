package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.media3.common.MimeTypes
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import java.util.Date
import java.util.UUID

sealed interface BaseEpisode {
    companion object {
        const val AUTO_DOWNLOAD_STATUS_ALLOW = 0
        const val AUTO_DOWNLOAD_STATUS_IGNORE = 1

        fun isHlsUrl(url: String?): Boolean {
            url ?: return false
            val path = url.substringBefore('?').substringBefore('#')
            return path.endsWith(".m3u8", ignoreCase = true)
        }

        /** The HLS MIME types the server may pass through verbatim; match case-insensitively. */
        val HLS_MIME_TYPES: Set<String> = setOf(
            MimeTypes.APPLICATION_M3U8.lowercase(), // application/x-mpegurl
            "application/vnd.apple.mpegurl",
            "audio/mpegurl",
            "application/mpegurl",
            "audio/x-mpegurl",
        )

        fun isHlsMimeType(type: String?): Boolean {
            return type != null && type.lowercase() in HLS_MIME_TYPES
        }

        /**
         * Used to reduce the changes sent out by the media session.
         * Returns true if the objects are the same.
         */
        val isMediaSessionEqual: (t1: BaseEpisode?, t2: BaseEpisode?) -> Boolean = { t1, t2 ->
            t1 != null &&
                t2 != null &&
                t1.uuid == t2.uuid &&
                (
                    (t1 is PodcastEpisode && t2 is PodcastEpisode && t1.isStarred == t2.isStarred) ||
                        (t1 is UserEpisode && t2 is UserEpisode && t1.tintColorIndex == t2.tintColorIndex)
                    )
        }
    }

    var uuid: String
    var publishedDate: Date
    var episodeDescription: String
    var title: String
    var sizeInBytes: Long
    var downloadStatus: EpisodeDownloadStatus
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
    var deselectedChapters: ChapterIndices
    var deselectedChaptersModified: Date?
    var isStarred: Boolean

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

    val isDownloadNotRequested
        get() = downloadStatus == EpisodeDownloadStatus.DownloadNotRequested

    val isQueuedForDownload
        get() = downloadStatus == EpisodeDownloadStatus.Queued

    val isWaitingForWifi
        get() = downloadStatus == EpisodeDownloadStatus.WaitingForWifi

    val isWaitingForPower
        get() = downloadStatus == EpisodeDownloadStatus.WaitingForPower

    val isWaitingForStorage
        get() = downloadStatus == EpisodeDownloadStatus.WaitingForStorage

    val isDownloadPending
        get() = downloadStatus.isPending

    val isDownloadCancellable
        get() = downloadStatus.isCancellable

    val isDownloading
        get() = downloadStatus == EpisodeDownloadStatus.Downloading

    val isDownloaded
        get() = downloadStatus == EpisodeDownloadStatus.Downloaded

    val isDownloadFailure
        get() = downloadStatus == EpisodeDownloadStatus.DownloadFailed

    val isAutoDownloadDisabled: Boolean
        get() = autoDownloadStatus == AUTO_DOWNLOAD_STATUS_IGNORE

    val canQueueForAutoDownload
        get() = !isFinished && !isArchived && !isAutoDownloadDisabled && !isHlsOnly

    val isInProgress: Boolean
        get() = EpisodePlayingStatus.IN_PROGRESS == playingStatus

    val isVideo: Boolean
        get() = fileType?.startsWith("video/") ?: false

    /** The enclosure itself is HLS, so there is no progressive file to download. */
    val isHlsOnly: Boolean
        get() = isHlsUrl(downloadUrl) || isHlsMimeType(fileType)

    /** A runtime-only resolved stream (e.g. the HLS alternate enclosure); overrides [streamUrl] when set. */
    var overrideStreamUrl: String?

    /** The content type of [overrideStreamUrl], used to decide HLS/video handling. */
    var overrideStreamContentType: String?

    /**
     * The URL to use when streaming. Downloaded playback uses [downloadedFilePath] instead. Falls back
     * to the progressive [downloadUrl] unless a stream has been resolved into [overrideStreamUrl].
     */
    val streamUrl: String?
        get() = overrideStreamUrl ?: downloadUrl

    /** Whether the URL that streaming will actually use is HLS. */
    val isStreamUrlHls: Boolean
        get() {
            val url = streamUrl ?: return false
            if (overrideStreamUrl != null) {
                return isHlsMimeType(overrideStreamContentType) || isHlsUrl(url)
            }
            return isHlsUrl(url) || (url == downloadUrl && isHlsMimeType(fileType))
        }

    /** Whether the stream that will actually play is video (honors a resolved stream's content type). */
    val isStreamVideo: Boolean
        get() = overrideStreamContentType?.startsWith("video/") ?: isVideo

    val isAudio: Boolean
        get() = !isVideo

    val podcastOrSubstituteUuid: String
        get() = if (this is PodcastEpisode) this.podcastUuid else Podcast.userPodcast.uuid

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
            fileType.equals(MimeTypes.APPLICATION_M3U8, ignoreCase = true) -> return ".m3u8"
            fileType.equals("application/vnd.apple.mpegurl", ignoreCase = true) -> return ".m3u8"
            else -> return if (fileType.startsWith("video/")) ".mp4" else ".mp3"
        }
    }

    fun lastPlaybackFailed(): Boolean {
        return !playErrorDetails.isNullOrEmpty()
    }

    fun displaySubtitle(podcast: Podcast? = null): String
}
