package au.com.shiftyjelly.pocketcasts.filters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.Tooltip
import au.com.shiftyjelly.pocketcasts.compose.components.TriangleDirection
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun FiltersTooltip(
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Tooltip(
        title = stringResource(LR.string.filters_tooltip_title),
        message = stringResource(LR.string.filters_tooltip_subtitle),
        onClickClose = onClickClose,
        triangleHorizontalAlignment = Alignment.End,
        triangleDirection = TriangleDirection.Up,
        modifier = modifier
            .padding(horizontal = 8.dp),
    )
}

@Preview
@Composable
private fun PodcastHeaderTooltipPreview() {
    AppTheme(Theme.ThemeType.INDIGO) {
        Box(
            modifier = Modifier
                .background(Color.White)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(16.dp),
        ) {
            FiltersTooltip(
                onClickClose = {},
            )
        }
    }
}
