package au.com.shiftyjelly.pocketcasts.player.view

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.EmptyState
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun UpNextEmptyState(
    onDiscoverTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        title = stringResource(LR.string.player_up_next_empty_title),
        subtitle = stringResource(LR.string.player_up_next_empty_subtitle),
        iconResourcerId = R.drawable.mini_player_upnext,
        buttonText = stringResource(LR.string.go_to_discover),
        onButtonClick = {
            onDiscoverTapped()
        },
        modifier = modifier
            .padding(horizontal = 32.dp, vertical = 20.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun UpNextEmptyStatePreview() {
    UpNextEmptyState(
        onDiscoverTapped = {},
    )
}
