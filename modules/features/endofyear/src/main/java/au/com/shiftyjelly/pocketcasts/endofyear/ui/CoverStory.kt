package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.models.to.Story
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun CoverStory(
    story: Story.Cover,
    measurements: EndOfYearMeasurements,
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(IR.raw.playback_intro_lottie),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor),
    ) {
        AnimatedVisibility(
            enter = fadeIn(),
            visible = progress > .5f,
        ) {
            TextH30(
                fontScale = measurements.smallDeviceFactor,
                fontSize = 25.sp,
                lineHeight = 28.sp,
                text = stringResource(LR.string.eoy_playback_intro_title),
                modifier = Modifier.padding(horizontal = 42.dp),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
        LottieAnimation(
            contentScale = ContentScale.Crop,
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun CoverStoryPreview() {
    PreviewBox(currentPage = 0) { measurements ->
        CoverStory(
            story = Story.Cover,
            measurements = measurements,
        )
    }
}
