package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.Story
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

internal const val StoryRotationDegrees = -15f
internal const val StoryRotationRadians = (StoryRotationDegrees * Math.PI / 180).toFloat()

internal val humaneFontFamily = FontFamily(
    Font(R.font.humane_regular, FontWeight.Normal),
    Font(R.font.humane_bold, FontWeight.Bold),
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
internal fun ShareStoryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
) {
    OutlinedEoyButton(
        text = stringResource(LR.string.end_of_year_share_story),
        onClick = onClick,
        includePadding = includePadding,
        modifier = modifier,
    )
}

@Composable
internal fun OutlinedEoyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
) {
    RowOutlinedButton(
        text = text,
        fontSize = 18.nonScaledSp,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = colorResource(UR.color.coolgrey_90),
        ),
        border = ButtonDefaults.outlinedBorder.copy(
            brush = SolidColor(colorResource(UR.color.coolgrey_90)),
        ),
        onClick = onClick,
        includePadding = includePadding,
        modifier = modifier,
    )
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
