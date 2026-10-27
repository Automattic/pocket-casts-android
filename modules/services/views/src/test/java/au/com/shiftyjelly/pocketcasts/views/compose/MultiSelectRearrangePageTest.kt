package au.com.shiftyjelly.pocketcasts.views.compose

import android.view.ContextThemeWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectRearrangeState
import com.automattic.eventhorizon.EventHorizon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp")
class MultiSelectRearrangePageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val state = MultiSelectRearrangeState(
        settings = mock<Settings>(),
        eventHorizon = EventHorizon(TestEventSink()),
        maxToolbarIcons = 4,
        source = SourceView.PODCAST_SCREEN,
        shouldShowRemoveListeningHistory = false,
    ).apply { onSettingsChanged(emptyList()) }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides ContextThemeWrapper(LocalContext.current, UR.style.ThemeLight)) {
                AppTheme(Theme.ThemeType.LIGHT) {
                    val items by state.items.collectAsStateWithLifecycle()
                    MultiSelectRearrangePage(
                        items = items,
                        onNavigationClick = { calls += "back" },
                        onToolbarLongClick = { calls += "theme" },
                        onItemMove = { from, to ->
                            calls += "move $from $to"
                            state.onItemMove(from, to)
                        },
                        onItemStartDrag = { calls += "start $it" },
                        onItemDrop = { calls += "finish $it" },
                        includeStatusBarPadding = false,
                    )
                }
            }
        }
    }

    @Test
    fun `shows the title, both headers and the actions in order`() {
        setContent()

        composeTestRule.onNodeWithText("Rearrange Actions").assertIsDisplayed()
        val texts = listOf("Shortcut in toolbar", "Download", "Archive", "Mark as played", "Play next", "In overflow", "Add to playlist", "Play last", "Star", "Share")
        val tops = texts.map { composeTestRule.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun `back button and toolbar long press call back`() {
        setContent()

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("Rearrange Actions").performTouchInput { longClick() }

        assertEquals(listOf("back", "theme"), calls)
    }

    @Test
    fun `dragging a row by its long press moves it and reports the drop position`() {
        setContent()
        val rowHeight = composeTestRule.onNodeWithText("Archive").fetchSemanticsNode().boundsInRoot.top -
            composeTestRule.onNodeWithText("Download").fetchSemanticsNode().boundsInRoot.top

        composeTestRule.onNodeWithText("Download").performTouchInput {
            down(center)
            advanceEventTime(1_000)
            repeat(10) { moveBy(Offset(0f, rowHeight * 1.2f / 10)) }
            up()
        }
        composeTestRule.waitForIdle()

        assertTrue(calls.toString(), calls.contains("move 1 2"))
        assertEquals("finish 2", calls.last())
        assertTrue(calls.none { it.startsWith("start") })
    }

    @Test
    fun `touching a drag handle starts the drag at once and reports the drop`() {
        setContent()
        val download = composeTestRule.onNodeWithText("Download").fetchSemanticsNode().boundsInRoot
        val archive = composeTestRule.onNodeWithText("Archive").fetchSemanticsNode().boundsInRoot

        composeTestRule.onRoot().performTouchInput {
            down(Offset(width - 30.dp.toPx(), download.center.y))
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().performTouchInput {
            down(Offset(width - 30.dp.toPx(), download.center.y))
            repeat(10) { moveBy(Offset(0f, (archive.top - download.top) * 1.2f / 10)) }
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(listOf("start 1", "finish 1", "start 1", "move 1 2", "finish 2"), calls)
    }

    @Test
    fun `a handle drag past the next row centre does not swap until the whole row is passed`() {
        setContent()
        val download = composeTestRule.onNodeWithText("Download").fetchSemanticsNode().boundsInRoot
        val archive = composeTestRule.onNodeWithText("Archive").fetchSemanticsNode().boundsInRoot
        val rowHeight = archive.top - download.top

        composeTestRule.onRoot().performTouchInput {
            down(Offset(width - 30.dp.toPx(), download.center.y))
            repeat(8) { moveBy(Offset(0f, rowHeight * 0.9f / 8)) }
        }
        composeTestRule.waitForIdle()
        assertEquals(listOf("start 1"), calls)

        composeTestRule.onRoot().performTouchInput {
            moveBy(Offset(0f, rowHeight * 0.2f))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(listOf("start 1", "move 1 2", "finish 2"), calls)
    }
}
