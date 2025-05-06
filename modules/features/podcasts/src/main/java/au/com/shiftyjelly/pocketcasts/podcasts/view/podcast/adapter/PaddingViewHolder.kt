package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter

class PaddingViewHolder(
    private val composeView: ComposeView,
) : RecyclerView.ViewHolder(composeView) {

    fun bind(data: PodcastAdapter.PaddingRow) {
        composeView.setContent {
            Box(modifier = Modifier.height(data.padding))
        }
    }
}
