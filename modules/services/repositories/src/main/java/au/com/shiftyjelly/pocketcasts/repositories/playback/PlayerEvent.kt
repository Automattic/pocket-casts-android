package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.common.PlaybackException

/**
 * A event on Player
 */
sealed class PlayerEvent {
    class PlayerError(val message: String, val error: PlaybackException? = null) : PlayerEvent()
    object BufferingStateChanged : PlayerEvent()
    object PlayerPlaying : PlayerEvent()
    object PlayerPaused : PlayerEvent()
    class Completion(val episodeUUID: String?) : PlayerEvent()
    object DurationAvailable : PlayerEvent()
    class SeekComplete(val positionMs: Int) : PlayerEvent()
    class MetadataAvailable(val metaData: EpisodeFileMetadata) : PlayerEvent()
    class RemoteMetadataNotMatched(val remoteEpisodeUuid: String) : PlayerEvent()
}
