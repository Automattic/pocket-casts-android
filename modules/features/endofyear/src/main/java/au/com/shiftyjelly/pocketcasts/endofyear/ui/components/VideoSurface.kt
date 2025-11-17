package au.com.shiftyjelly.pocketcasts.endofyear.ui.components

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.RepeatMode
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.state.rememberPresentationState

/**
 * Only use for end of year videos and not podcast videos as using the ExoPlayer in this way isn't optimal.
 */
@UnstableApi
@Composable
internal fun VideoSurface(
    @RawRes videoResourceId: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black,
    @RepeatMode repeat: Int = REPEAT_MODE_ALL,
    play: Boolean = true,
    videoRatio: Float? = null,
) {
    val context = LocalContext.current
    val player = remember { mutableStateOf<Player?>(null) }
    val presentationState = rememberPresentationState(player.value)

    LifecycleStartEffect(Unit) {
        val uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .path(videoResourceId.toString())
            .build()
        player.value = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = repeat
            prepare()
            playWhenReady = play
        }
        onStopOrDispose {
            player.value?.apply { release() }
            player.value = null
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        player.value?.let {
            PlayerSurface(
                player = it,
                // use TextureView instead of SurfaceView to avoid rendering issues on Android 24
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                modifier = videoRatio?.let { ratio -> Modifier.aspectRatio(ratio) } ?: Modifier,
            )
        } ?: run {
            // before the player is ready reserve the space
            Box(modifier = videoRatio?.let { ratio -> Modifier.aspectRatio(ratio) } ?: Modifier)
        }
        if (presentationState.coverSurface) {
            // while the video is loading, show a solid background instead of black
            Box(Modifier.background(backgroundColor).fillMaxSize())
        }
    }
}
