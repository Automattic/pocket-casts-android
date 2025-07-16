package au.com.shiftyjelly.pocketcasts.transcripts.ui

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import com.kevinnzou.web.LoadingState
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewState

@Composable
internal fun TranscriptWebView(
    transcript: Transcript.Web,
    modifier: Modifier = Modifier,
    theme: TranscriptTheme = TranscriptTheme.default(MaterialTheme.theme.colors),
) {
    val materialTheme = MaterialTheme.theme
    val isDarkOrPlayer = materialTheme.isDark || materialTheme.rememberPlayerColors() != null

    val state = rememberWebViewState(transcript.url)
    Box(
        modifier = modifier,
    ) {
        WebView(
            state = state,
            onCreated = { webView ->
                if (isDarkOrPlayer) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        webView.settings.isAlgorithmicDarkeningAllowed = true
                    } else {
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                            @Suppress("DEPRECATION")
                            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
                        }
                    }
                }
            },
        )

        when (state.loadingState) {
            is LoadingState.Initializing, is LoadingState.Loading -> {
                LoadingView(
                    color = theme.primaryText,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            LoadingState.Finished -> Unit
        }
    }
}
