package au.com.shiftyjelly.pocketcasts.settings

import android.annotation.SuppressLint
import android.webkit.HttpAuthHandler
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewState
import kotlinx.coroutines.launch

@Composable
internal fun HackWeekEoYPage(
    onGoBack: () -> Unit,
    isSubscribedCallback: () -> Boolean,
    onUpsell: () -> Unit,
    onShareScreenshot: () -> Unit,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {},
    onWebViewDisposed: (WebView) -> Unit = {},
) {
    var isWebViewLoading by remember { mutableStateOf(true) }
    val webViewState = rememberWebViewState(url = "https://pocketcasts.net/eoy/")
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        WebView(
            state = webViewState,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            client = remember {
                WebViewClient(
                    onPageLoaded = { isWebViewLoading = false },
                )
            },
            onCreated = { webView ->
                webView.settings.apply {
                    @SuppressLint("SetJavaScriptEnabled")
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    textZoom = 100
                }
                webView.addJavascriptInterface(
                    EoYJavascriptInterface {
                        scope.launch {
                            when (it) {
                                EoYWebMessage.Loaded -> webView.evaluateJavascript(
                                    "window.setUserData({subscriber: ${isSubscribedCallback()}});",
                                    null,
                                )

                                EoYWebMessage.Close -> onGoBack()
                                EoYWebMessage.Upsell -> onUpsell()
                                is EoYWebMessage.ShareStory -> onShareScreenshot()
                            }
                        }
                    },
                    "Android",
                )
                onWebViewCreated(webView)
            },
            onDispose = onWebViewDisposed,
        )
        AnimatedVisibility(
            visible = isWebViewLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CircularProgressIndicator()
        }
    }
}

sealed class EoYWebMessage {
    data object Loaded : EoYWebMessage()
    data object Close : EoYWebMessage()
    data class ShareStory(val payload: String?) : EoYWebMessage()
    data object Upsell : EoYWebMessage()
}

private class EoYJavascriptInterface(
    val onMessageReceived: (EoYWebMessage) -> Unit,
) {
    @JavascriptInterface
    fun loaded(data: Any?) {
        onMessageReceived(EoYWebMessage.Loaded)
    }

    @JavascriptInterface
    fun closeStories(data: String?) {
        onMessageReceived(EoYWebMessage.Close)
    }

    @JavascriptInterface
    fun shareStory(data: String?) {
        onMessageReceived(EoYWebMessage.ShareStory(data))
    }

    @JavascriptInterface
    fun plusFlow(data: String?) {
        onMessageReceived(EoYWebMessage.Upsell)
    }
}

private class WebViewClient(
    val onPageLoaded: () -> Unit,
) : AccompanistWebViewClient() {
    override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
        handler?.proceed("www-latest", "wjn3tbqwtf4CGMfgk1")
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        onPageLoaded()
    }
}
