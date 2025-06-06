package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bookmark.BookmarkRow
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class BookmarkViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {

    fun bind(data: PodcastAdapter.BookmarkItemData) {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                BookmarkRow(
                    bookmark = data.bookmark,
                    episode = data.episode,
                    isSelecting = data.isMultiSelecting(),
                    isSelected = data.isSelected(data.bookmark),
                    showIcon = true,
                    useEpisodeArtwork = data.useEpisodeArtwork,
                    showEpisodeTitle = true,
                    onPlayClick = { data.onBookmarkPlayClicked(data.bookmark) },
                    modifier = Modifier
                        .pointerInput(data.bookmark.adapterId) {
                            detectTapGestures(
                                onLongPress = { data.onBookmarkRowLongPress(data.bookmark) },
                                onTap = { data.onBookmarkRowClick(data.bookmark, bindingAdapterPosition) },
                            )
                        },
                )
            }
        }
    }
}
