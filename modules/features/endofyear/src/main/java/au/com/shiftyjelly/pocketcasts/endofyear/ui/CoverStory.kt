package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.ScrollingColumn
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.to.Story

@Composable
internal fun CoverStory(
    story: Story.Cover,
    measurements: EndOfYearMeasurements,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor),
    ) {
        ScrollingColumn(
            items = listOf(Unit),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlaybackText(
                color = Color(0xFFEEB1F4),
                fontSize = measurements.coverFontSize,
                modifier = Modifier.sizeIn(maxHeight = measurements.coverTextHeight),
            )
        }
        Image(
            painter = painterResource(R.drawable.end_of_year_2024_sticker_2),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 26.dp, y = measurements.closeButtonBottomEdge + 8.dp)
                .size(width = 172.dp, height = 163.dp),
        )
        Image(
            painter = painterResource(R.drawable.end_of_year_2024_sticker_1),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = -24.dp, y = -48.dp)
                .size(width = 250.dp, height = 188.dp),
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun CoverStoryPreview() {
    PreviewBox(currentPage = 0) { measurements ->
        CoverStory(
            story = Story.Cover,
            measurements = measurements,
        )
    }
}
