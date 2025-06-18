package au.com.shiftyjelly.pocketcasts.settings

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import android.webkit.HttpAuthHandler
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewState

@Composable
internal fun HackWeekEoYPage(
    onGoBack: () -> Unit,
    isSubscribedCallback: () -> Boolean,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {},
    onWebViewDisposed: (WebView) -> Unit = {},
) {
    var isWebViewLoading by remember { mutableStateOf(true) }
    var initialUrl by rememberSaveable { mutableStateOf("https://pocketcasts.net/eoy/") }
    val webViewState = rememberWebViewState(url = initialUrl)

    Box(
        modifier = modifier.background(color = Color.Blue)
    ) {
        Column {
            ThemedTopAppBar(
                modifier = modifier.fillMaxWidth().background(color = Color.Gray),
                onNavigationClick = onGoBack,
            )
            WebView(
                state = webViewState,
                modifier = Modifier.weight(1f).background(color = Color.Yellow),
                client = remember {
                    WebViewClient(
                        isUserSubscribed = isSubscribedCallback,
                        onPageLoaded = { isWebViewLoading = false }
                    )
                },
                onCreated = { webView ->
                    webView.settings.apply {
                        @SuppressLint("SetJavaScriptEnabled")
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        textZoom = 100
                    }
                    webView.addJavascriptInterface(object : EoYJavascriptInterface {
                        @JavascriptInterface
                        override fun receiveMessage(data: String): Boolean {
                            Log.d("===", "receiveMessage: $data")
                            return true
                        }
                    }, "Android")
                    onWebViewCreated(webView)
                },
                onDispose = onWebViewDisposed
            )
        }
        AnimatedVisibility(
            visible = isWebViewLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private interface EoYJavascriptInterface {
    @JavascriptInterface
    fun receiveMessage(data: String): Boolean
}

private class WebViewClient(
    private val isUserSubscribed: () -> Boolean,
    private val onPageLoaded: (String?) -> Unit
) : AccompanistWebViewClient() {
    override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
        Log.i("====", "onHttpAuthRequest $handler")
        handler?.proceed("www-latest", "wjn3tbqwtf4CGMfgk1")
    }

    override fun onPageFinished(view: WebView, url: String?) {
        Log.i("====", "onPageFinished $url")
        super.onPageFinished(view, url)
        view.evaluateJavascript("window.setUserData({subscriber: ${isUserSubscribed()}});", null)
        onPageLoaded(url)
    }


}