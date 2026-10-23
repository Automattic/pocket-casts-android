package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import javax.inject.Inject

/**
 * Changes the effects of the playing episode, saving them to the podcast when it overrides the global effects.
 */
class PlaybackEffectsUpdater @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
) {
    /**
     * Returns the updated effects, or null when no podcast episode is playing.
     */
    fun update(updater: PlaybackEffects.() -> Unit): PlaybackEffects? {
        val currentPodcast = playbackManager.getCurrentPodcast() ?: return null

        val overrideGlobalEffects = currentPodcast.overrideGlobalEffects
        val playbackEffects = if (overrideGlobalEffects) {
            currentPodcast.playbackEffects
        } else {
            settings.globalPlaybackEffects.value
        }
        playbackEffects.updater()
        if (overrideGlobalEffects) {
            podcastManager.updateEffectsBlocking(currentPodcast, playbackEffects)
        } else {
            settings.globalPlaybackEffects.set(playbackEffects, updateModifiedAt = true)
        }
        playbackManager.updatePlayerEffects(playbackEffects)
        return playbackEffects
    }
}
