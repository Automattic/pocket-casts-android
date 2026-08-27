package au.com.shiftyjelly.pocketcasts.component

import android.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

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
    val player = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 0f
            setMediaItem(MediaItem.fromUri(videoUrl))
        }
    }
    var showVideo by remember(videoUrl) { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                showVideo = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, isFocused) {
        if (isFocused) {
            delay(PLAY_DELAY_MS)
            player.volume = if (isPodcastPlaying()) 0f else PREVIEW_VOLUME
            player.prepare()
            player.seekTo(0)
            player.playWhenReady = true
            delay(MAX_PREVIEW_MS)
        }
        player.fadeOutAndStop()
        showVideo = false
    }

    val videoAlpha by animateFloatAsState(
        targetValue = if (showVideo) 1f else 0f,
        animationSpec = tween(durationMillis = FADE_DURATION_MS),
        label = "VideoPreviewAlpha",
    )

    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setBackgroundColor(Color.TRANSPARENT)
                this.player = player
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
        modifier = modifier.alpha(videoAlpha),
    )
}

private suspend fun ExoPlayer.fadeOutAndStop() {
    val startVolume = volume
    if (isPlaying && startVolume > 0f) {
        repeat(FADE_STEPS) { step ->
            volume = (startVolume * (FADE_STEPS - step - 1) / FADE_STEPS).coerceAtLeast(0f)
            delay((FADE_DURATION_MS / FADE_STEPS).toLong())
        }
    }
    playWhenReady = false
    stop()
}
