package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.foundation.background
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
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
                    sourceView = SourceView.PODCAST_SCREEN,
                    openFragment = { onHeadsetSettingsClicked() },
                    modifier = Modifier
                        .background(color = MaterialTheme.theme.colors.primaryUi02),
                )
            }
        }
    }
}
