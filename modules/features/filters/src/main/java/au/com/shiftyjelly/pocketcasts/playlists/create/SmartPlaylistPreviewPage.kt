package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType

@Composable
fun SmartPlaylistPreviewPage(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    )
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun SmartPlaylistPreviewPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SmartPlaylistPreviewPage(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
