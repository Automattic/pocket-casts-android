package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastLogoWhite
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryEpilogue
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val HeartImageSize = 72.dp
private const val BackgroundColor = 0xFF1A1A1A

@Composable
fun StoryEpilogueView(
    story: StoryEpilogue,
    onReplayClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(Color(BackgroundColor))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = modifier.height(40.dp))

        Spacer(modifier = modifier.weight(1f))

        HeartImage()

        Spacer(modifier = modifier.weight(0.34f))

        PrimaryText(story)

        Spacer(modifier = modifier.weight(0.16f))

        SecondaryText(story)

        Spacer(modifier = modifier.weight(0.32f))

        ReplayButton(onClick = onReplayClicked)

        Spacer(modifier = modifier.weight(1f))

        PodcastLogoWhite()

        Spacer(modifier = modifier.height(40.dp))
    }
}

@Composable
private fun HeartImage(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id = R.drawable.heart),
        contentDescription = null,
        modifier = modifier
            .size(HeartImageSize)
    )
}

@Composable
private fun PrimaryText(
    story: StoryEpilogue,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(id = LR.string.end_of_year_story_epilogue_title)
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryEpilogue,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(id = LR.string.end_of_year_story_epilogue_subtitle)
    StorySecondaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun ReplayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onClick() },
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            hoveredElevation = 0.dp,
            focusedElevation = 0.dp,
        ),
        colors = ButtonDefaults
            .buttonColors(
                backgroundColor = Color.Transparent,
                contentColor = Color.Transparent
            ),
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = Color.White
        )
        TextP40(
            text = stringResource(id = LR.string.end_of_year_replay),
            color = Color.White,
            modifier = modifier.padding(2.dp)
        )
    }
}
