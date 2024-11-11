package au.com.shiftyjelly.pocketcasts.endofyear.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.AutoResizeText
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.endofyear.StoryCaptureController
import au.com.shiftyjelly.pocketcasts.localization.helper.FriendlyDurationUnit
import au.com.shiftyjelly.pocketcasts.localization.helper.toFriendlyString
import au.com.shiftyjelly.pocketcasts.models.to.Story
import dev.shreyaspatil.capturable.capturable
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

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
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge),
    ) {
        val texts = getListeningTimeTexts(LocalContext.current, story.duration)
        SubcomposeLayout { constraints ->
            val subtitle = subcompose("subtitle") {
                TextH10(
                    text = texts.subtitle,
                    color = colorResource(UR.color.coolgrey_90),
                    fontScale = measurements.smallDeviceFactor,
                    disableAutoScale = true,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }[0].measure(constraints)

            val shareButton = subcompose("share-button") {
                ShareStoryButton(
                    story = story,
                    controller = controller,
                    onShare = onShareStory,
                )
            }[0].measure(constraints)

            val titleConstraints = constraints.copy(
                maxHeight = constraints.maxHeight - shareButton.height - subtitle.height,
            )
            val title = subcompose("title") {
                AutoResizeText(
                    text = texts.mainNumber,
                    fontFamily = humaneFontFamily,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    color = Color.Black,
                    // Humane font uses about 15% of its height for the ascent line
                    // We need to adjust height for it in order to center the text as we
                    // do not use any characters that go to the ascent line
                    heightFactor = 0.84f,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }[0].measure(titleConstraints)

            layout(constraints.maxWidth, constraints.maxHeight) {
                title.place(
                    x = (titleConstraints.maxWidth - title.width) / 2,
                    y = (titleConstraints.maxHeight - title.height) / 2,
                )
                subtitle.place(
                    x = 0,
                    y = constraints.maxHeight - shareButton.height - subtitle.height,
                )
                shareButton.place(
                    x = 0,
                    y = constraints.maxHeight - shareButton.height,
                )
            }
        }
        Image(
            painter = painterResource(IR.drawable.end_of_year_2024_sticker_6),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 20.dp)
                .size(
                    width = 197.dp * measurements.scale,
                    height = 165.dp * measurements.scale,
                ),
        )
    }
}

@Composable
private fun getListeningTimeTexts(
    context: Context,
    duration: Duration,
): ListeningTimeTexts {
    val (mainNumber, subtitle) = remember(duration, context) {
        val timeText = duration.toFriendlyString(
            resources = context.resources,
            maxPartCount = 3,
            minUnit = FriendlyDurationUnit.Minute,
            maxUnit = if (duration < 100.hours) FriendlyDurationUnit.Hour else FriendlyDurationUnit.Day,
        )
        val timeTextStrings = timeText.split(" ")
        val mainNumber = timeTextStrings.firstOrNull() ?: ""
        val subtitle = timeTextStrings.drop(1).joinToString(
            separator = " ",
            postfix = " ${context.getString(LR.string.end_of_year_listening_time_subtitle)}",
        )
        mainNumber to subtitle
    }
    return ListeningTimeTexts(mainNumber, subtitle)
}

private data class ListeningTimeTexts(
    val mainNumber: String,
    val subtitle: String,
)

@Preview(device = Devices.PortraitRegular)
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
