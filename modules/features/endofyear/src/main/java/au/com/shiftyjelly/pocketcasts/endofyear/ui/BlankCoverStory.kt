package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import au.com.shiftyjelly.pocketcasts.models.to.Story
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun BlankCoverStory(
    story: Story.BlankCover,
    measurements: EndOfYearMeasurements
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_logo_foreground),
            contentDescription = "Logo",
            tint = Color.White
        )
    }
}