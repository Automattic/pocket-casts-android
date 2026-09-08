package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
@ReadOnlyComposable
fun EpisodesSortType.displayLabel() = when (this) {
    EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> stringResource(LR.string.episode_sort_title_a_z)
    EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> stringResource(LR.string.episode_sort_title_z_a)
    EpisodesSortType.EPISODES_SORT_BY_DATE_DESC -> stringResource(LR.string.episode_sort_newest_to_oldest)
    EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> stringResource(LR.string.episode_sort_oldest_to_newest)
    EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> stringResource(LR.string.episode_sort_short_to_long)
    EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> stringResource(LR.string.episode_sort_long_to_short)
}
