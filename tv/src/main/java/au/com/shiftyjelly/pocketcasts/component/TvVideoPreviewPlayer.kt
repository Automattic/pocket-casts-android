package au.com.shiftyjelly.pocketcasts.component

import android.content.Context
import android.view.LayoutInflater
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import au.com.shiftyjelly.pocketcasts.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

private const val PLAY_DELAY_MS = 2000L
private const val MAX_PREVIEW_MS = 30_000L
private const val FADE_DURATION_MS = 300
private const val FADE_STEPS = 20
private const val PREVIEW_VOLUME = 0.5f

@Composable
fun TvVideoPreviewPlayer(
    videoUrl: String,
    isFocused: Boolean,
    isPodcastPlaying: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentIsPodcastPlaying by rememberUpdatedState(isPodcastPlaying)

    var player by remember(videoUrl) { mutableStateOf<ExoPlayer?>(null) }
    var hasFirstFrame by remember(videoUrl) { mutableStateOf(false) }
    var isPreviewing by remember(videoUrl) { mutableStateOf(false) }

    DisposableEffect(videoUrl) {
        onDispose {
            player?.release()
            player = null
        }
    }

    LaunchedEffect(videoUrl, isFocused) {
        if (isFocused) {
            player?.volume = 0f
            delay(PLAY_DELAY_MS)
            val exoPlayer = player ?: createPreviewPlayer(context, videoUrl) { hasFirstFrame = true }.also { player = it }
            exoPlayer.volume = if (currentIsPodcastPlaying()) 0f else PREVIEW_VOLUME
            exoPlayer.prepare()
            exoPlayer.seekTo(0)
            exoPlayer.playWhenReady = true
            isPreviewing = true
            delay(MAX_PREVIEW_MS)
            fadeOutAndRelease(exoPlayer)
            player = null
            hasFirstFrame = false
            isPreviewing = false
        } else {
            player?.let { exoPlayer ->
                isPreviewing = false
                fadeOutAndRelease(exoPlayer)
                player = null
                hasFirstFrame = false
            }
        }
    }

    val showVideo = isPreviewing && hasFirstFrame
    val videoAlpha by animateFloatAsState(
        targetValue = if (showVideo) 1f else 0f,
        animationSpec = tween(durationMillis = FADE_DURATION_MS),
        label = "VideoPreviewAlpha",
    )

    AndroidView(
        factory = { viewContext ->
            (LayoutInflater.from(viewContext).inflate(R.layout.view_tv_video_preview, null) as PlayerView).apply {
                useController = false
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
        modifier = modifier.alpha(videoAlpha),
    )
}

private fun createPreviewPlayer(context: Context, videoUrl: String, onFirstFrame: () -> Unit): ExoPlayer {
    return ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        volume = 0f
        setMediaItem(MediaItem.fromUri(videoUrl))
        addListener(
            object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    onFirstFrame()
                }

                override fun onPlayerError(error: PlaybackException) {
                    Timber.w(error, "Failed to play TV discover video preview")
                }
            },
        )
    }
}

private suspend fun fadeOutAndRelease(player: ExoPlayer) {
    val startVolume = player.volume
    if (player.isPlaying && startVolume > 0f) {
        repeat(FADE_STEPS) { step ->
            player.volume = (startVolume * (FADE_STEPS - step - 1) / FADE_STEPS).coerceAtLeast(0f)
            delay((FADE_DURATION_MS / FADE_STEPS).toLong())
        }
    } else {
        delay(FADE_DURATION_MS.toLong())
    }
    player.release()
}
