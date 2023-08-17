package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.NoBookmarksView
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.NoBookmarksViewColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class NoBookmarkViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
    private val onHeadsetSettingsClicked: () -> Unit,
) : RecyclerView.ViewHolder(composeView) {
    fun bind() {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                NoBookmarksView(
                    style = NoBookmarksViewColors.Default,
                    openFragment = { onHeadsetSettingsClicked() }
                )
            }
        }
    }
}
