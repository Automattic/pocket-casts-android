package au.com.shiftyjelly.pocketcasts.playlists

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
internal fun PlaylistsPage(
    uiState: UiState,
    onCreate: () -> Unit,
    onDelete: (PlaylistPreview) -> Unit,
    onShowOptions: () -> Unit,
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

@Preview
@Composable
private fun PlaylistPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    val uiState = remember {
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
        )
    }

    AppThemeWithBackground(themeType) {
        PlaylistsPage(
            uiState = uiState,
            onCreate = {},
            onDelete = {},
            onShowOptions = {},
        )
    }
}
