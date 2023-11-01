package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.endofyear.utils.rainbowBrush
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryCompletionRate
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable

private val PercentFontSize = 45.sp
private const val CompletionCircleAvailableWidthPercent = .75f
private val CompletionCircleBaseColor = Color(0xFF292B2E)

@Composable
fun CompletionCircle(
    story: StoryCompletionRate,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(maxWidth * CompletionCircleAvailableWidthPercent)
                .drawCompletionCircle(story.percent.toInt()),
            contentAlignment = Alignment.Center,
        ) {
            CompletionTextContent(story)
        }
    }
}

@Composable
private fun CompletionTextContent(
    story: StoryCompletionRate,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextH10(
            text = "${story.percent.toInt()}%",
            fontSize = PercentFontSize,
            color = story.tintColor,
            disableScale = true,
        )
        TextH50(
            text = "completion rate",
            textAlign = TextAlign.Center,
            color = story.subtitleColor,
            disableScale = disableScale()
        )
    }
}

private fun Modifier.drawCompletionCircle(
    completionPercent: Int,
) = graphicsLayer { alpha = 0.99f }
    .drawWithCache {
        val circleColor = CompletionCircleBaseColor
        val circleStroke = Stroke((size.height * 0.02f))
        val circleRadius = (size.width - circleStroke.width) * 0.5f

        val arcBrush = rainbowBrush(
            start = Offset(-0.65f * size.width, 0.5f * size.height),
            end = Offset(1.49f * size.width, 0.5f * size.height)
        )
        val arcStroke = Stroke((size.height * 0.05f))
        val arcDiameterOffset = arcStroke.width / 2
        val arcDimen = size.width - 2 * arcDiameterOffset
        val arcSweepAngle = completionPercent.toDouble() / 100 * 360

        onDrawWithContent {
            drawCircle(
                color = circleColor,
                radius = circleRadius,
                style = circleStroke,
                blendMode = BlendMode.SrcOver
            )
            withTransform({ rotate(-90f) }) {
                drawArc(
                    brush = arcBrush,
                    startAngle = 0f,
                    sweepAngle = arcSweepAngle.toFloat(),
                    useCenter = false,
                    topLeft = Offset(arcDiameterOffset, arcDiameterOffset),
                    size = Size(arcDimen, arcDimen),
                    style = arcStroke,
                    blendMode = BlendMode.SrcOver
                )
            }
            drawContent()
        }
    }

@ShowkaseComposable(name = "CompletionRateCircle", group = "Images", styleName = "10%")
@Preview(name = "10%")
@Composable
fun CompletionRate10percentPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK_CONTRAST) {
        CompletionCircle(
            StoryCompletionRate(
                percent = 10f,
            ),
            Modifier.size(300.dp)
        )
    }
}

@ShowkaseComposable(name = "CompletionRateCircle", group = "Images", styleName = "30%")
@Preview(name = "30%")
@Composable
fun CompletionRate30percentPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK_CONTRAST) {
        CompletionCircle(
            StoryCompletionRate(
                percent = 30f,
            ),
            Modifier.size(300.dp)
        )
    }
}

@ShowkaseComposable(name = "CompletionRateCircle", group = "Images", styleName = "70%")
@Preview(name = "70%")
@Composable
fun CompletionRate70percentPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK_CONTRAST) {
        CompletionCircle(
            StoryCompletionRate(
                percent = 70f,
            ),
            Modifier.size(300.dp)
        )
    }
}

@ShowkaseComposable(name = "CompletionRateCircle", group = "Images", styleName = "100%")
@Preview(name = "100%")
@Composable
fun CompletionRate100percentPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK_CONTRAST) {
        CompletionCircle(
            StoryCompletionRate(
                percent = 100f,
            ),
            Modifier.size(300.dp)
        )
    }
}
