package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.Tooltip
import au.com.shiftyjelly.pocketcasts.compose.components.TriangleDirection
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun RecentlyPlayedSortOptionTooltip(
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Tooltip(
        title = stringResource(LR.string.podcasts_sort_by_tooltip_title),
        message = stringResource(LR.string.podcasts_sort_by_tooltip_message),
        onClickClose = onClickClose,
        triangleHorizontalAlignment = Alignment.End,
        triangleDirection = TriangleDirection.Up,
        modifier = modifier
            .padding(horizontal = 8.dp),
    )
}

@Preview
@Composable
private fun RecentlyPlayedSortOptionTooltipPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        RecentlyPlayedSortOptionTooltip(
            onClickClose = {},
        )
    }
}
