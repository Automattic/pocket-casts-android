package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.endofyear.utils.rainbowBrush
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val PercentFontSize = 45.sp
private const val CompletionCircleAvailableWidthPercent = .75f
private val CompletionCircleBaseColor = Color(0xFF292B2E)

@Composable
fun CompletionRateCircle(
    percent: Int,
    titleColor: Color,
    subTitleColor: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val availableWidth = maxWidth * CompletionCircleAvailableWidthPercent
        Box(
            modifier = Modifier
                .size(availableWidth)
                .drawCompletionCircle(percent),
            contentAlignment = Alignment.Center,
        ) {
            CompletionTextContent(
                percent = percent,
                titleColor = titleColor,
                subTitleColor = subTitleColor,
            )
        }
    }
}

@Composable
private fun CompletionTextContent(
    percent: Int,
    titleColor: Color,
    subTitleColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextH10(
            text = "$percent%",
            color = titleColor,
            fontFamily = StoryFontFamily,
            fontSize = PercentFontSize,
            fontWeight = FontWeight.W300,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        TextH50(
            text = stringResource(LR.string.end_of_year_stories_year_completion_rate),
            color = subTitleColor,
            fontFamily = StoryFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.W600,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-5).dp)
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
    CompletionRateCirclePreview(10)
}

@ShowkaseComposable(name = "CompletionRateCircle", group = "Images", styleName = "30%")
@Preview(name = "30%")
@Composable
fun CompletionRate30percentPreview() {
    CompletionRateCirclePreview(30)
}

@ShowkaseComposable(name = "CompletionRateCircle", group = "Images", styleName = "70%")
@Preview(name = "70%")
@Composable
fun CompletionRate70percentPreview() {
    CompletionRateCirclePreview(70)
}

@ShowkaseComposable(name = "CompletionRateCircle", group = "Images", styleName = "100%")
@Preview(name = "100%")
@Composable
fun CompletionRate100percentPreview() {
    CompletionRateCirclePreview(100)
}

@Composable
fun CompletionRateCirclePreview(percent: Int) {
    AppThemeWithBackground(Theme.ThemeType.DARK_CONTRAST) {
        CompletionRateCircle(
            percent = percent,
            titleColor = Color.White,
            subTitleColor = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}
