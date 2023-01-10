package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.widget.Toast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.mp3.Mp3Extractor
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.video.VideoSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class SimplePlayer(val settings: Settings, val statsManager: StatsManager, val context: Context, override val onPlayerEvent: (au.com.shiftyjelly.pocketcasts.repositories.playback.Player, PlayerEvent) -> Unit) : LocalPlayer(onPlayerEvent) {

    companion object {
        private val BUFFER_TIME_MIN_MILLIS = TimeUnit.MINUTES.toMillis(15).toInt()
        private val BUFFER_TIME_MAX_MILLIS = BUFFER_TIME_MIN_MILLIS

        // Be careful increasing the size of the back buffer. It can easily lead to OOM errors.
        private val BACK_BUFFER_TIME_MILLIS = TimeUnit.MINUTES.toMillis(2).toInt()
    }

    private var player: ExoPlayer? = null

    private var renderersFactory: ShiftyRenderersFactory? = null
    private var playbackEffects: PlaybackEffects? = null

    var videoWidth: Int = 0
    var videoHeight: Int = 0

    override var isPip: Boolean = false

    private var videoChangedListener: VideoChangedListener? = null

    @Volatile
    private var prepared = false

    val exoPlayer: ExoPlayer?
        get() {
            return player
        }

    override suspend fun bufferedUpToMs(): Int {
        return withContext(Dispatchers.Main) {
            player?.bufferedPosition?.toInt() ?: 0
        }
    }

    override suspend fun bufferedPercentage(): Int {
        return withContext(Dispatchers.Main) {
            player?.bufferedPercentage ?: 0
        }
    }

    override suspend fun durationMs(): Int? {
        return withContext(Dispatchers.Main) {
            val duration = player?.duration ?: C.TIME_UNSET
            if (duration == C.TIME_UNSET) null else duration.toInt()
        }
    }

    override suspend fun isPlaying(): Boolean {
        return withContext(Dispatchers.Main) {
            player?.playWhenReady ?: false
        }
    }

    override fun handleCurrentPositionMs(): Int {
        return player?.currentPosition?.toInt() ?: -1
    }

    override fun handlePrepare() {
        if (prepared) {
            return
        }
        prepare()
    }

    override fun handleStop() {
        try {
            player?.stop()
        } catch (e: Exception) {
        }

        try {
            player?.release()
        } catch (e: Exception) {
        }

        player = null
        prepared = false

        videoChangedListener?.videoNeedsReset()
    }

    override fun handlePause() {
        player?.playWhenReady = false
    }

    override fun handlePlay() {
        player?.playWhenReady = true
    }

    override fun handleSeekToTimeMs(positionMs: Int) {
        if (player?.isCurrentMediaItemSeekable == false && player?.isPlaying == true) {
            Toast.makeText(context, "Unable to seek. File headers appear to be invalid.", Toast.LENGTH_SHORT).show()
        } else {
            player?.seekTo(positionMs.toLong())
            super.onSeekComplete(positionMs)
        }
    }

    override fun handleIsBuffering(): Boolean {
        return (player?.playbackState ?: Player.STATE_ENDED) == Player.STATE_BUFFERING
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
        player?.volume = volume
    }

    override fun setPodcast(podcast: Podcast?) {}

    private fun prepare() {
        val trackSelector = DefaultTrackSelector(context)

        val minBufferMillis = if (isStreaming) BUFFER_TIME_MIN_MILLIS else DefaultLoadControl.DEFAULT_MIN_BUFFER_MS
        val maxBufferMillis = if (isStreaming) BUFFER_TIME_MAX_MILLIS else DefaultLoadControl.DEFAULT_MAX_BUFFER_MS
        val backBufferMillis = if (isStreaming) BACK_BUFFER_TIME_MILLIS else DefaultLoadControl.DEFAULT_BACK_BUFFER_DURATION_MS
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBufferMillis,
                maxBufferMillis,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setBackBuffer(backBufferMillis, DefaultLoadControl.DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME)
            .build()

        val renderer = createRenderersFactory()
        this.renderersFactory = renderer
        val player = ExoPlayer.Builder(context, renderer)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setSeekForwardIncrementMs(settings.getSkipForwardInMs())
            .setSeekBackIncrementMs(settings.getSkipBackwardInMs())
            .build()

        renderer.onAudioSessionId(player.audioSessionId)

        handleStop()
        this.player = player

        setPlayerEffects()
        player.addListener(object : Player.Listener {
            @Deprecated("Deprecated. Use onTracksInfoChanged(TracksInfo) instead.")
            override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
                val episodeMetadata = EpisodeFileMetadata(filenamePrefix = episodeUuid)
                episodeMetadata.read(trackSelections, settings, context)
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

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Pocket Casts")
            .setAllowCrossProtocolRedirects(true)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val extractorsFactory = DefaultExtractorsFactory().setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
        val location = episodeLocation
        if (location == null) {
            onError(PlayerEvent.PlayerError("Episode has no source"))
            return
        }

        val uri: Uri = when (location) {
            is EpisodeLocation.Stream -> {
                Uri.parse(location.uri)
            }
            is EpisodeLocation.Downloaded -> {
                val filePath = location.filePath
                if (filePath != null) {
                    Uri.fromFile(File(filePath))
                } else {
                    onError(PlayerEvent.PlayerError("File has no file path"))
                    return
                }
            }
        } ?: return

        val mediaItem = MediaItem.fromUri(uri)
        val source = if (isHLS) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
                .createMediaSource(mediaItem)
        }
        player.setMediaSource(source)
        player.prepare()

        prepared = true
    }

    private fun addVideoListener(player: ExoPlayer) {
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

    private fun createRenderersFactory(): ShiftyRenderersFactory {
        val playbackEffects: PlaybackEffects? = this.playbackEffects
        return if (playbackEffects == null) {
            ShiftyRenderersFactory(context = context, statsManager = statsManager, boostVolume = false)
        } else {
            ShiftyRenderersFactory(context = context, statsManager = statsManager, boostVolume = playbackEffects.isVolumeBoosted)
        }
    }

    fun setDisplay(surfaceView: SurfaceView?): Boolean {
        val player = player ?: return false

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
        if (player == null || playbackEffects == null) return // nothing to set

        renderersFactory?.let {
            it.setPlaybackSpeed(playbackEffects.playbackSpeed.toFloat())
            it.setRemoveSilence(playbackEffects.trimMode)
            it.setBoostVolume(playbackEffects.isVolumeBoosted)
        }
        player.playbackParameters = PlaybackParameters(playbackEffects.playbackSpeed.toFloat(), 1f)
    }
}
