package au.com.shiftyjelly.pocketcasts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun TvTabPlaceholder(
    tab: TvTab,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        TextH10(
            text = stringResource(tab.contentDescriptionRes),
            color = Color.White,
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvTabPlaceholderPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvTabPlaceholder(tab = TvTab.Home)
        }
    }
}
