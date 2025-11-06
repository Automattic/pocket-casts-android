package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
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
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun CompletionRateStory(
    story: Story.CompletionRate,
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

        ) {
            val animationId = when (story.completionRate) {
                in 0f..0.3f -> R.raw.playback_completion_rate_20p_lottie
                in 0.3f..0.5f -> R.raw.playback_completion_rate_40p_lottie
                in 0.5f..0.7f -> R.raw.playback_completion_rate_60p_lottie
                in 0.7f..0.9f -> R.raw.playback_completion_rate_80p_lottie
                else -> R.raw.playback_completion_rate_100p_lottie
            }

            val composition by rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(animationId),
            )
            if (controller.isSharing) {
                LottieAnimation(
                    contentScale = ContentScale.Crop,
                    composition = composition,
                    alignment = Alignment.BottomCenter,
                    progress = { 1f },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            } else {
                LottieAnimation(
                    contentScale = ContentScale.Crop,
                    composition = composition,
                    alignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
            HeaderText(
                title = stringResource(
                    LR.string.end_of_year_stories_year_completion_rate_title,
                    (story.completionRate * 100).roundToInt(),
                ),
                subtitle = stringResource(
                    LR.string.end_of_year_stories_year_completion_rate_subtitle,
                    story.completedCount,
                    story.listenedCount,
                ),
                subscriptionTier = story.subscriptionTier,
                measurements = measurements,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                story.backgroundColor.copy(alpha = 0f),
                                story.backgroundColor,
                            ),
                        ),
                    ),
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

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun CompletionRatePreview(
    @PreviewParameter(CompletedCountProvider::class) count: Int,
) {
    PreviewBox(currentPage = 9) { measurements ->
        CompletionRateStory(
            story = Story.CompletionRate(
                listenedCount = 100,
                completedCount = count,
                subscriptionTier = SubscriptionTier.Patron,
            ),
            measurements = measurements,
            controller = StoryCaptureController.preview(),
            onShareStory = {},
        )
    }
}

private class CompletedCountProvider : PreviewParameterProvider<Int> {
    override val values = sequenceOf(0, 12, 56, 100)
}
