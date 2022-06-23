package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.os.SystemClock
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages audio focus with local media player.
 */
abstract class LocalPlayer(override val onPlayerEvent: (Player, PlayerEvent) -> Unit) : Player {

    companion object {
        // The volume we set the media player to seekToTimeMswhen we lose audio focus, but are allowed to reduce the volume instead of stopping playback.
        const val VOLUME_DUCK = 1.0f // We don't actually duck the volume
        // The volume we set the media player when we have audio focus.
        const val VOLUME_NORMAL = 1.0f
    }

    // playback position for starting or resuming from focus lost
    @Volatile private var positionMs: Int = 0

    private var seekingToPositionMs: Int = 0
    private var seekRetryAllowed: Boolean = false

    override var episodeUuid: String? = null

    override var episodeLocation: EpisodeLocation? = null
    override val url: String?
        get() = (episodeLocation as? EpisodeLocation.Stream)?.uri

    override val filePath: String?
        get() = (episodeLocation as? EpisodeLocation.Downloaded)?.filePath

    override val isRemote: Boolean
        get() = false

    override val isStreaming: Boolean
        get() = episodeLocation is EpisodeLocation.Stream

    override val name: String
        get() = "System"

    abstract fun handlePrepare()
    abstract fun handleStop()
    abstract fun handlePause()
    abstract fun handlePlay()
    abstract fun handleSeekToTimeMs(positionMs: Int)
    abstract fun handleIsBuffering(): Boolean
    abstract fun handleIsPrepared(): Boolean
    abstract fun handleCurrentPositionMs(): Int

    override suspend fun load(currentPositionMs: Int) {
        withContext(Dispatchers.Main) {
            this@LocalPlayer.positionMs = currentPositionMs
            handlePrepare()
            seekToTimeMs(currentPositionMs)
        }
    }

    // downloaded episodes don't buffer
    override suspend fun isBuffering(): Boolean {
        return withContext(Dispatchers.Main) {
            if (filePath != null) false else handleIsBuffering()
        }
    }

    override suspend fun play(currentPositionMs: Int) {
        withContext(Dispatchers.Main) {
            this@LocalPlayer.positionMs = currentPositionMs

            handlePrepare()
            playIfAllowed()
        }
    }

    override suspend fun pause() {
        withContext(Dispatchers.Main) {
            if (isPlaying()) {
                handlePause()
                positionMs = handleCurrentPositionMs()
            }
            onPlayerEvent(this@LocalPlayer, PlayerEvent.PlayerPaused)
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            positionMs = handleCurrentPositionMs()
            handleStop()
        }
    }

    override suspend fun getCurrentPositionMs(): Int {
        return withContext(Dispatchers.Main) {
            handleCurrentPositionMs()
        }
    }

    protected fun onError(event: PlayerEvent.PlayerError) {
        onPlayerEvent(this, event)
    }

    private suspend fun playIfAllowed() {
        setVolume(VOLUME_NORMAL)

        // already playing?
        if (isPlaying()) {
            onPlayerEvent(this, PlayerEvent.PlayerPlaying)
        } else {
            // check the player is seeked to the correct position
            val playerPositionMs = getCurrentPositionMs()
            // check if the player is where it's meant to be, allow for a 2 second variance
            if (Math.abs(positionMs - playerPositionMs) > 2000) {
                onSeekStart(positionMs)
                handleSeekToTimeMs(positionMs)
            }
            handlePlay()
            onPlayerEvent(this, PlayerEvent.PlayerPlaying)
        }
    }

    private fun onSeekStart(positionMs: Int) {
        this.seekingToPositionMs = positionMs
        this.seekRetryAllowed = true
    }

    protected fun onSeekComplete(positionMs: Int) {
        // Fix for the BLU phone. With a new media player (also after a hibernate) the MediaTek player call switches to an invalid time.
        if (positionMs < seekingToPositionMs - 5000 && seekRetryAllowed) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Player issue was meant to be %.3f but was %.3f", seekingToPositionMs / 1000f, positionMs / 1000f)
            SystemClock.sleep(100)
            seekRetryAllowed = false
            handleSeekToTimeMs(seekingToPositionMs)
            return
        }
        this.positionMs = positionMs
        onPlayerEvent(this, PlayerEvent.SeekComplete(positionMs))
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "LocalPlayer onSeekComplete %.3f", positionMs / 1000f)
    }

    protected fun onDurationAvailable() {
        onPlayerEvent(this, PlayerEvent.DurationAvailable)
    }

    protected fun onCompletion() {
        onPlayerEvent(this, PlayerEvent.Completion(episodeUuid))
    }

    protected fun onBufferingStateChanged() {
        if (isStreaming) {
            onPlayerEvent(this, PlayerEvent.BufferingStateChanged)
        }
    }

    protected fun onMetadataAvailable(episodeMetadata: EpisodeFileMetadata) {
        onPlayerEvent(this, PlayerEvent.MetadataAvailable(episodeMetadata))
    }

    override suspend fun seekToTimeMs(positionMs: Int) {
        withContext(Dispatchers.Main) {
            if (positionMs < 0) {
                return@withContext
            }

            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "LocalPlayer seekToToTimeMs %.3f", positionMs / 1000f)

            this@LocalPlayer.positionMs = positionMs
            if (handleIsPrepared()) {
                onSeekStart(positionMs)
                handleSeekToTimeMs(positionMs)
            }
        }
    }

    override fun setEpisode(episode: Playable) {
        this.episodeUuid = episode.uuid
        episodeLocation = if (episode.isDownloaded) {
            EpisodeLocation.Downloaded(episode.downloadedFilePath)
        } else {
            EpisodeLocation.Stream(episode.downloadUrl)
        }
    }

    override suspend fun setPlaybackEffects(playbackEffects: PlaybackEffects) {}
}
