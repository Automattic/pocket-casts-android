package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun YearVsYearStory(
    story: Story.YearVsYear,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) = YearVsYearStory(
    story = story,
    measurements = measurements,
    areCirclesVisible = false,
    controller = controller,
    onShareStory = onShareStory,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun YearVsYearStory(
    story: Story.YearVsYear,
    measurements: EndOfYearMeasurements,
    areCirclesVisible: Boolean,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Column(
        modifier = Modifier
            .capturable(controller.captureController(story))
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge + 8.dp),
    ) {
        ComparisonSection(
            story = story,
            measurements = measurements,
            areCirclesVisible = areCirclesVisible,
            forceCirclesVisible = controller.isSharing,
        )
        TextInfo(
            story = story,
            measurements = measurements,
            controller = controller,
            onShareStory = onShareStory,
        )
    }
}

@Composable
private fun ColumnScope.ComparisonSection(
    story: Story.YearVsYear,
    measurements: EndOfYearMeasurements,
    areCirclesVisible: Boolean,
    forceCirclesVisible: Boolean,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        YearVsYearBalls(
            story = story,
            measurements = measurements,
            areCirclesVisible = areCirclesVisible,
            forceCirclesVisible = forceCirclesVisible,
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.YearVsYearBalls(
    story: Story.YearVsYear,
    measurements: EndOfYearMeasurements,
    areCirclesVisible: Boolean,
    forceCirclesVisible: Boolean,
) {
    val smallTextFactory = rememberHumaneTextFactory(128.nonScaledSp * measurements.smallDeviceFactor)
    val largeTextFactory = rememberHumaneTextFactory(150.nonScaledSp * measurements.smallDeviceFactor)

    val baseSize = 350.dp * measurements.smallDeviceFactor
    val configuration = remember(story.yearOverYearChange) {
        when (story.yearOverYearChange) {
            in 0.9..1.1 -> YearVsYearConfiguration(
                lastYear = BallConfiguration(
                    circleSize = 8.dp,
                    textFactory = largeTextFactory,
                ),
                thisYear = BallConfiguration(
                    circleSize = 8.dp,
                    textFactory = largeTextFactory,
                ),
                offset = DpOffset(maxWidth / 4.5f, maxHeight / 8),
            )

            in 1.1..Double.POSITIVE_INFINITY -> YearVsYearConfiguration(
                lastYear = BallConfiguration(
                    circleSize = (baseSize / story.yearOverYearChange.toFloat()).coerceAtLeast(8.dp),
                    textFactory = smallTextFactory,
                ),
                thisYear = BallConfiguration(
                    circleSize = baseSize,
                    textFactory = largeTextFactory,
                ),
                offset = DpOffset(maxWidth / 3.5f, maxHeight / 10),
            )

            else -> YearVsYearConfiguration(
                lastYear = BallConfiguration(
                    circleSize = baseSize,
                    textFactory = largeTextFactory,
                ),
                thisYear = BallConfiguration(
                    circleSize = (baseSize * story.yearOverYearChange.toFloat()).coerceAtLeast(8.dp),
                    textFactory = smallTextFactory,
                ),
                offset = DpOffset(maxWidth / 3.5f, maxHeight / 10),
            )
        }
    }

    val scale = remember { Animatable(if (areCirclesVisible) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(350)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                stiffness = 50f,
                dampingRatio = 0.7f,
                visibilityThreshold = 0.001f,
            ),
        )
    }

    YearVsYearBall(
        circleSize = configuration.lastYear.circleSize,
        circleColor = Color.White,
        text = "2023",
        textFactory = configuration.lastYear.textFactory,
        textColor = story.backgroundColor,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(-configuration.offset.x, -configuration.offset.y)
            .rotate(15f)
            .scale(if (forceCirclesVisible) 1f else scale.value),
    )

    YearVsYearBall(
        circleSize = configuration.thisYear.circleSize,
        circleColor = Color.Black,
        text = "2024",
        textColor = story.backgroundColor,
        textFactory = configuration.thisYear.textFactory,
        modifier = Modifier
            .align(Alignment.Center)
            .offset(configuration.offset.x, configuration.offset.y)
            .rotate(-15f)
            .scale(if (forceCirclesVisible) 1f else scale.value),
    )
}

@Composable
private fun YearVsYearBall(
    circleSize: Dp,
    circleColor: Color,
    textFactory: HumaneTextFactory,
    textColor: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    if (textFactory.maxSize <= circleSize * 1.1f) {
        Box(
            modifier = modifier
                .requiredSize(circleSize)
                .background(circleColor, CircleShape),
        ) {
            textFactory.HumaneText(
                text = text,
                color = textColor,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier,
        ) {
            textFactory.HumaneText(
                text = text,
                color = circleColor,
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            Box(
                modifier = Modifier
                    .requiredSize(circleSize)
                    .background(circleColor, CircleShape),
            )
        }
    }
}

private data class YearVsYearConfiguration(
    val lastYear: BallConfiguration,
    val thisYear: BallConfiguration,
    val offset: DpOffset,
)

private data class BallConfiguration(
    val circleSize: Dp,
    val textFactory: HumaneTextFactory,
)

@Composable
private fun TextInfo(
    story: Story.YearVsYear,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Column {
        val badgeId = when (story.subscriptionTier) {
            SubscriptionTier.PLUS -> IR.drawable.end_of_year_2024_year_vs_year_plus_badge
            SubscriptionTier.PATRON -> IR.drawable.end_of_year_2024_year_vs_year_patron_badge
            SubscriptionTier.NONE -> null
        }
        if (badgeId != null) {
            Image(
                painter = painterResource(badgeId),
                contentDescription = null,
                modifier = Modifier.padding(start = 24.dp),
            )
        }
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        val (title, subtitle) = when (val percentage = story.yearOverYearChange) {
            in 0.9..1.1 -> {
                val title = stringResource(LR.string.end_of_year_stories_year_over_year_title_flat, "2023")
                val subtitle = stringResource(LR.string.end_of_year_stories_year_over_year_subtitle_flat)
                title to subtitle
            }

            in 0.0..0.9 -> {
                val title = stringResource(LR.string.end_of_year_stories_year_over_year_title_went_down, "2023")
                val subtitle = stringResource(LR.string.end_of_year_stories_year_over_year_subtitle_went_down)
                title to subtitle
            }

            in 1.1..5.0 -> {
                val percentageString = (100 * percentage).roundToInt().toString()
                val title = stringResource(LR.string.end_of_year_stories_year_over_year_title_went_up, "2023", percentageString)
                val subtitle = stringResource(LR.string.end_of_year_stories_year_over_year_subtitle_went_up, "2025")
                title to subtitle
            }

            else -> {
                val title = stringResource(LR.string.end_of_year_stories_year_over_year_title_went_up_a_lot, "2023")
                val subtitle = stringResource(LR.string.end_of_year_stories_year_over_year_subtitle_went_up, "2025")
                title to subtitle
            }
        }
        TextH10(
            text = title,
            fontScale = measurements.smallDeviceFactor,
            disableAutoScale = true,
            color = colorResource(UR.color.coolgrey_90),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextP40(
            text = subtitle,
            fontSize = 15.sp,
            disableAutoScale = true,
            color = colorResource(UR.color.coolgrey_90),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        ShareStoryButton(
            story = story,
            controller = controller,
            onShare = onShareStory,
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun TotalTimePreview(
    @PreviewParameter(YearVsYearRatioProvider::class) durations: Pair<Int, Int>,
) {
    PreviewBox(currentPage = 8) { measurements ->
        YearVsYearStory(
            story = Story.YearVsYear(
                lastYearDuration = durations.first.hours,
                thisYearDuration = durations.second.hours,
                subscriptionTier = SubscriptionTier.PLUS,
            ),
            measurements = measurements,
            areCirclesVisible = true,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun YearVsYearPatronPreview() {
    PreviewBox(currentPage = 8) { measurements ->
        YearVsYearStory(
            story = Story.YearVsYear(
                lastYearDuration = 100.hours,
                thisYearDuration = 100.hours,
                subscriptionTier = SubscriptionTier.PATRON,
            ),
            measurements = measurements,
            areCirclesVisible = true,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}

private class YearVsYearRatioProvider : PreviewParameterProvider<Pair<Int, Int>> {
    override val values = sequenceOf(
        0 to 1,
        100 to 500,
        100 to 200,
        100 to 120,
        120 to 100,
        200 to 100,
        500 to 100,
        1 to 0,
        0 to 0,
    )
}
