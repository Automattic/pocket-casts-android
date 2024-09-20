package au.com.shiftyjelly.pocketcasts.compose.components

import android.graphics.Color
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

@Composable
fun WebView(
    url: String,
    modifier: Modifier = Modifier,
    forceDark: Boolean = false,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                isScrollbarFadingEnabled = true
                isVerticalScrollBarEnabled = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    settings.isAlgorithmicDarkeningAllowed = forceDark
                } else {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                        @Suppress("DEPRECATION")
                        if (forceDark) {
                            WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON)
                        } else {
                            WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF)
                        }
                    }
                }
                setBackgroundColor(Color.TRANSPARENT)
            }
        },
        update = { webView ->
            webView.loadUrl(url)
        },
    )
}
