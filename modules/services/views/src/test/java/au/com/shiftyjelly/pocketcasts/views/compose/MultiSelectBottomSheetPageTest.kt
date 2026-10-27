package au.com.shiftyjelly.pocketcasts.views.compose

import android.view.ContextThemeWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectAction
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodeAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@RunWith(RobolectricTestRunner::class)
class MultiSelectBottomSheetPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val actions = MultiSelectEpisodeAction.listFromIds(emptyList())
        .filter { it is MultiSelectEpisodeAction.AddToPlaylist || it is MultiSelectEpisodeAction.Star } + MultiSelectAction.SelectAll

    private fun setContent(title: String = "3 selected episodes") {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides ContextThemeWrapper(LocalContext.current, UR.style.ThemeLight)) {
                AppTheme(Theme.ThemeType.LIGHT) {
                    MultiSelectBottomSheetPage(
                        title = title,
                        actions = actions,
                        onEditClick = { calls += "edit" },
                        onActionClick = { calls += it.analyticsValue },
                    )
                }
            }
        }
    }

    @Test
    fun `shows the title and every action in order`() {
        setContent()

        composeTestRule.onNodeWithText("3 selected episodes").assertIsDisplayed()
        val titles = listOf("Add to playlist", "Star", "Select all")
        titles.forEach { composeTestRule.onAllNodesWithText(it).assertCountEquals(1) }
        val tops = titles.map { composeTestRule.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun `tapping a row sends its action`() {
        setContent()

        composeTestRule.onNodeWithText("Star").performClick()
        composeTestRule.onNodeWithText("Select all").performClick()

        assertEquals(listOf(MultiSelectEpisodeAction.Star.analyticsValue, MultiSelectAction.SelectAll.analyticsValue), calls)
    }

    @Test
    fun `edit button opens the rearrange screen`() {
        setContent()

        composeTestRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Role)).performClick()

        assertEquals(listOf("edit"), calls)
    }
}
