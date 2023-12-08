package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class SimplePlayer(val settings: Settings, val statsManager: StatsManager, val context: Context, override val onPlayerEvent: (au.com.shiftyjelly.pocketcasts.repositories.playback.Player, PlayerEvent) -> Unit) : LocalPlayer(onPlayerEvent) {
    private val reducedBufferManufacturers = listOf("mercedes-benz")
    private val useReducedBuffer = reducedBufferManufacturers.contains(Build.MANUFACTURER.lowercase()) || Util.isWearOs(context)
    private val bufferTimeMinMillis = if (useReducedBuffer) TimeUnit.MINUTES.toMillis(2).toInt() else TimeUnit.MINUTES.toMillis(5).toInt()
    private val bufferTimeMaxMillis = bufferTimeMinMillis

    // Be careful increasing the size of the back buffer. It can easily lead to OOM errors.
    private val backBufferTimeMillis = if (useReducedBuffer) TimeUnit.SECONDS.toMillis(30).toInt() else TimeUnit.MINUTES.toMillis(1).toInt()

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

    @OptIn(UnstableApi::class)
    private fun prepare() {
        val trackSelector = DefaultTrackSelector(context)

        val minBufferMillis = if (isStreaming) bufferTimeMinMillis else DefaultLoadControl.DEFAULT_MIN_BUFFER_MS
        val maxBufferMillis = if (isStreaming) bufferTimeMaxMillis else DefaultLoadControl.DEFAULT_MAX_BUFFER_MS
        val backBufferMillis = if (isStreaming) backBufferTimeMillis else DefaultLoadControl.DEFAULT_BACK_BUFFER_DURATION_MS
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
            .setSeekForwardIncrementMs(settings.skipForwardInSecs.value * 1000L)
            .setSeekBackIncrementMs(settings.skipBackInSecs.value * 1000L)
            .build()

        renderer.onAudioSessionId(player.audioSessionId)

        handleStop()
        this.player = player

        setPlayerEffects()
        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                val episodeMetadata = EpisodeFileMetadata(filenamePrefix = episodeUuid)
                episodeMetadata.read(tracks, settings, context)
                onMetadataAvailable(episodeMetadata)
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                onBufferingStateChanged()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        onBufferingStateChanged()
                        onDurationAvailable()
                    }
                    Player.STATE_BUFFERING -> onBufferingStateChanged()
                    Player.STATE_ENDED -> onCompletion()
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
                if (location.uri != null) {
                    Uri.parse(location.uri)
                } else {
                    onError(PlayerEvent.PlayerError("Stream has no uri"))
                    return
                }
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
