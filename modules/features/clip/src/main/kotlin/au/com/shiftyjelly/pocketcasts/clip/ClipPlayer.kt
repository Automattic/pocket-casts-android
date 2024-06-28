package au.com.shiftyjelly.pocketcasts.clip

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ClipPlayer {
    val isPlayingState: StateFlow<Boolean>

    val errors: Flow<Exception>

    fun play(clip: Clip): Boolean

    fun stop(): Boolean

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
            return ExoPlayerClipPlayer(exoPlayer, mediaSourceFactory, playbackManager)
        }
    }
}

private class ExoPlayerClipPlayer(
    private val exoPlayer: ExoPlayer,
    private val mediaSourceFactory: MediaSource.Factory,
    private val playbackManager: PlaybackManager,
) : ClipPlayer {
    override val isPlayingState = MutableStateFlow(false)
    override val errors = MutableSharedFlow<Exception>()

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

    @OptIn(UnstableApi::class)
    override fun play(clip: Clip): Boolean {
        if (exoPlayer.isLoading || exoPlayer.isPlaying) {
            return false
        }
        playbackManager.pause()
        exoPlayer.setMediaSource(mediaSourceFactory.createMediaSource(clip.toMediaItem()))
        exoPlayer.prepare()
        exoPlayer.play()
        return true
    }

    override fun stop(): Boolean {
        if (!exoPlayer.isPlaying) {
            return false
        }
        exoPlayer.stop()
        return true
    }

    override fun release() {
        exoPlayer.stop()
        exoPlayer.release()
    }

    private fun Clip.toMediaItem() = MediaItem.Builder()
        .setUri(episode.let { if (it.isDownloaded) it.downloadedFilePath else it.downloadUrl })
        .setClippingConfiguration(
            ClippingConfiguration.Builder()
                .setStartPositionMs(range.start.inWholeMilliseconds)
                .setEndPositionMs(range.end.inWholeMilliseconds)
                .build(),
        )
        .build()
}
