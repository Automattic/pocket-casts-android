package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import android.R.attr.top
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
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
                    modifier = Modifier
                        .padding(
                            top = if (data.isFirst) 12.dp else 0.dp,
                            bottom = if (data.isLast) 12.dp else 0.dp,
                        ),
                )
            }
        }
    }
}
