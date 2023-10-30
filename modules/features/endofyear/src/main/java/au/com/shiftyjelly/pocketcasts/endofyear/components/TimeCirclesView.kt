package au.com.shiftyjelly.pocketcasts.endofyear.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.endofyear.utils.rainbowBrush
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val SecsInOneDay = 60 * 60 * 24
private const val MissingDaysOverlayColor = 0xFF8F97A4

@Composable
fun TimeCirclesView(
    timeInSecs: Long,
) {
    val context = LocalContext.current
    val maxArea = Size(LocalView.current.width.toFloat(), LocalView.current.height * 0.3f)
    val number = ceil(timeInSecs.toDouble() / SecsInOneDay)
    val eachBallSquareArea = (maxArea.width * maxArea.height) / number
    val numberOfBallsPerLine = max(7.0, ceil(maxArea.width / sqrt(eachBallSquareArea)))
    val numberOfLines = min(ceil(number / numberOfBallsPerLine), max(1.0, ceil(maxArea.height / sqrt(eachBallSquareArea))))
    val ballCalculatedWidth = maxArea.width / numberOfBallsPerLine
    val ballCalculatedHeight = maxArea.height / numberOfLines
    val ballPadding = min(ballCalculatedWidth, ballCalculatedHeight) * 0.05
    val ballFinalWidth = min(ballCalculatedWidth, ballCalculatedHeight) - 4 * ballPadding
    val missingDays = ((numberOfLines * numberOfBallsPerLine * SecsInOneDay) - timeInSecs) / SecsInOneDay
    val ballWidthWithPadding = ballFinalWidth + (2 * ballPadding)
    val missingDaysWidth = missingDays * ballWidthWithPadding

    Column(
        Modifier
            .timeCircleBackground(
                missingDaysOverlayXOffset = ((numberOfBallsPerLine - missingDays) * ballFinalWidth).toFloat(),
                missingDaysOverlaySize = Size(missingDaysWidth.toFloat(), ballFinalWidth.toFloat())
            ),
    ) {
        repeat(numberOfLines.toInt()) {
            Row {
                repeat(numberOfBallsPerLine.toInt()) {
                    Box(
                        modifier = Modifier
                            .size(ballFinalWidth.toInt().pxToDp(context).dp)
                            .padding(ballPadding.toInt().pxToDp(context).dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.timeCircleBackground(
    missingDaysOverlayXOffset: Float,
    missingDaysOverlaySize: Size,
) =
    graphicsLayer { alpha = 0.99f }
        .drawWithCache {
            val brush = rainbowBrush(
                start = Offset(0f, 0f),
                end = Offset(1.22f * size.width, 1.25f * size.height),
            )
            onDrawWithContent {
                drawContent()

                drawRect(
                    brush = brush,
                    blendMode = BlendMode.SrcAtop
                )

                // Missing days overlay
                drawRect(
                    color = Color(MissingDaysOverlayColor),
                    topLeft = Offset(missingDaysOverlayXOffset, size.height - missingDaysOverlaySize.height),
                    size = missingDaysOverlaySize,
                    blendMode = BlendMode.SrcAtop
                )
            }
        }
