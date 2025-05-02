package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.DividerLine
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class DividerLineViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {

    fun bind() {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                DividerLine()
            }
        }
    }
}
