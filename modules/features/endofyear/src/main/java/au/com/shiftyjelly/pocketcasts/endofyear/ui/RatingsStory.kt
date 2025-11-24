package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.endofyear.ui.components.HeaderText
import au.com.shiftyjelly.pocketcasts.models.to.Rating
import au.com.shiftyjelly.pocketcasts.models.to.RatingStats
import au.com.shiftyjelly.pocketcasts.models.to.Story
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun RatingsStory(
    story: Story.Ratings,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
    onLearnAboutRatings: () -> Unit,
) {
    val maxRatingCount = story.stats.max().second
    val modifier = Modifier
        .fillMaxSize()
        .background(story.backgroundColor)
        .padding(top = measurements.closeButtonBottomEdge + 16.dp)
    if (maxRatingCount != 0) {
        Box {
            PresentRatings(
                modifier = Modifier
                    .capturable(controller.captureController(story))
                    .then(modifier),
                story = story,
                measurements = measurements,
                controller = controller,
            )
            ShareStoryButton(
                story = story,
                controller = controller,
                onShare = onShareStory,
                modifier = Modifier
                    .padding(bottom = 18.dp)
                    .align(alignment = Alignment.BottomCenter),
            )
        }
    } else {
        AbsentRatings(
            modifier = modifier,
            measurements = measurements,
            onLearnAboutRatings = onLearnAboutRatings,
        )
    }
}

@Composable
private fun PresentRatings(
    story: Story.Ratings,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        val title = when (val rating = story.stats.max().first) {
            Rating.One, Rating.Two, Rating.Three -> stringResource(LR.string.eoy_story_ratings_title_2)
            Rating.Four, Rating.Five -> stringResource(LR.string.eoy_story_ratings_title_1, rating.numericalValue)
        }
        val subtitle = when (val rating = story.stats.max().first) {
            Rating.One, Rating.Two, Rating.Three -> stringResource(LR.string.eoy_story_ratings_subtitle_2)
            Rating.Four, Rating.Five -> stringResource(LR.string.eoy_story_ratings_subtitle_1, rating.numericalValue)
        }
        HeaderText(
            title = title,
            subtitle = subtitle,
            modifier = Modifier
                .fillMaxWidth(),
            measurements = measurements,
        )
        RatingBars(
            stats = story.stats,
            forceBarsVisible = controller.isSharing,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.85f to Color.Transparent,
                            1f to story.backgroundColor,
                        ),
                    )
                },
        )
    }
}

@Composable
private fun RatingBars(
    stats: RatingStats,
    forceBarsVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        Rating.entries.forEach { rating ->
            val barRange = (stats.relativeToMax(rating) * 10).roundToInt()
            AnimatedRatingBar(
                rating = rating.numericalValue,
                heightRange = barRange,
                forceBarVisible = forceBarsVisible,
            )
        }
    }
}

@Composable
private fun RowScope.AnimatedRatingBar(
    rating: Int,
    heightRange: Int,
    forceBarVisible: Boolean,
) = Box(modifier = Modifier.weight(1f)) {
    val pillarComposition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.pillar_bars_i3),
    )
    val numberComposition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.pillar_numbers_i3),
    )

    val pillarAnimatable = rememberLottieAnimatable()
    val numberAnimatable = rememberLottieAnimatable()
    val isPreview = LocalInspectionMode.current
    val freezeBar = forceBarVisible || isPreview

    pillarComposition?.let { comp ->
        val markerIndex = comp.markers.size - heightRange
        val targetMarker = comp.markers.find { it.name == "marker_$markerIndex" } ?: comp.markers.firstOrNull()

        LaunchedEffect(targetMarker) {
            val clipSpec = if (targetMarker == null) {
                LottieClipSpec.Frame(min = 0, max = 1)
            } else {
                LottieClipSpec.Marker(marker = targetMarker.name)
            }

            pillarAnimatable.animate(
                composition = pillarComposition,
                clipSpec = clipSpec,
            )
        }

        LaunchedEffect(freezeBar, targetMarker) {
            if (freezeBar && targetMarker != null) {
                val endProgress = (targetMarker.startFrame + targetMarker.durationFrames) / comp.durationFrames
                pillarAnimatable.snapTo(comp, endProgress)
            }
        }
    }

    numberComposition?.let { comp ->
        val markerIndex = comp.markers.size - heightRange
        val targetMarker = comp.markers.find { it.name == "marker_$markerIndex" } ?: comp.markers.firstOrNull()

        LaunchedEffect(targetMarker) {
            val clipSpec = if (targetMarker == null) {
                LottieClipSpec.Frame(min = 0, max = 1)
            } else {
                LottieClipSpec.Marker(marker = targetMarker.name)
            }

            numberAnimatable.animate(
                composition = numberComposition,
                clipSpec = clipSpec,
            )
        }

        LaunchedEffect(freezeBar, targetMarker) {
            if (freezeBar && targetMarker != null) {
                val endProgress = (targetMarker.startFrame + targetMarker.durationFrames) / comp.durationFrames
                numberAnimatable.snapTo(comp, endProgress)
            }
        }
    }

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.TEXT,
            value = rating.toString(),
            "main number",
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = colorResource(UR.color.white).toArgb(),
            "main number",
        ),
    )

    LottieAnimation(
        modifier = Modifier.fillMaxSize(),
        composition = pillarComposition,
        progress = { pillarAnimatable.progress },
        contentScale = ContentScale.FillBounds,
    )

    LottieAnimation(
        modifier = Modifier.fillMaxSize(),
        composition = numberComposition,
        progress = { numberAnimatable.progress },
        dynamicProperties = dynamicProperties,
        fontMap = remember {
            mapOf(
                "Inter-Regular" to Typeface.create("sans-serif", Typeface.NORMAL),
            )
        },
    )
}

@Composable
private fun AbsentRatings(
    measurements: EndOfYearMeasurements,
    onLearnAboutRatings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextH10(
            text = stringResource(LR.string.eoy_story_ratings_title_no_ratings),
            fontScale = measurements.smallDeviceFactor,
            disableAutoScale = true,
            fontSize = 25.sp,
            color = colorResource(UR.color.white),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextP40(
            text = stringResource(LR.string.eoy_story_ratings_subtitle_no_ratings),
            disableAutoScale = true,
            color = colorResource(UR.color.white),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            repeat(5) { index ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextP40(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        text = (index + 1).toString(),
                        textAlign = TextAlign.Center,
                        color = colorResource(UR.color.white),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(color = Color(0xFFFF4562)),
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        SolidEoyButton(
            text = stringResource(LR.string.eoy_story_ratings_learn_button_label),
            onClick = onLearnAboutRatings,
            backgroundColor = colorResource(UR.color.white),
            textColor = colorResource(UR.color.black),
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun RatingsHighPreview() {
    PreviewBox(currentPage = 4) { measurements ->
        RatingsStory(
            story = Story.Ratings(
                stats = RatingStats(
                    ones = 20,
                    twos = 30,
                    threes = 0,
                    fours = 25,
                    fives = 130,
                ),
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
            onLearnAboutRatings = {},
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun RatingsLowPreview() {
    PreviewBox(currentPage = 4) { measurements ->
        RatingsStory(
            story = Story.Ratings(
                stats = RatingStats(
                    ones = 20,
                    twos = 50,
                    threes = 0,
                    fours = 25,
                    fives = 0,
                ),
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
            onLearnAboutRatings = {},
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun RatingsNonePreview() {
    PreviewBox(currentPage = 4) { measurements ->
        RatingsStory(
            story = Story.Ratings(
                stats = RatingStats(
                    ones = 0,
                    twos = 0,
                    threes = 0,
                    fours = 0,
                    fives = 0,
                ),
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
            onLearnAboutRatings = {},
        )
    }
}
