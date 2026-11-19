package au.com.shiftyjelly.pocketcasts.views.activity

import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.theme

@Composable
internal fun WebViewPage(
    title: String,
    isLoading: Boolean,
    onNavigationClick: () -> Unit,
    onCreateWebView: (Context) -> WebView,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ThemedTopAppBar(
            title = title,
            navigationButton = NavigationButton.Back,
            onNavigationClick = onNavigationClick,
            iconColor = MaterialTheme.theme.colors.primaryInteractive01,
            textColor = MaterialTheme.theme.colors.primaryText01,
            backgroundColor = MaterialTheme.theme.colors.secondaryUi01,
            bottomShadow = true,
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                factory = onCreateWebView,
                modifier = Modifier.fillMaxSize(),
            )
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.theme.colors.primaryInteractive01,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp),
                )
            }
        }
    }
}
