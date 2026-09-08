package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
@ReadOnlyComposable
fun PlaylistEpisodeSortType.displayLabel() = when (this) {
    PlaylistEpisodeSortType.NewestToOldest -> stringResource(LR.string.sort_newest_to_oldest)
    PlaylistEpisodeSortType.OldestToNewest -> stringResource(LR.string.sort_oldest_to_newest)
    PlaylistEpisodeSortType.ShortestToLongest -> stringResource(LR.string.episode_sort_short_to_long)
    PlaylistEpisodeSortType.LongestToShortest -> stringResource(LR.string.episode_sort_long_to_short)
    PlaylistEpisodeSortType.DragAndDrop -> stringResource(LR.string.episode_sort_custom_order)
}
