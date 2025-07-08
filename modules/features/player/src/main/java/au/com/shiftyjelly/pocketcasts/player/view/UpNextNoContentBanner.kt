package au.com.shiftyjelly.pocketcasts.player.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun UpNextNoContentBanner(
    onDiscoverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NoContentBanner(
        title = stringResource(LR.string.player_up_next_empty_title),
        body = stringResource(LR.string.player_up_next_empty_subtitle),
        iconResourceId = R.drawable.mini_player_upnext,
        primaryButtonText = stringResource(LR.string.go_to_discover),
        onPrimaryButtonClick = onDiscoverClick,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun UpNextNoContentBannerPreview() {
    UpNextNoContentBanner(
        onDiscoverClick = {},
    )
}
