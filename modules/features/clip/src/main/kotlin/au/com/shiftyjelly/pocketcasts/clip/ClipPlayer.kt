package au.com.shiftyjelly.pocketcasts.clip

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface ClipPlayer {
    val isPlayingState: StateFlow<Boolean>

    val playbackProgress: StateFlow<Duration>

    val errors: Flow<Exception>

    fun play(clip: Clip): Boolean

    fun stop(): Boolean

    fun pause(): Boolean

    fun seekTo(duration: Duration)

    fun setPlaybackPollingPeriod(idleDuration: Duration)

    fun release()

    @OptIn(UnstableApi::class)
    class Factory @Inject constructor(
        private val playbackManager: PlaybackManager,
        @ClipSimpleCache private val simpleCache: SimpleCache,
    ) {
        fun create(context: Context): ClipPlayer {
            val httpSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Pocket Casts")
                .setAllowCrossProtocolRedirects(true)
            val dataSourceFactory = DefaultDataSource.Factory(context, httpSourceFactory)
            val cacheSourceFactory = CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(dataSourceFactory)

            val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
            val mediaSourceFactory = DefaultMediaSourceFactory(cacheSourceFactory, extractorsFactory)
            val exoPlayer = ExoPlayer.Builder(context).build()
            return ExoPlayerClipPlayer(
                exoPlayer,
                mediaSourceFactory,
                playbackManager,
            )
        }
    }
}

@OptIn(UnstableApi::class)
private class ExoPlayerClipPlayer(
    private val exoPlayer: ExoPlayer,
    private val mediaSourceFactory: MediaSource.Factory,
    private val playbackManager: PlaybackManager,
) : ClipPlayer {
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    override val isPlayingState = MutableStateFlow(false)
    override val errors = MutableSharedFlow<Exception>()
    override val playbackProgress = channelFlow {
        while (currentCoroutineContext().isActive) {
            // Instead of simple delay loop we use inner job that we join.
            // This is done to account for dynamic changes to scale and resolution of the timeline.
            // When the timeline is zoomed in we want to send events much more frequently to
            // make the playback progress smooth.
            pendingPlaybkacProgressJob?.join()
            pendingPlaybkacProgressJob = launch {
                if (isPlaybackProgressDispatchEnabled) {
                    send(exoPlayer.currentPosition.milliseconds)
                }
                delay(playbackPollingFrequency)
            }
        }
    }.stateIn(coroutineScope, SharingStarted.Lazily, Duration.ZERO)

    // Progress UI indicator syncs with playback progress emited from the player.
    // Disable the dispatch to make dragging animation smoother.
    private var isPlaybackProgressDispatchEnabled = true
    private var playbackPollingFrequency = 100.milliseconds
    private var pendingPlaybkacProgressJob: Job? = null
    private var currentSeekToDuration: Duration? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingState.value = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                errors.tryEmit(error)
            }
        })
    }

    override fun play(clip: Clip): Boolean {
        if (exoPlayer.isLoading || exoPlayer.isPlaying) {
            return false
        }
        if (playbackManager.isPlaying()) {
            playbackManager.pause()
        }
        pendingPlaybkacProgressJob?.cancel()
        isPlaybackProgressDispatchEnabled = true
        if (!continuePlayback()) {
            playNewClip(clip)
        }
        return true
    }

    private fun continuePlayback() = if (exoPlayer.currentMediaItem != null) {
        if (exoPlayer.playbackState == STATE_ENDED) {
            exoPlayer.seekTo(0L)
        } else {
            currentSeekToDuration?.let { exoPlayer.seekTo(it.inWholeMilliseconds) }
            currentSeekToDuration = null
        }
        exoPlayer.play()
        true
    } else {
        false
    }

    private fun playNewClip(clip: Clip) {
        exoPlayer.setMediaSource(mediaSourceFactory.createMediaSource(clip.toMediaItem()))
        exoPlayer.prepare()
        currentSeekToDuration?.let { exoPlayer.seekTo(it.inWholeMilliseconds) }
        currentSeekToDuration = null
        exoPlayer.play()
    }

    override fun stop(): Boolean {
        if (!exoPlayer.isPlaying) {
            exoPlayer.clearMediaItems()
            return false
        }
        exoPlayer.stop()
        isPlaybackProgressDispatchEnabled = true
        exoPlayer.clearMediaItems()
        return true
    }

    override fun pause(): Boolean {
        if (!exoPlayer.isPlaying) {
            return false
        }
        exoPlayer.pause()
        isPlaybackProgressDispatchEnabled = true
        return true
    }

    override fun seekTo(duration: Duration) {
        isPlaybackProgressDispatchEnabled = false
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
        currentSeekToDuration = duration
    }

    override fun setPlaybackPollingPeriod(idleDuration: Duration) {
        playbackPollingFrequency = idleDuration
        pendingPlaybkacProgressJob?.cancel()
    }

    override fun release() {
        exoPlayer.stop()
        exoPlayer.release()
    }

    private fun Clip.toMediaItem() = MediaItem.Builder()
        .setUri(sourceUri)
        .setClippingConfiguration(
            ClippingConfiguration.Builder()
                .setStartPositionMs(range.start.inWholeMilliseconds)
                .setEndPositionMs(range.end.inWholeMilliseconds)
                .build(),
        )
        .build()
}
