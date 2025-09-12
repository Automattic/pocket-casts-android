package au.com.shiftyjelly.pocketcasts.playlists.component

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
import androidx.compose.runtime.ReadOnlyComposable
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
import au.com.shiftyjelly.pocketcasts.compose.components.TextC50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
@ReadOnlyComposable
internal fun PlaylistEpisodeSortType.displayLabel() = when (this) {
    PlaylistEpisodeSortType.NewestToOldest -> stringResource(LR.string.sort_newest_to_oldest)
    PlaylistEpisodeSortType.OldestToNewest -> stringResource(LR.string.sort_oldest_to_newest)
    PlaylistEpisodeSortType.ShortestToLongest -> stringResource(LR.string.episode_sort_short_to_long)
    PlaylistEpisodeSortType.LongestToShortest -> stringResource(LR.string.episode_sort_long_to_short)
    PlaylistEpisodeSortType.DragAndDrop -> stringResource(LR.string.episode_sort_custom_order)
}

@Composable
internal fun PlaylistSortOptionsColumn(
    selectedSortType: PlaylistEpisodeSortType,
    onSelectSortType: (PlaylistEpisodeSortType) -> Unit,
    modifier: Modifier = Modifier,
    availableSortTypes: List<PlaylistEpisodeSortType> = PlaylistEpisodeSortType.entries,
) {
    Column(
        modifier = modifier,
    ) {
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextC50(
            text = stringResource(LR.string.sort_by).uppercase(),
            color = MaterialTheme.theme.colors.support01,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        availableSortTypes.forEachIndexed { index, type ->
            SortTypeRow(
                type = type,
                isSelected = type == selectedSortType,
                onClick = { onSelectSortType(type) },
                showDivider = index != PlaylistEpisodeSortType.entries.lastIndex,
            )
        }
    }
}

@Composable
private fun SortTypeRow(
    type: PlaylistEpisodeSortType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
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
            TextH30(
                text = type.displayLabel(),
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Spacer(
                    modifier = Modifier.size(24.dp),
                )
                Icon(
                    painter = painterResource(IR.drawable.ic_check),
                    tint = MaterialTheme.theme.colors.primaryIcon01,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
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
private fun SortOptionsColumnPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        PlaylistSortOptionsColumn(
            selectedSortType = PlaylistEpisodeSortType.NewestToOldest,
            onSelectSortType = {},
        )
    }
}
