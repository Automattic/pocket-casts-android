package au.com.shiftyjelly.pocketcasts.search.adapter

import androidx.compose.foundation.background
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastItem
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class SearchPodcastViewHolder(
    val composeView: ComposeView,
    val theme: Theme,
    val onPodcastClick: (Podcast) -> Unit,
) : RecyclerView.ViewHolder(composeView) {

    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
    }

    fun bind(podcast: Podcast) {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                PodcastItem(
                    podcast = podcast,
                    subscribed = podcast.isSubscribed,
                    showSubscribed = true,
                    onClick = { onPodcastClick(podcast) },
                    modifier = Modifier
                        .background(color = MaterialTheme.theme.colors.primaryUi01)
                )
            }
        }
    }
}
