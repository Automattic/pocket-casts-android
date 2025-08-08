package au.com.shiftyjelly.pocketcasts.playlists.edit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun SmartPlaylistOptionsColumn(
    sortType: PlaylistEpisodeSortType,
    hasEpisodes: Boolean,
    onClickSelectAll: () -> Unit,
    onClickSortBy: () -> Unit,
    onClickDownloadAll: () -> Unit,
    onClickChromecast: () -> Unit,
    onClickOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        if (hasEpisodes) {
            OptionRow(
                iconId = IR.drawable.ic_playlists_select_episode,
                primaryText = stringResource(LR.string.playlist_select_episodes),
                onClick = onClickSelectAll,
            )
        }
        OptionRow(
            iconId = IR.drawable.ic_playlists_sort,
            primaryText = stringResource(LR.string.playlist_sort_by),
            secondaryText = sortType.displayLabel(),
            onClick = onClickSortBy,
        )
        if (hasEpisodes) {
            OptionRow(
                iconId = IR.drawable.ic_playlists_download_all,
                primaryText = stringResource(LR.string.playlist_download_all),
                onClick = onClickDownloadAll,
            )
        }
        OptionRow(
            iconId = IR.drawable.ic_chrome_cast,
            primaryText = stringResource(LR.string.chromecast),
            onClick = onClickChromecast,
        )
        OptionRow(
            iconId = IR.drawable.ic_playlists_options,
            primaryText = stringResource(LR.string.playlist_options),
            showDivider = false,
            onClick = onClickOpenSettings,
        )
    }
}

@Composable
private fun OptionRow(
    @DrawableRes iconId: Int,
    primaryText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    secondaryText: String? = null,
) {
    Column(
        modifier = modifier
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp, horizontal = 20.dp),
        ) {
            Icon(
                painter = painterResource(iconId),
                tint = MaterialTheme.theme.colors.primaryIcon01,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(
                modifier = Modifier.size(24.dp),
            )
            TextH30(
                text = primaryText,
                modifier = Modifier.weight(1f),
            )
            if (secondaryText != null) {
                TextH50(
                    text = secondaryText,
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.theme.colors.primaryUi05),
            )
        } else {
            Spacer(
                modifier = Modifier.height(1.dp),
            )
        }
    }
}

@Preview
@Composable
private fun SmartPlaylistOptionsColumnPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SmartPlaylistOptionsColumn(
            sortType = PlaylistEpisodeSortType.NewestToOldest,
            hasEpisodes = true,
            onClickSelectAll = {},
            onClickSortBy = {},
            onClickDownloadAll = {},
            onClickChromecast = {},
            onClickOpenSettings = {},
        )
    }
}
