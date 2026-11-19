package au.com.shiftyjelly.pocketcasts.discover.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RegionSelectPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val australia = DiscoverRegion(name = "Australia", flag = "", code = "au")
    private val austria = DiscoverRegion(name = "Austria", flag = "", code = "at")

    @Test
    fun `shows the title and every region`() {
        setContent()

        composeTestRule.onNodeWithText("Select a Region").assertExists()
        composeTestRule.onNodeWithText("Australia").assertExists()
        composeTestRule.onNodeWithText("Austria").assertExists()
    }

    @Test
    fun `passes the tapped region`() {
        val clicked = mutableListOf<DiscoverRegion>()
        setContent(onRegionClick = { clicked += it })

        composeTestRule.onNodeWithText("Austria").performClick()

        assertEquals(listOf(austria), clicked)
    }

    @Test
    fun `navigates back from the toolbar`() {
        var backClicks = 0
        setContent(onNavigationClick = { backClicks++ })

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backClicks)
    }

    private fun setContent(
        onRegionClick: (DiscoverRegion) -> Unit = {},
        onNavigationClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                RegionSelectPage(
                    regions = listOf(australia, austria),
                    selectedRegionCode = "au",
                    bottomInset = PaddingValues(),
                    onRegionClick = onRegionClick,
                    onNavigationClick = onNavigationClick,
                )
            }
        }
    }
}
