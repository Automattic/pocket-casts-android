package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedCategories

@Composable
fun StoryListenedCategoriesView(
    story: StoryListenedCategories,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(16.dp)) {
        TextH30(
            text = "You listened to ${story.listenedCategories.count()} different categories this year",
            textAlign = TextAlign.Center,
            color = story.tintColor,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        TextH30(
            text = "Let's take a look at some of your favourites..",
            textAlign = TextAlign.Center,
            color = story.tintColor,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}
