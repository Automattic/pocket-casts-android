package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@RunWith(RobolectricTestRunner::class)
class AddFileComponentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val colours = listOf(
        AddFileColour(1, 0xFF8F97A4.toInt(), false),
        AddFileColour(2, 0xFFF43E37.toInt(), true),
    )

    @Test
    fun `colour picker selects unlocked colours and reports locked ones`() {
        val selected = mutableListOf<Int>()
        var lockedClicks = 0
        composeTestRule.setContent {
            ThemedContent {
                AddFileColourPicker(
                    colours = colours,
                    imageItem = null,
                    selectedIndex = 1,
                    isLocked = true,
                    onColourClick = { selected += it },
                    onLockedColourClick = { lockedClicks++ },
                )
            }
        }

        val clickable = composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertEquals(2, clickable.size)
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()
        composeTestRule.onAllNodes(hasClickAction())[1].performClick()

        assertEquals(listOf(1), selected)
        assertEquals(1, lockedClicks)
    }

    @Test
    fun `image button shows the lock only when requested and forwards clicks`() {
        var clicks = 0
        composeTestRule.setContent {
            ThemedContent {
                AddFileImageButton(text = "Add Custom Image", isLockVisible = true, onClick = { clicks++ })
            }
        }

        composeTestRule.onNodeWithContentDescription("locked").assertExists()
        composeTestRule.onNodeWithText("Add Custom Image").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun `image button hides the lock`() {
        composeTestRule.setContent {
            ThemedContent {
                AddFileImageButton(text = "Add Custom Image", isLockVisible = false, onClick = {})
            }
        }

        assertEquals(0, composeTestRule.onAllNodesWithContentDescription("locked").fetchSemanticsNodes().size)
    }

    @Test
    fun `upgrade callout forwards close and learn more clicks`() {
        var clicks = 0
        var closeClicks = 0
        composeTestRule.setContent {
            ThemedContent {
                AddFileUpgradeCallout(onClick = { clicks++ }, onCloseClick = { closeClicks++ })
            }
        }

        composeTestRule.onNodeWithContentDescription("Pocket Casts Plus").assertExists()
        composeTestRule.onNodeWithContentDescription("close upgrade offer").performClick()
        composeTestRule.onNodeWithText("Learn more about Pocket Casts Plus").performClick()

        assertEquals(1, closeClicks)
        assertEquals(1, clicks)
    }

    @Composable
    private fun ThemedContent(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalContext provides ContextThemeWrapper(LocalContext.current, UR.style.ThemeLight)) {
            AppTheme(Theme.ThemeType.LIGHT, content)
        }
    }
}
