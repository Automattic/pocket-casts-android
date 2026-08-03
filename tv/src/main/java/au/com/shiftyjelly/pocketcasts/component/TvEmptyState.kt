package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun TvEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    autoFocusAction: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    if (autoFocusAction && onAction != null) {
        LaunchedEffect(Unit) {
            withFrameNanos {}
            runCatching { focusRequester.requestFocus() }
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = TvTextStyles.ScreenTitle,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = TvColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 400.dp),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onAction,
                    colors = TvButtonDefaults.filledButtonColors(),
                    modifier = if (autoFocusAction) Modifier.focusRequester(focusRequester) else Modifier,
                ) {
                    Text(actionLabel, style = TvTextStyles.ModalButtonLabel)
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvEmptyStatePreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize().background(TvColors.Dark)) {
                TvEmptyState(
                    title = "Nothing here yet",
                    subtitle = "Content will show up here once it's available.",
                    actionLabel = "Get the app",
                    onAction = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
