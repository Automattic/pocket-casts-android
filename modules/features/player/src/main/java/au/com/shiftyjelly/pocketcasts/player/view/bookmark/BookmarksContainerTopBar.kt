package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.compose.bars.AppCompatTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
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
    val colors = MaterialTheme.theme.colors
    AppCompatTopAppBar(
        title = stringResource(LR.string.bookmarks),
        navigationButton = if (isDialog) {
            NavigationButton.Drawable(IR.drawable.ic_close, LR.string.close)
        } else {
            NavigationButton.Drawable(IR.drawable.ic_arrow_back, LR.string.back)
        },
        onNavigationClick = onNavigationClick,
        iconColor = if (isDialog) colors.primaryIcon01 else colors.secondaryIcon01,
        titleColor = if (isDialog) colors.primaryText01 else colors.secondaryText01,
        backgroundColor = if (isDialog) colors.primaryUi01 else colors.secondaryUi01,
        bottomShadow = false,
        modifier = modifier,
    )
}
