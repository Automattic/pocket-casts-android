package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun EndingStory(
    story: Story.Ending,
    measurements: EndOfYearMeasurements,
    onRestartPlayback: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(IR.drawable.navdrawer_logo),
            contentDescription = null,
            modifier = Modifier.size(60.dp)
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextH10(
            text = stringResource(LR.string.end_of_year_story_epilogue_title, 2025),
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
            text = stringResource(LR.string.end_of_year_story_epilogue_subtitle),
            disableAutoScale = true,
            color = colorResource(UR.color.white),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.weight(1f))
        SolidEoyButton(
            text = stringResource(LR.string.end_of_year_replay),
            onClick = onRestartPlayback,
            backgroundColor = colorResource(UR.color.white),
            textColor = colorResource(UR.color.black)
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun EndingPreview() {
    PreviewBox(currentPage = 10) { measurements ->
        EndingStory(
            story = Story.Ending,
            measurements = measurements,
            onRestartPlayback = {},
        )
    }
}
