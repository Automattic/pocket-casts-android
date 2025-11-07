package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.models.to.Story
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun TotalTimeStory(
    story: Story.TotalTime,
    measurements: EndOfYearMeasurements,
    controller: StoryCaptureController,
    onShareStory: (File) -> Unit,
) {
    Box(
        modifier = Modifier
            .capturable(controller.captureController(story))
            .fillMaxSize()
            .background(story.backgroundColor),
    ) {
        val totalMinutes = story.duration.inWholeMinutes
        var animatedNumber by remember { mutableLongStateOf((totalMinutes - 1000).coerceAtLeast(0)) }

        LaunchedEffect(totalMinutes) {
            val startValue = (totalMinutes - 1000).coerceAtLeast(0)
            val endValue = totalMinutes
            val animatable = Animatable(startValue.toFloat())

            animatable.animateTo(
                targetValue = endValue.toFloat(),
                animationSpec = tween(durationMillis = 2000),
            ) {
                animatedNumber = this.value.toLong()
            }
        }

        val mask by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(IR.raw.total_time_mask),
        )
        val text by rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(IR.raw.total_time_text),
        )

        val dynamicProperties = rememberLottieDynamicProperties(
            rememberLottieDynamicProperty(
                property = LottieProperty.TEXT,
                value = animatedNumber.toString(),
                "content",
                "40,456",
            ),
            rememberLottieDynamicProperty(
                property = LottieProperty.TEXT,
                value = stringResource(LR.string.end_of_year_listening_time_subtitle),
                "content",
                "minutes listened",
            ),
        )

        val context = LocalContext.current
        LottieAnimation(
            composition = mask,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        LottieAnimation(
            composition = text,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            dynamicProperties = dynamicProperties,
            fontMap = remember {
                mapOf(
                    "Inter-Medium" to Typeface.create("sans-serif", Typeface.NORMAL),
                    "Humane-SemiBold" to Typeface.createFromAsset(context.assets, "fonts/humane_semibold.otf"),
                )
            },
        )

        ShareStoryButton(
            modifier = Modifier
                .padding(bottom = 18.dp)
                .align(Alignment.BottomCenter),
            story = story,
            controller = controller,
            onShare = onShareStory,
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun TotalTimePreview(
    @PreviewParameter(PlaybackTimeProvider::class) duration: Duration,
) {
    PreviewBox(currentPage = 5) { measurements ->
        TotalTimeStory(
            story = Story.TotalTime(
                duration = duration,
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}

private class PlaybackTimeProvider : PreviewParameterProvider<Duration> {
    override val values = sequenceOf(
        Duration.ZERO,
        99.hours + 12.minutes + 6.seconds,
        36.days + 12.hours + 30.minutes + 10.seconds,
        22.hours + 30.minutes + 10.seconds,
        120.days + 12.hours + 30.minutes + 10.seconds,
        120.hours + 30.minutes + 10.seconds,
        1452.days + 17.hours, // Theoretically possible with playback effects
    )
}
