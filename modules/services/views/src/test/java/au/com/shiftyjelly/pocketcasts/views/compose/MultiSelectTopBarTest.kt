package au.com.shiftyjelly.pocketcasts.views.compose

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.children
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.MultiSelectViewOverflowMenuShownEvent
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@RunWith(RobolectricTestRunner::class)
class MultiSelectTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val icons = listOf(
        MultiSelectToolbarItem(R.id.menu_delete, LR.string.delete, R.drawable.ic_delete),
    )

    private fun setContent(overflow: MultiSelectToolbarOverflow) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides ContextThemeWrapper(LocalContext.current, UR.style.ThemeLight)) {
                AppTheme(Theme.ThemeType.LIGHT) {
                    MultiSelectTopBar(
                        title = "3",
                        items = MultiSelectToolbarItems(icons, overflow),
                        onNavigationClick = { calls += "navigation" },
                        onItemClick = { calls += "item $it" },
                        onBottomSheetClick = { calls += "sheet ${it.actionIds}" },
                    )
                }
            }
        }
    }

    @Test
    fun `shows the count and exits from the back button`() {
        setContent(MultiSelectToolbarOverflow.None)

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("navigation"), calls)
    }

    @Test
    fun `action icons use their titles as descriptions`() {
        setContent(MultiSelectToolbarOverflow.None)

        composeTestRule.onNodeWithContentDescription("Delete").performClick()

        assertEquals(listOf("item ${R.id.menu_delete}"), calls)
    }

    @Test
    fun `episodes overflow opens the bottom sheet`() {
        val sheet = MultiSelectToolbarOverflow.BottomSheet(listOf(R.id.menu_share), showRemoveListeningHistory = false)
        setContent(sheet)

        composeTestRule.onNodeWithContentDescription("More options").performClick()

        assertEquals(listOf("sheet ${sheet.actionIds}"), calls)
    }

    @Test
    fun `menu overflow shows the hidden actions`() {
        val items = staticMultiSelectToolbarItems(RuntimeEnvironment.getApplication().resources, R.menu.menu_multiselect_upnext)
        setContent(items.overflow)

        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.waitForIdle()
        val list = shadowOf(RuntimeEnvironment.getApplication()).latestPopupWindow.contentView.findListView()
        val titles = List(list.adapter.count) { list.adapter.getView(it, null, list).findTitle() }
        list.performItemClick(list.adapter.getView(2, null, list), 2, 0)

        assertEquals(listOf("Move to top", "Move to bottom", "Select all", "Select none", "Add to playlist"), titles)
        assertEquals(listOf("item ${R.id.menu_select_all}"), calls)
    }

    @Test
    fun `controller bar tracks the overflow and exits multi select`() {
        val sink = TestEventSink()
        val helper = MultiSelectEpisodesHelper(
            episodeManager = mock(),
            userEpisodeManager = mock(),
            podcastManager = mock(),
            playbackManager = mock(),
            downloadQueue = mock(),
            settings = mock<Settings> { on { multiSelectItemsFlow } doReturn MutableStateFlow(emptyList()) },
            eventHorizon = EventHorizon(sink),
            applicationScope = mock<CoroutineScope>(),
            crashLogging = mock(),
            shareDialogFactory = mock(),
            addToPlaylistFragmentFactory = mock(),
        ).apply { source = SourceView.FILTERS }
        composeTestRule.runOnUiThread {
            helper.isMultiSelecting = true
            helper.select(PodcastEpisode(uuid = "episode", publishedDate = Date()))
        }
        val controller = MultiSelectController(helper, CoroutineScope(Dispatchers.Main.immediate))
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                MultiSelectTopBar(controller, EventHorizon(sink))
            }
        }

        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Download").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        assertEquals(MultiSelectViewOverflowMenuShownEvent(source = SourceView.FILTERS.analyticsValue), sink.pollEvent())

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertFalse(helper.isMultiSelecting)
    }

    private fun View.findListView(): ListView = this as? ListView
        ?: (this as ViewGroup).children.firstNotNullOf { runCatching { it.findListView() }.getOrNull() }

    private fun View.findTitle(): String = (this as? TextView)?.text?.toString()
        ?: (this as ViewGroup).children.firstNotNullOf { runCatching { it.findTitle() }.getOrNull() }
}
