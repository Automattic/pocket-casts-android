package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastItem
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryTopFivePodcasts

@Composable
fun StoryTopFivePodcastsView(
    story: StoryTopFivePodcasts,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        TextH30(
            text = "Your Top Podcasts",
            textAlign = TextAlign.Center,
            color = story.tintColor,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
        story.topPodcasts.forEach { topPodcast ->
            PodcastItem(
                podcast = topPodcast.toPodcast(),
                onClick = null,
                tintColor = story.tintColor,
                showDivider = false
            )
        }
    }
}
