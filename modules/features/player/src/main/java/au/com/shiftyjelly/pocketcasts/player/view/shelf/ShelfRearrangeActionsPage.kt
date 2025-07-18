package au.com.shiftyjelly.pocketcasts.player.view.shelf

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ShelfRearrangeActionsPage(
    theme: Theme,
    shelfViewModel: ShelfViewModel,
    shelfSharedViewModel: ShelfSharedViewModel,
    playerViewModel: PlayerViewModel,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconColor = Color(ThemeColor.playerContrast01(theme.activeTheme))
    val backgroundColorInt = theme.playerBackground2Color(playerViewModel.podcast)
    val backgroundColor = Color(backgroundColorInt)
    val toolbarColor = Color(theme.playerBackgroundColor(playerViewModel.podcast))
    val selectedColorInt = theme.playerHighlight7Color(playerViewModel.podcast)
    val selectedBackgroundInt = remember(backgroundColorInt, selectedColorInt) { ColorUtils.calculateCombinedColor(backgroundColorInt, selectedColorInt) }

    val shelfItemsState by shelfSharedViewModel.uiState.collectAsStateWithLifecycle()
    val episode by remember {
        playerViewModel.playingEpisodeLive.asFlow()
            .map { (episode, _) -> episode }
            .distinctUntilChangedBy { it.uuid }
    }.collectAsStateWithLifecycle(null)

    Content(
        backgroundColor = backgroundColor,
        toolbarColor = toolbarColor,
        iconColor = iconColor,
        onBackPress = onBackPress,
        modifier = modifier,
    ) {
        val uiState by shelfViewModel.uiState.collectAsState()
        MenuShelfItems(
            state = uiState,
            normalBackgroundColor = Color(backgroundColorInt),
            selectedBackgroundColor = Color(selectedBackgroundInt),
            onMove = { from, to -> shelfViewModel.onShelfItemMove(from, to) },
        )
    }

    LaunchedEffect(shelfItemsState.shelfItems, episode?.uuid) {
        shelfViewModel.setData(shelfItemsState.shelfItems, episode)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (shelfViewModel.uiState.value.isEditable) {
                shelfViewModel.onDismiss()
            }
        }
    }
}

@Composable
private fun Content(
    backgroundColor: Color,
    toolbarColor: Color,
    iconColor: Color,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier,
    menuShelfItems: @Composable () -> Unit,
) {
    Column(modifier = modifier.background(backgroundColor)) {
        ThemedTopAppBar(
            title = stringResource(LR.string.rearrange_actions),
            backgroundColor = toolbarColor,
            textColor = MaterialTheme.theme.colors.playerContrast01,
            iconColor = iconColor,
            bottomShadow = true,
            onNavigationClick = onBackPress,
        )

        menuShelfItems()
    }
}
