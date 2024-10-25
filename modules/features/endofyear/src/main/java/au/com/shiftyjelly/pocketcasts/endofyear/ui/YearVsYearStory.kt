package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
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
    onShareStory: () -> Unit,
) = YearVsYearStory(
    story = story,
    measurements = measurements,
    showCircles = false,
    onShareStory = onShareStory,
)

@Composable
private fun YearVsYearStory(
    story: Story.YearVsYear,
    measurements: EndOfYearMeasurements,
    showCircles: Boolean,
    onShareStory: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge + 8.dp),
    ) {
        ComparisonSection(
            story = story,
            measurements = measurements,
            showCircles = showCircles,
        )
        TextInfo(
            story = story,
            measurements = measurements,
            onShareStory = onShareStory,
        )
    }
}

@Composable
private fun ColumnScope.ComparisonSection(
    story: Story.YearVsYear,
    measurements: EndOfYearMeasurements,
    showCircles: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        if (story.yearOverYearChange in 0.9..1.1) {
            SameListeningTime(measurements)
        } else {
            DifferentListeningTime(story, measurements, showCircles)
        }
    }
}

@Composable
private fun BoxScope.SameListeningTime(
    measurements: EndOfYearMeasurements,
) {
    val fontSize = 150.nonScaledSp * measurements.smallDeviceFactor
    // Humane font has a lot bottom space
    // Precalculate texts' heights to properly size them
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val textHeight = remember {
        density.run {
            measurer.measure(
                text = "2023",
                style = TextStyle(
                    fontFamily = humaneFontFamily,
                    fontSize = fontSize,
                ),
            ).firstBaseline.toDp()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.CenterStart)
            .offset(x = measurements.width / 8, -measurements.width / 8)
            .rotate(15f),
    ) {
        Text(
            text = "2023",
            fontSize = fontSize,
            fontFamily = humaneFontFamily,
            color = Color.White,
            modifier = Modifier.requiredHeight(textHeight),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color.White, CircleShape),
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .offset(x = -measurements.width / 8, measurements.width / 20)
            .rotate(-15f),
    ) {
        Text(
            text = "2024",
            fontSize = fontSize,
            fontFamily = humaneFontFamily,
            color = Color.Black,
            modifier = Modifier.requiredHeight(textHeight),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color.Black, CircleShape),
        )
    }
}

@Composable
private fun BoxScope.DifferentListeningTime(
    story: Story.YearVsYear,
    measurements: EndOfYearMeasurements,
    showCircles: Boolean,
) {
    val (smallText, largeText) = if (story.thisYearDuration > story.lastYearDuration) {
        "2023" to "2024"
    } else {
        "2024" to "2023"
    }
    val smallFontSize = 128.nonScaledSp * measurements.smallDeviceFactor
    val largeFontSize = 150.nonScaledSp * measurements.smallDeviceFactor
    // Humane font has a lot bottom space
    // Precalculate texts' heights to properly size them
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val smallTextHeight = remember {
        density.run {
            measurer.measure(
                text = smallText,
                style = TextStyle(
                    fontFamily = humaneFontFamily,
                    fontSize = smallFontSize,
                ),
            ).firstBaseline.toDp()
        }
    }
    val largeTextHeight = remember {
        density.run {
            measurer.measure(
                text = largeText,
                style = TextStyle(
                    fontFamily = humaneFontFamily,
                    fontSize = largeFontSize,
                ),
            ).firstBaseline.toDp()
        }
    }

    val smallSize = 292.dp * measurements.scale * measurements.smallDeviceFactor
    val largeSize = 348.dp * measurements.scale * measurements.smallDeviceFactor
    val configuration = if (smallText == "2023") {
        YearVsYearConfiguration(
            lastYearFontSize = smallFontSize,
            lastYearTextHeight = smallTextHeight,
            lastYearSize = smallSize,
            lastYearOffset = DpOffset(x = -smallSize / 5, y = -smallSize / 5),
            thisYearFontSize = largeFontSize,
            thisYearTextHeight = largeTextHeight,
            thisYearSize = largeSize,
            thisYearOffset = DpOffset(x = largeSize / 4, largeSize / 10),
        )
    } else {
        YearVsYearConfiguration(
            lastYearFontSize = largeFontSize,
            lastYearTextHeight = largeTextHeight,
            lastYearSize = largeSize,
            lastYearOffset = DpOffset(x = -largeSize / 4, -largeSize / 10),
            thisYearFontSize = smallFontSize,
            thisYearTextHeight = smallTextHeight,
            thisYearSize = smallSize,
            thisYearOffset = DpOffset(x = smallSize / 5, y = smallSize / 5),
        )
    }

    val scale = remember { Animatable(if (showCircles) 1f else 0f) }
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

    Box(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .scale(scale.value)
            .offset(configuration.lastYearOffset.x, configuration.lastYearOffset.y)
            .requiredSize(configuration.lastYearSize)
            .background(Color.White, CircleShape),
    ) {
        Text(
            text = "2023",
            fontSize = configuration.lastYearFontSize,
            fontFamily = humaneFontFamily,
            color = story.backgroundColor,
            modifier = Modifier
                .align(Alignment.Center)
                .requiredHeight(configuration.lastYearTextHeight)
                .rotate(15f),
        )
    }

    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .scale(scale.value)
            .offset(configuration.thisYearOffset.x, configuration.thisYearOffset.y)
            .requiredSize(configuration.thisYearSize)
            .background(Color.Black, CircleShape),
    ) {
        Text(
            text = "2024",
            fontSize = configuration.thisYearFontSize,
            fontFamily = humaneFontFamily,
            color = story.backgroundColor,
            modifier = Modifier
                .align(Alignment.Center)
                .requiredHeight(configuration.thisYearTextHeight)
                .rotate(-15f),
        )
    }
}

private class YearVsYearConfiguration(
    val lastYearFontSize: TextUnit,
    val lastYearTextHeight: Dp,
    val lastYearSize: Dp,
    val lastYearOffset: DpOffset,
    val thisYearFontSize: TextUnit,
    val thisYearTextHeight: Dp,
    val thisYearSize: Dp,
    val thisYearOffset: DpOffset,
)

@Composable
private fun TextInfo(
    story: Story.YearVsYear,
    measurements: EndOfYearMeasurements,
    onShareStory: () -> Unit,
) {
    Column(
        modifier = Modifier.background(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.1f to story.backgroundColor,
            ),
        ),
    ) {
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
        ShareStoryButton(onClick = onShareStory)
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun YearVsYearThisYearPreview() {
    PreviewBox(currentPage = 8) { measurements ->
        YearVsYearStory(
            story = Story.YearVsYear(
                lastYearDuration = 2.hours,
                thisYearDuration = 3.hours,
                subscriptionTier = SubscriptionTier.PLUS,
            ),
            measurements = measurements,
            showCircles = true,
            onShareStory = {},
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun YearVsYearThisYearLargePreview() {
    PreviewBox(currentPage = 8) { measurements ->
        YearVsYearStory(
            story = Story.YearVsYear(
                lastYearDuration = 0.hours,
                thisYearDuration = 3.hours,
                subscriptionTier = SubscriptionTier.PATRON,
            ),
            measurements = measurements,
            showCircles = true,
            onShareStory = {},
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun YearVsYearLastYearPreview() {
    PreviewBox(currentPage = 8) { measurements ->
        YearVsYearStory(
            story = Story.YearVsYear(
                lastYearDuration = 3.hours,
                thisYearDuration = 2.hours,
                subscriptionTier = SubscriptionTier.PLUS,
            ),
            measurements = measurements,
            showCircles = true,
            onShareStory = {},
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun YearVsYearEqualPreview() {
    PreviewBox(currentPage = 8) { measurements ->
        YearVsYearStory(
            story = Story.YearVsYear(
                lastYearDuration = 100.hours,
                thisYearDuration = 101.hours,
                subscriptionTier = SubscriptionTier.PLUS,
            ),
            measurements = measurements,
            showCircles = true,
            onShareStory = {},
        )
    }
}
