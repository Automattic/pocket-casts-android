package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchToolbarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the hint without the clear button when the query is empty`() {
        setContent(query = "")

        composeTestRule.onNodeWithText("Search podcasts or add RSS URL").assertExists()
        composeTestRule.onNodeWithContentDescription("Clear query").assertDoesNotExist()
    }

    @Test
    fun `passes the typed text`() {
        val queries = mutableListOf<String>()
        setContent(query = "", onQueryChange = { queries += it })

        composeTestRule.onNode(hasSetTextAction()).performTextInput("abc")

        assertEquals(listOf("abc"), queries)
    }

    @Test
    fun `searches from the keyboard action`() {
        var searches = 0
        setContent(query = "abc", onSearch = { searches++ })

        composeTestRule.onNode(hasSetTextAction()).performImeAction()

        assertEquals(1, searches)
    }

    @Test
    fun `clears the query`() {
        var clears = 0
        setContent(query = "abc", onClear = { clears++ })

        composeTestRule.onNodeWithContentDescription("Clear query").performClick()

        assertEquals(1, clears)
    }

    @Test
    fun `navigates back`() {
        var backClicks = 0
        setContent(query = "abc", onBack = { backClicks++ })

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backClicks)
    }

    private fun setContent(
        query: String,
        onQueryChange: (String) -> Unit = {},
        onSearch: () -> Unit = {},
        onClear: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                SearchToolbar(
                    query = query,
                    isFloating = false,
                    focusRequester = remember { FocusRequester() },
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onClear = onClear,
                    onBack = onBack,
                )
            }
        }
    }
}
