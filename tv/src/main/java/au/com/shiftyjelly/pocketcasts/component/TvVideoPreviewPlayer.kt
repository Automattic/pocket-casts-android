package au.com.shiftyjelly.pocketcasts.component

import android.app.ActivityManager
import android.content.Context
import androidx.annotation.OptIn
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
import androidx.core.content.getSystemService
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import kotlinx.coroutines.delay
import timber.log.Timber

private const val PLAY_DELAY_MS = 2000L
private const val MAX_PREVIEW_MS = 30_000L
private const val FADE_DURATION_MS = 300
private const val FADE_STEPS = 20
private const val PREVIEW_VOLUME = 0.5f
private const val MIN_PREVIEW_TOTAL_MEMORY_BYTES = 1_280L * 1024 * 1024

@OptIn(UnstableApi::class)
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

    LaunchedEffect(videoUrl, isFocused) {
        if (isFocused) {
            val exoPlayer = player ?: run {
                delay(PLAY_DELAY_MS)
                createPreviewPlayer(
                    context = context,
                    videoUrl = videoUrl,
                    handlesAudioFocus = !currentIsPodcastPlaying(),
                    onFirstFrame = { hasFirstFrame = true },
                ).also { player = it }
            }
            exoPlayer.volume = if (currentIsPodcastPlaying()) 0f else PREVIEW_VOLUME
            exoPlayer.prepare()
            exoPlayer.seekTo(0)
            exoPlayer.playWhenReady = true
            isPreviewing = true
            delay(MAX_PREVIEW_MS)
            isPreviewing = false
            fadeOutVolume(exoPlayer)
            player = null
            hasFirstFrame = false
        } else {
            player?.let { exoPlayer ->
                isPreviewing = false
                fadeOutVolume(exoPlayer)
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

    player?.let { exoPlayer ->
        DisposableEffect(exoPlayer) {
            onDispose { exoPlayer.release() }
        }
        PlayerSurface(
            player = exoPlayer,
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = modifier.alpha(videoAlpha),
        )
    }
}

@Composable
fun rememberSupportsVideoPreviews(): Boolean {
    val context = LocalContext.current
    return remember(context) { supportsVideoPreviews(context) }
}

private var cachedSupportsVideoPreviews: Boolean? = null

private fun supportsVideoPreviews(context: Context): Boolean = cachedSupportsVideoPreviews ?: run {
    val activityManager = context.getSystemService<ActivityManager>()
    val memoryInfo = ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }
    val supported = activityManager != null &&
        !activityManager.isLowRamDevice &&
        memoryInfo.totalMem >= MIN_PREVIEW_TOTAL_MEMORY_BYTES
    supported.also { cachedSupportsVideoPreviews = it }
}

private fun createPreviewPlayer(
    context: Context,
    videoUrl: String,
    handlesAudioFocus: Boolean,
    onFirstFrame: () -> Unit,
): ExoPlayer {
    val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()
    return ExoPlayer.Builder(context).setAudioAttributes(audioAttributes, handlesAudioFocus).build().apply {
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

private suspend fun fadeOutVolume(player: ExoPlayer) {
    val startVolume = player.volume
    if (player.isPlaying && startVolume > 0f) {
        repeat(FADE_STEPS) { step ->
            player.volume = (startVolume * (FADE_STEPS - step - 1) / FADE_STEPS).coerceAtLeast(0f)
            delay((FADE_DURATION_MS / FADE_STEPS).toLong())
        }
    } else {
        delay(FADE_DURATION_MS.toLong())
    }
}
