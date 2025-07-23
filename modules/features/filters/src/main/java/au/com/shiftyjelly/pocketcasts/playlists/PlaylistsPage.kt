package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.playlists.PlaylistsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
internal fun PlaylistsPage(
    uiState: UiState,
    onDelete: (PlaylistPreview) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
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
            onDelete = {},
        )
    }
}
