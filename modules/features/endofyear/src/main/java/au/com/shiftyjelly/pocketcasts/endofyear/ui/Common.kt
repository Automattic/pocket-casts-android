package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.buttons.CircleIconButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.models.to.Story
import java.io.File
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal val Story.backgroundColor
    get() = when (this) {
        is Story.Cover -> Color(0xFF27486A)
        is Story.NumberOfShows -> Color(0xFF27486A)
        is Story.TopShow -> Color(0xFF17423B)
        is Story.TopShows -> Color(0xFF96BCD1)
        is Story.Ratings -> Color(0xFFA22828)
        is Story.TotalTime -> Color(0xFF27486A)
        is Story.LongestEpisode -> Color(0xFF17423B)
        is Story.PlusInterstitial -> Color(0xFF96BCD1)
        is Story.YearVsYear -> Color(0xFFA22828)
        is Story.CompletionRate -> Color(0xFF27486A)
        is Story.Ending -> Color(0xFF27486A)
        is Story.PlaceholderWhileLoading -> Color(0xFF27486A)
    }

internal val Story.controlsColor
    get() = when (this) {
        is Story.Cover -> Color.White
        is Story.NumberOfShows -> Color.White
        is Story.TopShow -> Color.White
        is Story.TopShows -> Color.Black
        is Story.Ratings -> Color.White
        is Story.TotalTime -> Color.White
        is Story.LongestEpisode -> Color.White
        is Story.PlusInterstitial -> Color.Black
        is Story.YearVsYear -> Color.White
        is Story.CompletionRate -> Color.White
        is Story.Ending -> Color.White
        is Story.PlaceholderWhileLoading -> Color.White
    }

internal data class EndOfYearMeasurements(
    val width: Dp,
    val height: Dp,
    val statusBarInsets: WindowInsets,
    val coverFontSize: TextUnit,
    val coverTextHeight: Dp,
    val closeButtonBottomEdge: Dp,
) {
    val scale = width / 393.dp
    val smallDeviceFactor = if (width > 380.dp) 1f else 0.85f
}

@Composable
internal fun ShareStoryButton(
    story: Story,
    controller: StoryCaptureController,
    onShare: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    CircleIconButton(
        size = 40.dp,
        icon = painterResource(IR.drawable.ic_share),
        contentDescription = stringResource(LR.string.end_of_year_share_story),
        backgroundColor = Color.Black.copy(alpha = 0.44f),
        iconColor = Color.White,
        onClick = {
            scope.launch {
                val file = controller.capture(story)
                if (file != null) {
                    onShare(file)
                }
            }
        },
        modifier = modifier.padding(4.dp),
    )
}

@Composable
internal fun SolidEoyButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RowOutlinedButton(
        text = text,
        fontSize = 18.nonScaledSp,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = backgroundColor,
            contentColor = textColor,
        ),
        border = ButtonDefaults.outlinedBorder.copy(
            brush = SolidColor(backgroundColor),
        ),
        onClick = onClick,
        includePadding = false,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp),
    )
}

@Composable
internal fun PreviewBox(
    currentPage: Int,
    progress: Float = 0.5f,
    content: @Composable (EndOfYearMeasurements) -> Unit,
) {
    BoxWithConstraints {
        val measurements = EndOfYearMeasurements(
            width = maxWidth,
            height = maxHeight,
            statusBarInsets = WindowInsets(top = 16.dp),
            coverFontSize = 260.sp,
            coverTextHeight = 210.dp,
            closeButtonBottomEdge = 52.dp,
        )
        content(measurements)
        TopControls(
            pagerState = rememberPagerState(initialPage = currentPage, pageCount = { 11 }),
            progress = progress,
            measurements = measurements,
            onClose = {},
            onPreviousStory = {},
            onNextStory = {},
            controller = StoryCaptureController.preview(),
            isTalkbackOn = false,
            color = Color.White,
        )
    }
}
