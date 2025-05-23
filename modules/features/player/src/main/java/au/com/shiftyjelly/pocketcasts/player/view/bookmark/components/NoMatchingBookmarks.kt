package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.EmptyState
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun NoMatchingBookmarks(
    modifier: Modifier = Modifier,
) {
    EmptyState(
        title = stringResource(LR.string.podcast_no_bookmarks_found),
        subtitle = stringResource(LR.string.bookmarks_search_results_not_found),
        iconResourceId = R.drawable.ic_bookmark,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun NoMatchingBookmarksPreview() {
    AppTheme(themeType = Theme.ThemeType.LIGHT) {
        NoMatchingBookmarks()
    }
}
