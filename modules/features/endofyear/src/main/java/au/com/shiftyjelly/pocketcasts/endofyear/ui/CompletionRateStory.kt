package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
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
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun CompletionRateStory(
    story: Story.CompletionRate,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) = CompletionRateStory(
    story = story,
    measurements = measurements,
    controller = controller,
    areBarsVisible = false,
    onShareStory = onShareStory,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CompletionRateStory(
    story: Story.CompletionRate,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    areBarsVisible: Boolean,
    onShareStory: (File) -> Unit,
) {
    Column(
        modifier = Modifier
            .capturable(controller.captureController(story))
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge + 80.dp),
    ) {
        var areVisible by remember { mutableStateOf(areBarsVisible) }
        LaunchedEffect(Unit) {
            delay(350)
            areVisible = true
        }

        BarsSection(
            story = story,
            areBarsVisible = areVisible,
            forceBarsVisible = controller.isSharing,
            modifier = Modifier.weight(1f),
        )
        CompletionRateInfo(
            story = story,
            measurements = measurements,
            controller = controller,
            onShareStory = onShareStory,
        )
    }
}

private val BarHeight = 1.5.dp
private val SpaceHeight = 4.dp

@Composable
private fun BarsSection(
    story: Story.CompletionRate,
    areBarsVisible: Boolean,
    forceBarsVisible: Boolean,
    modifier: Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        val transition = updateTransition(
            targetState = areBarsVisible,
            label = "bars-transition",
        )
        val density = LocalDensity.current
        val baseOffset = density.run { (maxHeight * 1.1f).roundToPx() }
        val barsOffset by transition.animateIntOffset(
            label = "bar-offset",
            transitionSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = 50f,
                    visibilityThreshold = IntOffset(1, 1),
                )
            },
            targetValueByState = { state ->
                when (state) {
                    true -> IntOffset.Zero
                    false -> IntOffset(0, baseOffset)
                }
            },
        )
        val completionOffset by transition.animateIntOffset(
            label = "completion-offset",
            transitionSpec = {
                spring(
                    dampingRatio = lerp(Spring.DampingRatioMediumBouncy, Spring.DampingRatioLowBouncy, story.completionRate.toFloat()),
                    stiffness = lerp(25f, 40f, story.completionRate.toFloat()),
                    visibilityThreshold = IntOffset(1, 1),
                )
            },
            targetValueByState = { state ->
                when (state) {
                    true -> IntOffset.Zero
                    false -> IntOffset(0, (baseOffset * story.completionRate).roundToInt())
                }
            },
        )

        val textFactory = rememberHumaneTextFactory(
            fontSize = 112.nonScaledSp,
        )
        val textAlpha by transition.animateFloat(
            label = "text-alpha",
            transitionSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = 50f,
                )
            },
            targetValueByState = { state ->
                when (state) {
                    true -> 1f
                    false -> 0f
                }
            },
        )

        // Subcomposition is needed because we need accurate measurements.
        // Otherwise completion rate of 100% won't match the bars' height exactly.
        SubcomposeLayout { constraints ->
            var accumulatedHeight = 0
            val bars = ArrayList<Placeable>()
            var index = 0
            while (accumulatedHeight < constraints.maxHeight) {
                val bar = subcompose("bar-$index") {
                    Box(
                        modifier = Modifier
                            .offset { if (forceBarsVisible) IntOffset.Zero else barsOffset }
                            .padding(top = SpaceHeight)
                            .fillMaxWidth(0.58f)
                            .height(BarHeight)
                            .background(Color.Black),
                    )
                }[0].measure(constraints)
                bars += bar
                accumulatedHeight += bar.height
                index++
            }
            if (bars.isNotEmpty()) {
                bars.removeAt(bars.size - 1)
            }
            val spaceHeightPx = LocalDensity.run { SpaceHeight.roundToPx() }
            val barsHeightPx = (bars.sumOf { it.height } - spaceHeightPx).coerceAtLeast(0)

            val completionBar = subcompose("completion-bar") {
                val completedPercent = (story.completionRate * 100).roundToInt()
                val completionHeight = LocalDensity.current.run {
                    (barsHeightPx * story.completionRate).roundToInt().toDp()
                }

                val text = "$completedPercent%"
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .offset { if (forceBarsVisible) IntOffset.Zero else completionOffset }
                        .fillMaxWidth(0.58f)
                        .fillMaxHeight(),
                ) {
                    if (completionHeight >= textFactory.textHeight + 32.dp) {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(completionHeight)
                                .background(Color.Black),
                        ) {
                            textFactory.HumaneText(
                                text = text,
                                color = story.backgroundColor,
                                paddingValues = PaddingValues(bottom = 16.dp, end = 16.dp),
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.wrapContentSize(),
                        ) {
                            textFactory.HumaneText(
                                text = text,
                                color = Color.Black,
                                paddingValues = if (completedPercent == 0) {
                                    PaddingValues()
                                } else {
                                    PaddingValues(bottom = 16.dp)
                                },
                                modifier = Modifier.alpha(if (forceBarsVisible) 1f else textAlpha),
                            )
                            Box(
                                modifier = Modifier
                                    .offset { if (forceBarsVisible) IntOffset.Zero else completionOffset }
                                    .fillMaxWidth()
                                    .height(completionHeight)
                                    .background(Color.Black),
                            )
                        }
                    }
                }
            }[0].measure(constraints)

            layout(constraints.maxWidth, constraints.maxHeight) {
                bars.forEachIndexed { index, bar ->
                    bar.place(
                        x = 0,
                        y = constraints.maxHeight - bar.height * (index + 1),
                    )
                }

                completionBar.place(
                    x = constraints.maxWidth - completionBar.width,
                    y = 0,
                )
            }
        }
    }
}

@Composable
private fun CompletionRateInfo(
    story: Story.CompletionRate,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Column(
        Modifier.background(story.backgroundColor),
    ) {
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        val badgeId = when (story.subscriptionTier) {
            SubscriptionTier.PLUS -> IR.drawable.end_of_year_2024_completion_rate_plus_badge
            SubscriptionTier.PATRON -> IR.drawable.end_of_year_2024_completion_rate_patron_badge
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
        TextH10(
            text = stringResource(
                LR.string.end_of_year_stories_year_completion_rate_title,
                (story.completionRate * 100).roundToInt(),
            ),
            fontScale = measurements.smallDeviceFactor,
            disableAutoScale = true,
            color = colorResource(UR.color.coolgrey_90),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextP40(
            text = stringResource(
                LR.string.end_of_year_stories_year_completion_rate_subtitle,
                story.listenedCount,
                story.completedCount,
            ),
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
private fun CompletionRatePreview(
    @PreviewParameter(CompletedCountProvider::class) count: Int,
) {
    PreviewBox(currentPage = 9) { measurements ->
        CompletionRateStory(
            story = Story.CompletionRate(
                listenedCount = 100,
                completedCount = count,
                subscriptionTier = SubscriptionTier.PATRON,
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            areBarsVisible = true,
            onShareStory = {},
        )
    }
}

private class CompletedCountProvider : PreviewParameterProvider<Int> {
    override val values = sequenceOf(0, 12, 56, 100)
}
