package au.com.shiftyjelly.pocketcasts.views.extensions

import android.webkit.WebView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebViewTest {
    @Test
    fun `returns URL for a text link`() {
        assertEquals(
            "https://pocketcasts.com",
            webViewLinkUrl(WebView.HitTestResult.SRC_ANCHOR_TYPE, "https://pocketcasts.com"),
        )
    }

    @Test
    fun `returns URL for an image link`() {
        assertEquals(
            "https://pocketcasts.com",
            webViewLinkUrl(WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE, "https://pocketcasts.com"),
        )
    }

    @Test
    fun `ignores non-link hit test results`() {
        assertNull(webViewLinkUrl(WebView.HitTestResult.UNKNOWN_TYPE, "https://pocketcasts.com"))
    }

    @Test
    fun `ignores missing or blank URLs`() {
        assertNull(webViewLinkUrl(WebView.HitTestResult.SRC_ANCHOR_TYPE, null))
        assertNull(webViewLinkUrl(WebView.HitTestResult.SRC_ANCHOR_TYPE, ""))
    }
}
