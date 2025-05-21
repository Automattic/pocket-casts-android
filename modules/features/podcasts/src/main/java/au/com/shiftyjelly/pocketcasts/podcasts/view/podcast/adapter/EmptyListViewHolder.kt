package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.EmptyState
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class EmptyListViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {
    fun bind(emptyList: PodcastAdapter.EmptyList) {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                EmptyState(
                    title = emptyList.title,
                    subtitle = emptyList.subtitle,
                    iconResourceId = emptyList.iconResourceId,
                    primaryButtonText = emptyList.buttonText,
                    onPrimaryButtonClick = emptyList.onButtonClick,
                    modifier = Modifier.padding(top = 56.dp),
                )
            }
        }
    }
}
