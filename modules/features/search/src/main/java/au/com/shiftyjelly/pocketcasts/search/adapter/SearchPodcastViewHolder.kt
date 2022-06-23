package au.com.shiftyjelly.pocketcasts.search.adapter

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.search.component.SearchPodcastRow
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class SearchPodcastViewHolder(
    val composeView: ComposeView,
    val theme: Theme,
    val onPodcastClick: (Podcast) -> Unit
) : RecyclerView.ViewHolder(composeView) {

    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
    }

    fun bind(podcast: Podcast) {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                SearchPodcastRow(podcast = podcast, subscribed = podcast.isSubscribed, onClick = { onPodcastClick(podcast) })
            }
        }
    }
}
