package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.EpisodeSearchView
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter.BookmarkHeader
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class BookmarkHeaderViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
) : RecyclerView.ViewHolder(composeView) {
    fun bind(bookmarkHeader: BookmarkHeader) {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.theme.colors.primaryUi02)
                ) {
                    SearchHeader(bookmarkHeader)
                    Divider(color = MaterialTheme.theme.colors.primaryUi05)
                    if (bookmarkHeader.bookmarksCount > 0) {
                        BookmarksCountView(bookmarkHeader)
                    }
                }
            }
        }
    }
    @Composable
    private fun SearchHeader(bookmarkHeader: BookmarkHeader) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                SearchView(
                    bookmarkHeader = bookmarkHeader,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            IconButton(
                onClick = { bookmarkHeader.onOptionsClicked() },
                modifier = Modifier
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_more_vert_black_24dp),
                    contentDescription = stringResource(LR.string.more_options),
                    tint = MaterialTheme.theme.colors.primaryIcon02,
                )
            }
        }
    }
    @Composable
    fun SearchView(
        bookmarkHeader: BookmarkHeader,
        modifier: Modifier = Modifier,
    ) {
        val hintText = stringResource(id = LR.string.bookmarks_search)
        AndroidView(
            modifier = modifier,
            factory = { context ->
                EpisodeSearchView(context).apply {
                    val searchText = findViewById<EditText>(R.id.searchText)
                    searchText.hint = hintText
                    this.onFocus = { bookmarkHeader.onSearchFocus() }
                    onSearch = { query ->
                        bookmarkHeader.onSearchQueryChanged(query)
                    }
                    text = bookmarkHeader.searchTerm
                }
            },
        )
    }
    @Composable
    private fun BookmarksCountView(bookmarkHeader: BookmarkHeader) {
        TextP60(
            text = if (bookmarkHeader.bookmarksCount > 1) {
                stringResource(LR.string.bookmarks_plural, bookmarkHeader.bookmarksCount)
            } else {
                stringResource(LR.string.bookmarks_singular)
            },
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = Modifier
                .padding(start = 16.dp)
                .padding(vertical = 14.dp)
        )
    }
}
