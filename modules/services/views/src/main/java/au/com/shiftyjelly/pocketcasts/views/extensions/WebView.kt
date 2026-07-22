package au.com.shiftyjelly.pocketcasts.views.extensions

import android.view.ViewGroup
import android.webkit.WebView

fun WebView.copyLinkOnLongPress() {
    setOnLongClickListener {
        val url = webViewLinkUrl(hitTestResult.type, hitTestResult.extra) ?: return@setOnLongClickListener false
        context.copyLinkToClipboard(url)
        true
    }
}

internal fun webViewLinkUrl(type: Int, url: String?): String? {
    val isLink = type == WebView.HitTestResult.SRC_ANCHOR_TYPE || type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
    return url?.takeIf { isLink && it.isNotBlank() }
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
