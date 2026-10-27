package au.com.shiftyjelly.pocketcasts.discover.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CategoriesBottomSheetPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val arts = DiscoverCategory(id = 1, name = "Arts", icon = "", source = "")
    private val comedy = DiscoverCategory(id = 2, name = "Comedy", icon = "", source = "")

    @Test
    fun `shows the upper case header and every category`() {
        setContent()

        composeTestRule.onNodeWithText("SELECT A CATEGORY").assertExists()
        composeTestRule.onNodeWithContentDescription("Select a category").assertExists()
        composeTestRule.onNodeWithText("Arts").assertExists()
        composeTestRule.onNodeWithText("Comedy").assertExists()
    }

    @Test
    fun `passes the tapped category`() {
        val clicked = mutableListOf<DiscoverCategory>()
        setContent(onCategoryClick = { clicked += it })

        composeTestRule.onNodeWithText("Comedy").performClick()

        assertEquals(listOf(comedy), clicked)
    }

    private fun setContent(onCategoryClick: (DiscoverCategory) -> Unit = {}) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                CategoriesBottomSheetPage(
                    categories = listOf(arts, comedy),
                    contentPadding = PaddingValues(),
                    onCategoryClick = onCategoryClick,
                )
            }
        }
    }
}
