package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun NoContentScreen(@StringRes title: Int, @StringRes message: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeaderChip(text = title)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(message),
                color = MaterialTheme.colors.onPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
private fun NoContentScreenPreview() {
    WearAppTheme {
        NoContentScreen(
            title = LR.string.downloads,
            message = LR.string.no_episodes,
        )
    }
}
