package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.Banner
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PlaylistsPage(
    uiState: UiState,
    onCreate: () -> Unit,
    onDelete: (PlaylistPreview) -> Unit,
    onShowOptions: () -> Unit,
    onFreeAccountBannerCtaClick: () -> Unit,
    onFreeAccountBannerDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.theme.colors.secondaryUi01)
            .then(modifier),
    ) {
        Toolbar(
            onCreatePlaylist = onCreate,
            onShowOptions = onShowOptions,
        )

        AnimatedVisibility(
            visible = uiState.showFreeAccountBanner,
            enter = BannerEnterTransition,
            exit = BannerExitTransition,
        ) {
            Banner(
                title = stringResource(LR.string.encourage_account_filters_banner_title),
                description = stringResource(LR.string.encourage_account_filters_banner_description),
                actionLabel = stringResource(LR.string.encourage_account_banner_action_label),
                icon = painterResource(IR.drawable.ic_refresh),
                onActionClick = onFreeAccountBannerCtaClick,
                onDismiss = onFreeAccountBannerDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        LazyColumn(
            state = listState,
        ) {
            itemsIndexed(
                items = uiState.playlists,
                key = { _, item -> item.uuid },
            ) { index, playlist ->
                PlaylistPreviewRow(
                    playlist = playlist,
                    showDivider = index != uiState.playlists.lastIndex,
                    onDelete = { onDelete(playlist) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun Toolbar(
    onCreatePlaylist: () -> Unit,
    onShowOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(MaterialTheme.theme.colors.secondaryUi01)
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.h6) {
            Text(
                text = "Playlists",
                color = MaterialTheme.theme.colors.primaryText01,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        Spacer(
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onCreatePlaylist,
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_add_black_24dp),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.primaryIcon01,
            )
        }
        IconButton(
            onClick = onShowOptions,
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_overflow),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.primaryIcon01,
            )
        }
    }
}

private val BannerEnterTransition = fadeIn() + expandVertically()
private val BannerExitTransition = fadeOut() + shrinkVertically()

@Preview
@Composable
private fun PlaylistPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    var uiState by remember {
        mutableStateOf(
            UiState(
                playlists = List(10) { index ->
                    PlaylistPreview(
                        uuid = "uuid-$index",
                        title = "Playlist $index",
                        episodeCount = index,
                        podcasts = emptyList(),
                    )
                },
                showOnboarding = false,
                showFreeAccountBanner = true,
            ),
        )
    }

    AppThemeWithBackground(themeType) {
        PlaylistsPage(
            uiState = uiState,
            onCreate = {},
            onDelete = { playlist ->
                uiState = uiState.copy(playlists = uiState.playlists - playlist)
            },
            onShowOptions = {},
            onFreeAccountBannerCtaClick = {},
            onFreeAccountBannerDismiss = {
                uiState = uiState.copy(showFreeAccountBanner = false)
            },
        )
    }
}
