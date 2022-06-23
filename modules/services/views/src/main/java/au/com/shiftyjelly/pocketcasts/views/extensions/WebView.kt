package au.com.shiftyjelly.pocketcasts.views.extensions

import android.view.ViewGroup
import android.webkit.WebView

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
