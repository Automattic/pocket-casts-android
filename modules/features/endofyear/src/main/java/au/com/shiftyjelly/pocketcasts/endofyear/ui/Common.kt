package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.buttons.CircleIconButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.models.to.Story
import java.io.File
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

internal const val STORY_ROTATION_DEGREES = -15f
internal const val STORY_ROTATION_RADIANS = (STORY_ROTATION_DEGREES * Math.PI / 180).toFloat()

internal val humaneFontFamily = FontFamily(
    Font(R.font.humane_regular, FontWeight.Normal),
    Font(R.font.humane_bold, FontWeight.Bold),
)

internal val Story.backgroundColor
    get() = when (this) {
        is Story.Cover -> Color(0xFF27486A)
        is Story.NumberOfShows -> Color(0xFFEFECAD)
        is Story.TopShow -> Color(0xFF17423B)
        is Story.TopShows -> Color(0xFF96BCD1)
        is Story.Ratings -> Color(0xFFA22828)
        is Story.TotalTime -> Color(0xFF27486A)
        is Story.LongestEpisode -> Color(0xFFE0EFAD)
        is Story.PlusInterstitial -> Color(0xFFEFECAD)
        is Story.YearVsYear -> Color(0xFFA22828)
        is Story.CompletionRate -> Color(0xFF27486A)
        is Story.Ending -> Color(0xFF27486A)
    }
internal val Story.controlsColor
    get() = when (this) {
        is Story.Cover,
        is Story.NumberOfShows,
        is Story.TopShow,
        is Story.Ratings,
        is Story.TotalTime,
        is Story.LongestEpisode,
        is Story.YearVsYear,
        is Story.CompletionRate,
        is Story.Ending,
        -> Color.White
        else -> Color.Black
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
internal fun OutlinedEoyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        includePadding = false,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp),
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
internal fun PlaybackText(
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    Text(
        text = "PLAYBACK",
        color = color,
        fontSize = fontSize,
        fontFamily = humaneFontFamily,
        onTextLayout = onTextLayout,
        modifier = modifier,
    )
}

@Composable
internal fun rememberHumaneTextFactory(
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.W500,
): HumaneTextFactory {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Humane font has a lot of empty space below first baseline
    // that is used for lowercase characters.
    // However, our designs use capital letters only and do not account for that empty space
    // and we have to adjust texts' heights accordingly.
    return remember {
        val measurement = textMeasurer.measure(
            text = "A",
            style = TextStyle(
                fontFamily = humaneFontFamily,
                fontSize = fontSize,
                fontWeight = fontWeight,
            ),
        )
        HumaneTextFactory(
            fontSize = fontSize,
            fontWeight = fontWeight,
            textHeight = density.run { (measurement.firstBaseline * 1.01f).toDp() },
            textWidth = density.run { measurement.size.width.toDp() },
        )
    }
}

internal class HumaneTextFactory(
    val fontSize: TextUnit,
    val fontWeight: FontWeight,
    val textHeight: Dp,
    val textWidth: Dp,
) {
    val maxSize get() = maxOf(textWidth, textHeight)

    @Composable
    fun HumaneText(
        text: String,
        modifier: Modifier = Modifier,
        paddingValues: PaddingValues = PaddingValues(),
        color: Color = colorResource(UR.color.coolgrey_90),
    ) {
        Text(
            text = text,
            color = color,
            fontFamily = humaneFontFamily,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1,
            modifier = Modifier
                .padding(paddingValues)
                .requiredHeight(textHeight)
                .then(modifier),
        )
    }
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
            controller = StoryCaptureController.preview(),
            color = Color.White,
        )
    }
}
