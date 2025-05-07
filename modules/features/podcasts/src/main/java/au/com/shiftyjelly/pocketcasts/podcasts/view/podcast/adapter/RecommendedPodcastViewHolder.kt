package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.podcast.ListPodcastSubscribeRow
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class RecommendedPodcastViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {

    fun bind(data: PodcastAdapter.RecommendedPodcast) {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                val uuid = data.podcast.uuid
                ListPodcastSubscribeRow(
                    uuid = uuid,
                    title = data.podcast.title ?: "",
                    author = data.podcast.author ?: "",
                    subscribed = data.podcast.isSubscribed,
                    onRowClick = { data.onRowClick(uuid, data.listDate) },
                    onSubscribeClick = { data.onSubscribeClick(uuid, data.listDate) },
                )
            }
        }
    }
}
