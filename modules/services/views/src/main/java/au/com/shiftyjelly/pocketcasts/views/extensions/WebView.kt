package au.com.shiftyjelly.pocketcasts.views.extensions

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView

private const val FOCUSED_NODE_URL_KEY = "url"

fun WebView.copyLinkOnLongPress() {
    setOnLongClickListener {
        val result = hitTestResult
        when (result.type) {
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                val url = webViewLinkUrl(result.type, result.extra) ?: return@setOnLongClickListener false
                context.copyLinkToClipboard(url)
                true
            }

            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                requestFocusNodeHref(
                    Handler(Looper.getMainLooper()) { message ->
                        webViewLinkUrl(
                            type = WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
                            focusedNodeUrl = message.data.getString(FOCUSED_NODE_URL_KEY),
                        )?.let(context::copyLinkToClipboard)
                        true
                    }.obtainMessage(),
                )
                true
            }

            else -> false
        }
    }
}

internal fun webViewLinkUrl(
    type: Int,
    hitTestUrl: String? = null,
    focusedNodeUrl: String? = null,
): String? {
    val url = when (type) {
        WebView.HitTestResult.SRC_ANCHOR_TYPE -> hitTestUrl
        WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> focusedNodeUrl
        else -> null
    }
    return url?.takeIf(String::isNotBlank)
}

@Suppress("DEPRECATION")
fun WebView?.cleanup() {
    this ?: return

    (parent as? ViewGroup)?.removeView(this)
    this.clearHistory()
    this.clearCache(false)
    this.loadUrl("about:blank")
    this.onPause()
    this.removeAllViews()
    this.destroyDrawingCache()
    this.destroy()
}
