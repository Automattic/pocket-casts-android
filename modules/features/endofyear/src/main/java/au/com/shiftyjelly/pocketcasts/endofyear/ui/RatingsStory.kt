package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
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
import kotlin.math.max
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun RatingsStory(
    story: Story.Ratings,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
    onLearnAboutRatings: () -> Unit,
) {
    val maxRatingCount = story.stats.max().second
    if (maxRatingCount != 0) {
        PresentRatings(
            story = story,
            measurements = measurements,
            controller = controller,
            onShareStory = onShareStory,
        )
    } else {
        AbsentRatings(
            modifier = Modifier
                .background(color = story.backgroundColor)
                .fillMaxSize()
                .padding(top = measurements.closeButtonBottomEdge + 20.dp),
            story = story,
            measurements = measurements,
            onLearnAboutRatings = onLearnAboutRatings,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PresentRatings(
    story: Story.Ratings,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Column(
        modifier = Modifier
            .capturable(controller.captureController(story))
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge + 20.dp),
    ) {
        TextH10(
            text = when (val rating = story.stats.max().first) {
                Rating.One, Rating.Two, Rating.Three -> stringResource(LR.string.eoy_story_ratings_title_2)
                Rating.Four, Rating.Five -> stringResource(LR.string.eoy_story_ratings_title_1, rating.numericalValue)
            },
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
            text = when (val rating = story.stats.max().first) {
                Rating.One, Rating.Two, Rating.Three -> stringResource(LR.string.eoy_story_ratings_subtitle_2)
                Rating.Four, Rating.Five -> stringResource(LR.string.eoy_story_ratings_subtitle_1, rating.numericalValue)
            },
            disableAutoScale = true,
            color = colorResource(UR.color.white),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            RatingBars(
                stats = story.stats,
                forceBarsVisible = controller.isSharing,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp)
            )
            ShareStoryButton(
                story = story,
                controller = controller,
                onShare = onShareStory,
            )
        }
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
            AnimatedRatingBar(
                rating = rating.numericalValue,
                heightRange = (stats.relativeToMax(rating) * 10).toInt(),
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
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(IR.raw.playback_story_ratings_pillar_lottie)
    )

    val animatable = rememberLottieAnimatable()
    val isPreview = LocalInspectionMode.current
    val freezeBar = forceBarVisible || isPreview

    composition?.let { comp ->
        val targetMarker = if (heightRange > 0) {
            val markerIndex = max(1, comp.markers.size - heightRange)
            comp.markers.find { it.name == "marker_$markerIndex" } ?: comp.markers.lastOrNull()
        } else {
            null
        }

        LaunchedEffect(targetMarker, animatable) {
            val clipSpec = if (targetMarker == null) {
                LottieClipSpec.Frame(min = 0, max = 1)
            } else {
                LottieClipSpec.Marker(marker = targetMarker.name)
            }

            animatable.animate(
                composition = composition,
                clipSpec = clipSpec
            )
        }

        LaunchedEffect(freezeBar, targetMarker, animatable) {
            if (freezeBar && targetMarker != null) {
                val endProgress = (targetMarker.startFrame + targetMarker.durationFrames) / comp.durationFrames
                animatable.snapTo(comp, endProgress)
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
            "main number"
        )
    )

    LottieAnimation(
        modifier = Modifier.weight(1f),
        composition = composition,
        progress = { animatable.progress },
        dynamicProperties = dynamicProperties,
        fontMap = remember {
            mapOf(
                "Inter-Regular" to Typeface.create("sans-serif", Typeface.NORMAL)
            )
        },
    )
}

@Composable
private fun AbsentRatings(
    modifier: Modifier = Modifier,
    story: Story.Ratings,
    measurements: EndOfYearMeasurements,
    onLearnAboutRatings: () -> Unit,
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
        RatingBars(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            stats = story.stats,
            forceBarsVisible = false,
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        SolidEoyButton(
            text = stringResource(LR.string.eoy_story_ratings_learn_button_label),
            onClick = onLearnAboutRatings,
            backgroundColor = colorResource(UR.color.white),
            textColor = colorResource(UR.color.black)
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
