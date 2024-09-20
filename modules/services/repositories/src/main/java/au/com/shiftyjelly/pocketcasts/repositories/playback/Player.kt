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
    ) : EpisodeLocation

    data class Downloaded(
        override val episode: BaseEpisode,
        override val uri: String?,
    ) : EpisodeLocation

    companion object {
        fun create(episode: BaseEpisode) = if (episode.isDownloaded) {
            EpisodeLocation.Downloaded(episode, episode.downloadedFilePath)
        } else {
            EpisodeLocation.Stream(episode, episode.downloadUrl)
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

    suspend fun load(currentPositionMs: Int)
    suspend fun getCurrentPositionMs(): Int
    suspend fun play(currentPositionMs: Int)
    suspend fun pause()
    suspend fun stop()
    suspend fun setPlaybackEffects(playbackEffects: PlaybackEffects)
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
    fun setEpisode(episode: BaseEpisode)
}
