package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bookmark.BookmarkRow
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

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
                    onArtworkClick = if (FeatureFlag.isEnabled(Feature.SMART_BOOKMARKS)) {
                        { data.onBookmarkArtworkClick() }
                    } else {
                        null
                    },
                    onClick = { data.onBookmarkRowClick(data.bookmark, bindingAdapterPosition) },
                    onLongClick = { data.onBookmarkRowLongPress(data.bookmark) },
                    onShareClick = { swipeState -> data.onBookmarkSwipeShare(data.bookmark, swipeState::settle) },
                    onDeleteClick = { swipeState -> data.onBookmarkSwipeDelete(data.bookmark, swipeState::settle) },
                )
            }
        }
    }
}
