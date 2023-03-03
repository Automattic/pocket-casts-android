package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.podcast.PodcastSubscribeImage
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

private val PodcastItemIconSize = 156.dp

@Composable
fun SearchPodcastItem(
    podcast: Podcast,
    onClick: (() -> Unit)?,
    onSubscribeClick: ((Podcast) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .width(PodcastItemIconSize + 16.dp)
            .then(if (onClick == null) Modifier else Modifier.clickable { onClick() })
            .padding(8.dp)
    ) {
        PodcastSubscribeImage(
            podcastUuid = podcast.uuid,
            podcastTitle = "",
            podcastSubscribed = podcast.isSubscribed,
            onSubscribeClick = if (onSubscribeClick != null) {
                { onSubscribeClick(podcast) }
            } else {
                null
            },
            subscribeButtonSize = 32.dp,
            shadowSize = 0.dp,
            subscribeOnPodcastTap = false
        )

        Spacer(Modifier.height(8.dp))

        TextH40(
            text = podcast.title,
            maxLines = 1,
        )
        TextH50(
            text = podcast.author,
            maxLines = 1,
            color = MaterialTheme.theme.colors.primaryText02
        )
    }
}
