package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects

sealed interface EpisodeLocation {
    val episode: BaseEpisode
    val uri: String?

    data class Stream(
        override val episode: BaseEpisode,
        override val uri: String?,
        val isHls: Boolean,
        val isVideo: Boolean,
    ) : EpisodeLocation

    data class Downloaded(
        override val episode: BaseEpisode,
        override val uri: String?,
    ) : EpisodeLocation

    companion object {
        fun create(episode: BaseEpisode, preferStream: Boolean = false) = if (episode.isDownloaded && !(preferStream && episode.isStreamUrlVideo)) {
            EpisodeLocation.Downloaded(episode, episode.downloadedFilePath)
        } else {
            EpisodeLocation.Stream(episode, episode.streamUrl, episode.isStreamUrlHls, episode.isStreamUrlVideo)
        }
    }
}

val EpisodeLocation?.isHlsStream: Boolean
    get() = (this as? EpisodeLocation.Stream)?.isHls == true

/** Whether the stream is a video rendition, which the episode's own file type may not advertise. */
val EpisodeLocation?.isVideoStream: Boolean
    get() = (this as? EpisodeLocation.Stream)?.isVideo == true

/**
 * Whether the stream the player prepared carries video. A resolved video rendition starts [Unknown] until
 * the player's tracks resolve it to [HasVideo] or [AudioOnly]; the surface is shown only once it reaches [HasVideo].
 */
enum class StreamVideoState {
    NotVideo,
    Unknown,
    HasVideo,
    AudioOnly,
    ;

    companion object {
        fun initialFor(episode: BaseEpisode, audioOnly: Boolean, playingVideoStream: Boolean, isRemote: Boolean) = when {
            audioOnly && (episode.isVideo || playingVideoStream) -> AudioOnly
            playingVideoStream && !isRemote -> Unknown
            else -> NotVideo
        }
    }
}

interface Player {
    var isPip: Boolean
    val isRemote: Boolean
    val isStreaming: Boolean
    val filePath: String?
    val url: String?
    val episodeUuid: String?
    val name: String
    val isDownloading: Boolean
    val onPlayerEvent: (Player, PlayerEvent) -> Unit

    /**
     * Smoothed RMS level of the playing audio in the range 0f..1f, updated from the audio
     * pipeline so it is safe to poll every frame. Players that don't measure audio return 0f,
     * which the UI treats as "no level available" rather than silence.
     */
    val currentAudioLevel: Float get() = 0f

    suspend fun load(currentPositionMs: Int)
    suspend fun getCurrentPositionMs(): Int
    suspend fun play(currentPositionMs: Int)
    suspend fun pause()
    suspend fun stop()
    suspend fun setPlaybackEffects(playbackEffects: PlaybackEffects)
    fun updateAudioOnly() {}
    suspend fun seekToTimeMs(positionMs: Int)
    suspend fun isPlaying(): Boolean
    suspend fun isBuffering(): Boolean
    suspend fun durationMs(): Int?
    suspend fun bufferedUpToMs(): Int
    suspend fun bufferedPercentage(): Int
    fun supportsTrimSilence(): Boolean
    fun supportsVolumeBoost(): Boolean
    fun supportsVideo(): Boolean
    fun setVolume(volume: Float)
    fun setPodcast(podcast: Podcast?)
    fun setEpisode(episode: BaseEpisode, preferStream: Boolean = false)
}
