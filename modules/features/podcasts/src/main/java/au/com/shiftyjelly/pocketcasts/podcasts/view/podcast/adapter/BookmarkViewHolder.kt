package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bookmark.BookmarkItem
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class BookmarkViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {

    fun bind(
        bookmark: Bookmark,
        onBookmarkPlayClicked: (Bookmark) -> Unit,
    ) {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                BookmarkItem(
                    bookmark = bookmark,
                    onPlayClick = { onBookmarkPlayClicked(it) },
                )
            }
        }
    }
}
