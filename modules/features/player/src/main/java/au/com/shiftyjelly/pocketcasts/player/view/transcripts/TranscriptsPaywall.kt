package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
internal fun TranscriptsPaywall() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null, interactionSource = null, onClick = {}),
    ) {
        TextH10(text = "Paywall")
    }
}

@Preview
@Composable
private fun TranscriptsPaywallPreview() {
    AppThemeWithBackground(
        themeType = Theme.ThemeType.DARK,
    ) {
        TranscriptsPaywall()
    }
}
