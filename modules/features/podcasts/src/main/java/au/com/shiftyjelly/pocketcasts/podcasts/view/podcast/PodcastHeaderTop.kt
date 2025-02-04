package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PodcastHeaderArtwork(
    podcast: Podcast,
    onPodcastClick: () -> Unit,
    onPodcastLongClick: () -> Unit,
    modifier: Modifier = Modifier,

) {
    Card(
        modifier = modifier
            .clipToBounds()
            .clip(RoundedCornerShape(8.dp))
            .shadow(elevation = 8.dp)
            .combinedClickable(
                onClick = { onPodcastClick.invoke() },
                onLongClick = { onPodcastLongClick.invoke() },
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = 8.dp,
        contentColor = Color.Transparent,
    ) {
        PodcastImage(
            uuid = podcast.uuid,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
