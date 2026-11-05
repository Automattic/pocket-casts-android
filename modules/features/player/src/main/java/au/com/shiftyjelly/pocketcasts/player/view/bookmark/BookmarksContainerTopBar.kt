package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun BookmarksContainerTopBar(
    isDialog: Boolean,
    onNavigationClick: () -> Unit,
    multiSelectTopBar: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (multiSelectTopBar != null) {
        multiSelectTopBar()
        return
    }
    ThemedTopAppBar(
        title = stringResource(LR.string.bookmarks),
        navigationButton = if (isDialog) NavigationButton.Close else NavigationButton.Back,
        onNavigationClick = onNavigationClick,
        style = if (isDialog) ThemedTopAppBar.Style.Immersive else ThemedTopAppBar.Style.Solid,
        modifier = modifier,
    )
}
