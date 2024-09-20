package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun NoBookmarksInSearchView(
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Content(
        onClick = onActionClick,
        modifier = modifier,
    )
}

@Composable
private fun Content(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageView(
        titleView = {
            TextH20(
                text = stringResource(LR.string.podcast_no_bookmarks_found),
                color = MaterialTheme.theme.colors.primaryText01,
            )
        },
        message = stringResource(LR.string.bookmarks_search_results_not_found),
        buttonTitle = stringResource(LR.string.clear_search),
        buttonAction = onClick,
        style = MessageViewColors.Default,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun NoBookmarksInSearchPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Content(
            onClick = {},
        )
    }
}
