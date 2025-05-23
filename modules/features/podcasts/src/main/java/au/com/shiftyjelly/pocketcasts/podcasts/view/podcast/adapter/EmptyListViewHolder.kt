package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class EmptyListViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {
    fun bind(emptyList: PodcastAdapter.EmptyList) {
        composeView.setContentWithViewCompositionStrategy {
            AppTheme(theme.activeTheme) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    NoContentBanner(
                        title = emptyList.title,
                        body = emptyList.subtitle,
                        iconResourceId = emptyList.iconResourceId,
                        primaryButtonText = emptyList.buttonText,
                        onPrimaryButtonClick = emptyList.onButtonClick,
                        modifier = Modifier.padding(top = 56.dp, bottom = 16.dp),
                    )
                }
            }
        }
    }
}
