package au.com.shiftyjelly.pocketcasts.endofyear.storyviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryFake2

@Composable
fun StoryFake2View(
    story: StoryFake2,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(16.dp)) {
        TextH30(
            text = "The longest episode you listened to was ${story.episode.title}",
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}
