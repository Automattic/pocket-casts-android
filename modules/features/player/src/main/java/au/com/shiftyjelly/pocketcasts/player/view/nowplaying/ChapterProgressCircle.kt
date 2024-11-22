package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ChapterProgressCircle(
    progress: Float,
    config: ChapterProgressConfig = ChapterProgressConfig(),
    modifier: Modifier = Modifier,
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
    ChapterProgressCircle(
        progress = 0.25f,
    )
}
