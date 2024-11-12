package au.com.shiftyjelly.pocketcasts.player.view.shelf

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.app.MediaRouteButton
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.Pill
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ShelfBottomSheetPage(
    shelfViewModel: ShelfViewModel,
    playerViewModel: PlayerViewModel,
    onEditButtonClick: () -> Unit,
    onShelfItemClick: (item: ShelfItem, enabled: Boolean) -> Unit,
) {
    val trimmedShelf by remember {
        playerViewModel.trimmedShelfLive.asFlow()
            .map { it.copy(it.first.drop(4), it.second) }
    }.collectAsStateWithLifecycle(null)
    var performMediaRouteButtonClick by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    trimmedShelf?.let { (shelfItems, episode) ->
        Content(
            onEditButtonClick = {
                shelfViewModel.onEditButtonClick()
                onEditButtonClick()
            },
            mediaRouteButton = {
                MediaRouteButton(
                    performClick = performMediaRouteButtonClick,
                    onMediaRouteButtonClick = {
                        shelfViewModel.onMediaRouteButtonClick()
                    },
                )
            },
        ) {
            MenuShelfItems(
                shelfViewModel = shelfViewModel,
                onClick = { item, enabled ->
                    coroutineScope.launch {
                        if (item == ShelfItem.Cast) {
                            performMediaRouteButtonClick = true
                            delay(100) // allow perform action to complete before dismissing the bottom sheet
                        }
                        onShelfItemClick(item, enabled)
                    }
                },
            )
        }
        LaunchedEffect(shelfItems, episode) {
            shelfViewModel.setData(shelfItems, episode)
        }
    }
}

@Composable
private fun Content(
    onEditButtonClick: () -> Unit,
    mediaRouteButton: @Composable () -> Unit,
    menuShelfItems: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Spacer(Modifier.height(8.dp))

        Pill(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .background(MaterialTheme.theme.colors.playerContrast01),
        )

        Spacer(Modifier.height(8.dp))

        mediaRouteButton()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
        ) {
            TextH30(
                text = stringResource(LR.string.player_more_actions),
                color = MaterialTheme.theme.colors.playerContrast01,
            )

            IconButton(
                onClick = onEditButtonClick,
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_edit),
                    contentDescription = stringResource(LR.string.edit),
                    tint = MaterialTheme.theme.colors.playerContrast01,
                )
            }
        }
        menuShelfItems()
    }
}

@Composable
private fun MediaRouteButton(
    performClick: Boolean,
    onMediaRouteButtonClick: () -> Unit,
) {
    AndroidView(
        factory = { context ->
            MediaRouteButton(context).apply {
                visibility = View.GONE
                setOnClickListener {
                    onMediaRouteButtonClick()
                }
                CastButtonFactory.setUpMediaRouteButton(context, this)
            }
        },
        update = { view ->
            if (performClick) {
                view.performClick()
            }
        },
    )
}

@Preview
@Composable
private fun ShelfBottomSheetPageContentPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Content(
            onEditButtonClick = {},
            mediaRouteButton = {},
            menuShelfItems = {},
        )
    }
}
