package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.Story

internal const val StoryRotationDegrees = -15f
internal const val StoryRotationRadians = (StoryRotationDegrees * Math.PI / 180).toFloat()

internal val humaneFontFamily = FontFamily(
    Font(R.font.humane_regular, FontWeight.Normal),
)

internal val Story.backgroundColor
    get() = when (this) {
        is Story.Cover -> Color(0xFFEE661C)
        is Story.NumberOfShows -> Color(0xFFEFECAD)
        is Story.TopShow -> Color(0xFFEDB0F3)
        is Story.TopShows -> Color(0xFFE0EFAD)
        is Story.Ratings -> Color(0xFFEFECAD)
        is Story.TotalTime -> Color(0xFFEDB0F3)
        is Story.LongestEpisode -> Color(0xFFE0EFAD)
        is Story.PlusInterstitial -> Color(0xFFEFECAD)
        is Story.YearVsYear -> Color(0xFFEEB1F4)
        is Story.CompletionRate -> Color(0xFFE0EFAD)
        is Story.Ending -> Color(0xFFEE661C)
    }

internal data class EndOfYearMeasurements(
    val width: Dp,
    val height: Dp,
    val coverFontSize: TextUnit,
    val coverTextHeight: Dp,
    val closeButtonBottomEdge: Dp,
) {
    val scale = width / 393.dp
}

@Composable
internal fun PreviewBox(
    content: @Composable (EndOfYearMeasurements) -> Unit,
) {
    BoxWithConstraints {
        content(
            EndOfYearMeasurements(
                width = maxWidth,
                height = maxHeight,
                coverFontSize = 260.sp,
                coverTextHeight = 210.dp,
                closeButtonBottomEdge = 44.dp,
            ),
        )
        CloseButton(onClose = {})
    }
}
