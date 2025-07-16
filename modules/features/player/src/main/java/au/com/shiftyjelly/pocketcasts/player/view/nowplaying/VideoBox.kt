package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.player.view.video.VideoView
import au.com.shiftyjelly.pocketcasts.repositories.playback.Player

@Composable
internal fun VideoBox(
    player: Player?,
    configureVideoView: (VideoView) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .background(Color(0xFFD5F6FB), RoundedCornerShape(12.dp))
                .aspectRatio(1.78f),
        ) {
            Text(
                text = "VideoView preview",
                color = Color.Black,
            )
        }
    } else {
        var aspectRatio by remember { mutableFloatStateOf(1.78f) }

        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    configureVideoView(this)
                    addOnAspectRatioListener { ratio -> aspectRatio = ratio }
                }
            },
            update = { videoView ->
                videoView.player = player
                videoView.connectWithDelay()
            },
            modifier = modifier.aspectRatio(aspectRatio),
        )
    }
}
