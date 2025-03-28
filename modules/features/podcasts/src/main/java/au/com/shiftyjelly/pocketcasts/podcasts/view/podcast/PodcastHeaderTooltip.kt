package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

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
internal fun PodcastHeaderTooltip(
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Tooltip(
        title = stringResource(LR.string.podcast_header_redesign_title),
        message = stringResource(LR.string.podcast_header_redesign_message),
        onClickClose = onClickClose,
        triangleHorizontalAlignment = Alignment.CenterHorizontally,
        triangleDirection = TriangleDirection.Down,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun PodcastHeaderTooltipPreview() {
    AppTheme(Theme.ThemeType.ELECTRIC) {
        Box(
            modifier = Modifier
                .background(Color.White)
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(16.dp),
        ) {
            PodcastHeaderTooltip(
                onClickClose = {},
            )
        }
    }
}
