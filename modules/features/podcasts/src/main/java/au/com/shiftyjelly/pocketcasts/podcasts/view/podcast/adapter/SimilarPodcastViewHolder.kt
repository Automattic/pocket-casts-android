package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.podcast.ListPodcastSubscribeRow
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class SimilarPodcastViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {

    fun bind(data: PodcastAdapter.SimilarPodcast) {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                ListPodcastSubscribeRow(
                    uuid = data.podcast.uuid,
                    title = data.podcast.title ?: "",
                    author = data.podcast.author ?: "",
                    subscribed = data.podcast.isSubscribed,
                    onRowClick = data.onRowClick,
                    onSubscribeClick = data.onSubscribeClick,
                )
            }
        }
    }
}
