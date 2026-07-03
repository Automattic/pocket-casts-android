package au.com.shiftyjelly.pocketcasts.voicecontrol.playback

import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.PlaybackContext
import javax.inject.Inject
import javax.inject.Singleton

interface PlaybackContextProvider {
    fun current(): PlaybackContext
}

@Singleton
class PlaybackManagerPlaybackContextProvider @Inject constructor(
    private val playbackManager: PlaybackManager,
) : PlaybackContextProvider {
    override fun current(): PlaybackContext {
        val episode = playbackManager.getCurrentEpisode()
        return PlaybackContext(
            episodeId = episode?.uuid ?: "",
            positionMs = 0L,
            recentTimestamps = emptyList(),
        )
    }
}
