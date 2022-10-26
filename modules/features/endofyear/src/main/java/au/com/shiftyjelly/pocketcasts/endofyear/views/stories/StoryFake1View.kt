package au.com.shiftyjelly.pocketcasts.endofyear.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastItem
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryFake1

@Composable
fun StoryFake1View(
    story: StoryFake1,
    modifier: Modifier = Modifier,
) {
    Column {
        TextH30(
            text = "Your Top Podcasts",
            textAlign = TextAlign.Center,
            color = story.tintColor,
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        LazyColumn(modifier = modifier.fillMaxWidth()) {
            items(story.podcasts.size) { index ->
                PodcastItem(
                    podcast = story.podcasts[index],
                    onClick = {},
                    tintColor = story.tintColor,
                    showDivider = false
                )
            }
        }
    }
}
