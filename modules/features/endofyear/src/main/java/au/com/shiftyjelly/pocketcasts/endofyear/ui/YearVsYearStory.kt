package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.endofyear.ui.components.HeaderText
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.models.to.Story.YearVsYear.Trend
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.hours
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun YearVsYearStory(
    story: Story.YearVsYear,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .capturable(controller.captureController(story))
                .fillMaxSize()
                .background(story.backgroundColor),
            contentAlignment = Alignment.TopCenter,
        ) {
            YearVsYearAnimation(
                trend = story.trend,
                isSharing = controller.isSharing,
                story = story,
            )

            TextInfo(
                story = story,
                measurements = measurements,
            )
        }
        ShareStoryButton(
            story = story,
            controller = controller,
            onShare = onShareStory,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun BoxScope.YearVsYearAnimation(
    trend: Trend,
    isSharing: Boolean,
    story: Story.YearVsYear,
) {
    val animationId = when (trend) {
        Trend.Same -> R.raw.playback_year_vs_year_same_lottie
        Trend.Down -> R.raw.playback_year_vs_year_down_lottie
        Trend.Up -> R.raw.playback_year_vs_year_up_lottie
        Trend.UpALot -> R.raw.playback_year_vs_year_up_a_lot_lottie
    }
    // the lottie animations use the Inter font, for the text to load we need to provide a replacement font or it doesn't show
    val fontMap = remember {
        runCatching {
            mapOf("Inter" to Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        }
            .onFailure { e -> Timber.e(e, "Failed to load font for Lottie animation") }
            .getOrElse { emptyMap() }
    }
    val lastYearHours = story.lastYearDuration.inWholeHours
    val thisYearHours = story.thisYearDuration.inWholeHours
    val lastYearText = "$lastYearHours ${pluralStringResource(id = LR.plurals.hour, count = lastYearHours.toInt())}"
    val thisYearText = "$thisYearHours ${pluralStringResource(id = LR.plurals.hour, count = thisYearHours.toInt())}"
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.TEXT,
            value = lastYearText,
            keyPath = arrayOf("hours_2024"),
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.TEXT,
            value = thisYearText,
            keyPath = arrayOf("hours_2025"),
        ),
    )
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(animationId),
        imageAssetsFolder = "lottie-images",
    )
    if (isSharing) {
        LottieAnimation(
            contentScale = ContentScale.Crop,
            composition = composition,
            alignment = Alignment.BottomCenter,
            progress = { 1f },
            fontMap = fontMap,
            dynamicProperties = dynamicProperties,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    } else {
        LottieAnimation(
            contentScale = ContentScale.Crop,
            composition = composition,
            applyOpacityToLayers = true,
            alignment = Alignment.BottomCenter,
            fontMap = fontMap,
            dynamicProperties = dynamicProperties,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun TextInfo(
    story: Story.YearVsYear,
    measurements: EndOfYearMeasurements,
) {
    val title = when (story.trend) {
        Trend.Same -> stringResource(LR.string.end_of_year_stories_year_over_year_title_flat, EndOfYearManager.YEAR_TO_SYNC.value.toString())
        Trend.Down -> {
            val percentageString = story.percentageChange.absoluteValue.toString()
            stringResource(LR.string.end_of_year_stories_year_over_year_title_went_down, percentageString)
        }
        Trend.Up -> {
            val percentageString = story.percentageChange.toString()
            stringResource(LR.string.end_of_year_stories_year_over_year_title_went_up, (EndOfYearManager.YEAR_TO_SYNC.value - 1).toString(), percentageString)
        }
        Trend.UpALot -> stringResource(LR.string.end_of_year_stories_year_over_year_title_went_up_a_lot)
    }

    val subtitle = when (story.trend) {
        Trend.Same -> stringResource(LR.string.end_of_year_stories_year_over_year_subtitle_flat)
        Trend.Down -> stringResource(LR.string.end_of_year_stories_year_over_year_subtitle_went_down)
        Trend.Up -> stringResource(LR.string.end_of_year_stories_year_over_year_subtitle_went_up)
        Trend.UpALot -> stringResource(LR.string.end_of_year_stories_year_over_year_subtitle_went_up_a_lot)
    }

    HeaderText(
        title = title,
        subtitle = subtitle,
        subscriptionTier = story.subscriptionTier,
        titleMaxLines = 2,
        measurements = measurements,
    )
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun TotalTimePreview(
    @PreviewParameter(YearVsYearRatioProvider::class) durations: Pair<Int, Int>,
) {
    PreviewBox(currentPage = 8) { measurements ->
        YearVsYearStory(
            story = Story.YearVsYear(
                lastYearDuration = durations.first.hours,
                thisYearDuration = durations.second.hours,
                subscriptionTier = SubscriptionTier.Plus,
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun YearVsYearPatronPreview() {
    PreviewBox(currentPage = 8) { measurements ->
        YearVsYearStory(
            story = Story.YearVsYear(
                lastYearDuration = 100.hours,
                thisYearDuration = 100.hours,
                subscriptionTier = SubscriptionTier.Patron,
            ),
            measurements = measurements,
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
