package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity

@Composable
fun ChapterProgressCircle(
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.getActivity()
    if (activity == null) return
    val playerViewModel: PlayerViewModel = ViewModelProvider(activity)[PlayerViewModel::class.java]
    val progress by playerViewModel.listDataLive
        .map { it.podcastHeader.chapterProgress }
        .observeAsState(0f)

    Content(
        progress = progress,
        modifier = modifier,
    )
}

@Composable
private fun Content(
    progress: Float,
    modifier: Modifier = Modifier,
    config: ChapterProgressConfig = ChapterProgressConfig(),
) {
    Canvas(
        modifier
            .size(config.imageSize),
    ) {
        val borderWidthPx = config.strokeWidth.toPx()
        val degrees = 360f * (1f - progress)
        drawArc(
            color = Color.White.copy(alpha = 0.4f),
            startAngle = -90f,
            sweepAngle = -degrees,
            useCenter = false,
            style = Stroke(borderWidthPx, cap = StrokeCap.Butt),
        )
    }
}

data class ChapterProgressConfig(
    val imageSize: Dp = 28.dp,
    val strokeWidth: Dp = 2.dp,
)

@Preview
@Composable
fun ChapterProgressCirclePreview() {
    Content(
        progress = 0.25f,
    )
}
