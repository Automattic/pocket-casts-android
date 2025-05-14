package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.DividerSubTitle
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class DividerSubTitleViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {

    fun bind(data: PodcastAdapter.DividerSubTitleRow) {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                DividerSubTitle(
                    title = data.title,
                    icon = data.icon,
                    onClick = data.onClick,
                )
            }
        }
    }
}
