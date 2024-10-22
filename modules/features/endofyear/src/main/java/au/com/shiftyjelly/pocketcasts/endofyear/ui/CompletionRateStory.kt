package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun CompletionRateStory(
    story: Story.CompletionRate,
    measurements: EndOfYearMeasurements,
) = CompletionRateStory(story, measurements, showBars = false)

@Composable
private fun CompletionRateStory(
    story: Story.CompletionRate,
    measurements: EndOfYearMeasurements,
    showBars: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge + 80.dp),
    ) {
        var areBarsVisible by remember { mutableStateOf(showBars) }
        LaunchedEffect(Unit) {
            delay(350)
            areBarsVisible = true
        }

        BarsSection(
            story = story,
            showBars = areBarsVisible,
            modifier = Modifier.weight(1f),
        )
        CompletionRateInfo(
            story = story,
            measurements = measurements,
        )
    }
}

private val BarHeight = 1.5.dp
private val SpaceHeight = 4.dp

@Composable
private fun BarsSection(
    story: Story.CompletionRate,
    showBars: Boolean,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        // Subcomposition is needed because we need accurate measurements.
        // Otherwise completion rate of 100% won't match the bars' height exactly.
        AnimatedVisibility(
            visible = showBars,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = CubicBezierEasing(0f, 0f, 0.58f, 1.0f),
                ),
            ),
        ) {
            SubcomposeLayout { constraints ->
                var accumulatedHeight = 0
                val bars = ArrayList<Placeable>()
                var index = 0
                while (accumulatedHeight < constraints.maxHeight) {
                    val bar = subcompose("bar-$index") {
                        Box(
                            modifier = Modifier
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
                    bars.removeLast()
                }
                val spaceHeightPx = LocalDensity.run { SpaceHeight.roundToPx() }
                val barsHeightPx = (bars.sumOf { it.height } - spaceHeightPx).coerceAtLeast(0)

                val completionBar = subcompose("completion-bar") {
                    val textFactory = rememberHumaneTextFactory(
                        fontSize = 112.nonScaledSp,
                    )
                    val completedPercent = (story.completionRate * 100).roundToInt()
                    val completionHeight = LocalDensity.current.run {
                        (barsHeightPx * story.completionRate).roundToInt().toDp()
                    }

                    val text = "$completedPercent%"
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .fillMaxWidth(0.58f)
                            .fillMaxHeight(),
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(completionHeight)
                                .background(Color.Black),
                        ) {
                            if (completionHeight > textFactory.textHeight + 32.dp) {
                                textFactory.HumaneText(
                                    text = text,
                                    color = story.backgroundColor,
                                    paddingValues = PaddingValues(bottom = 16.dp, end = 16.dp),
                                )
                            }
                        }

                        if (completionHeight < textFactory.textHeight + 32.dp) {
                            textFactory.HumaneText(
                                text = text,
                                color = Color.Black,
                                paddingValues = if (completedPercent == 0) {
                                    PaddingValues()
                                } else {
                                    PaddingValues(bottom = 16.dp)
                                },
                                modifier = Modifier.offset(y = -completionHeight),
                            )
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
}

@Composable
private fun CompletionRateInfo(
    story: Story.CompletionRate,
    measurements: EndOfYearMeasurements,
) {
    Column {
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        val badgeId = when (story.subscriptionTier) {
            SubscriptionTier.PLUS -> IR.drawable.end_of_year_2024_completion_rate_plus_badge
            SubscriptionTier.PATRON -> IR.drawable.end_of_year_2024_completion_rate_patron_badge
            SubscriptionTier.NONE, null -> null
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
        ShareStoryButton(onClick = {})
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
            showBars = true,
        )
    }
}

private class CompletedCountProvider : PreviewParameterProvider<Int> {
    override val values = sequenceOf(0, 12, 56, 100)
}
