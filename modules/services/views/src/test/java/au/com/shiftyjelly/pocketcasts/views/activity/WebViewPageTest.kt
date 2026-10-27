package au.com.shiftyjelly.pocketcasts.views.activity

import android.webkit.WebView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebViewPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the title, hosts the web view and navigates up`() {
        var upClicks = 0
        var webView: WebView? = null
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.DARK) {
                WebViewPage(
                    title = "Clip",
                    isLoading = false,
                    onNavigationClick = { upClicks++ },
                    onCreateWebView = { context -> WebView(context).also { webView = it } },
                )
            }
        }

        composeTestRule.onNodeWithText("Clip").assertExists()
        composeTestRule.onNodeWithContentDescription("Navigate up").performClick()

        assertNotNull(webView)
        assertEquals(1, upClicks)
    }
}
