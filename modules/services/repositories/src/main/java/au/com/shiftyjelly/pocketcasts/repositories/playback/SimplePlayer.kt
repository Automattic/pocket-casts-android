package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.widget.Toast
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class SimplePlayer(
    val settings: Settings,
    val statsManager: StatsManager,
    val context: Context,
    override val onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit,
    val player: Player,
) : LocalPlayer(onPlayerEvent, player) {

    private var renderersFactory: ShiftyRenderersFactory? = null
    private var playbackEffects: PlaybackEffects? = null

    var videoWidth: Int = 0
    var videoHeight: Int = 0

    override var isPip: Boolean = false

    private var videoChangedListener: VideoChangedListener? = null

    override var playable: Playable? = null

    @Volatile
    private var prepared = false

    override suspend fun bufferedUpToMs(): Int {
        return withContext(Dispatchers.Main) {
            player.bufferedPosition.toInt()
        }
    }

    override suspend fun bufferedPercentage(): Int {
        return withContext(Dispatchers.Main) {
            player.bufferedPercentage
        }
    }

    override suspend fun durationMs(): Int? {
        return withContext(Dispatchers.Main) {
            val duration = player.duration
            if (duration == C.TIME_UNSET) null else duration.toInt()
        }
    }

    override fun isPlaying(): Boolean = player.playWhenReady

    override fun handleCurrentPositionMs(): Int = player.currentPosition.toInt()

    override fun handlePrepare() {
        if (prepared) {
            return
        }
        prepare()
    }

    override fun handleStop() {
        prepared = false
        videoChangedListener?.videoNeedsReset()
    }

    override fun handlePause() {
        player.playWhenReady = false
    }

    override fun handlePlay() {
        player.playWhenReady = true
    }

    override fun handleSeekToTimeMs(positionMs: Int) {
        if (!player.isCurrentMediaItemSeekable && player.isPlaying) {
            Toast.makeText(context, "Unable to seek. File headers appear to be invalid.", Toast.LENGTH_SHORT).show()
        } else {
            player.seekTo(positionMs.toLong())
            super.onSeekComplete(positionMs)
        }
    }

    override fun handleIsBuffering(): Boolean {
        return (player.playbackState) == Player.STATE_BUFFERING
    }

    override fun handleIsPrepared(): Boolean {
        return prepared
    }

    override fun supportsTrimSilence(): Boolean {
        return true
    }

    override fun supportsVolumeBoost(): Boolean {
        return true
    }

    override suspend fun setPlaybackEffects(playbackEffects: PlaybackEffects) {
        withContext(Dispatchers.Main) {
            this@SimplePlayer.playbackEffects = playbackEffects
            setPlayerEffects()
        }
    }

    override fun supportsVideo(): Boolean {
        return true
    }

    override fun setVolume(volume: Float) {
        player.volume = volume
    }

    override fun setPodcast(podcast: Podcast?) {}

    override fun prepare() {
        setPlayerEffects()

        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                val episodeMetadata = EpisodeFileMetadata(filenamePrefix = playable?.uuid)
                episodeMetadata.read(tracks, settings, context)
                onMetadataAvailable(episodeMetadata)
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                onBufferingStateChanged()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onCompletion()
                } else if (playbackState == Player.STATE_READY) {
                    onBufferingStateChanged()
                    onDurationAvailable()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                LogBuffer.e(LogBuffer.TAG_PLAYBACK, error, "Play failed.")
                val event = PlayerEvent.PlayerError(error.message ?: "", error)
                this@SimplePlayer.onError(event)
            }
        })

        addVideoListener(player)

        playable?.uuid?.let {
            player.setMediaItem(
                MediaItem.Builder()
                    .setMediaId(it)
                    .build()
            )
            player.prepare()
            prepared = true
        }
    }

    private fun addVideoListener(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height

                videoChangedListener?.let {
                    Handler(Looper.getMainLooper()).post { it.videoSizeChanged(videoSize.width, videoSize.height, videoSize.pixelWidthHeightRatio) }
                }
            }
        })
    }

    fun setDisplay(surfaceView: SurfaceView?): Boolean {
        return try {
            player.setVideoSurfaceHolder(surfaceView?.holder)
            true
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    interface VideoChangedListener {
        fun videoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float)
        fun videoNeedsReset()
    }

    fun setVideoSizeChangedListener(videoChangedListener: VideoChangedListener) {
        this.videoChangedListener = videoChangedListener
        if (videoWidth != 0 && videoHeight != 0) {
            videoChangedListener.videoSizeChanged(videoWidth, videoHeight, 1f)
        }
    }

    private fun setPlayerEffects() {
        val player = player
        val playbackEffects = playbackEffects
            ?: return // nothing to set

        renderersFactory?.let {
            it.setPlaybackSpeed(playbackEffects.playbackSpeed.toFloat())
            it.setRemoveSilence(playbackEffects.trimMode)
            it.setBoostVolume(playbackEffects.isVolumeBoosted)
        }
        player.playbackParameters = PlaybackParameters(playbackEffects.playbackSpeed.toFloat(), 1f)
    }
}
