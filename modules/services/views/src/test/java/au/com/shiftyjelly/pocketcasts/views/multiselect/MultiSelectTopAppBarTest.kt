package au.com.shiftyjelly.pocketcasts.views.multiselect

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectController
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

@RunWith(RobolectricTestRunner::class)
class MultiSelectTopAppBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the count, tracks the overflow and exits multi select`() {
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
        ).apply { source = SourceView.DOWNLOADS }
        composeTestRule.runOnUiThread {
            helper.isMultiSelecting = true
            helper.select(PodcastEpisode(uuid = "episode", publishedDate = Date()))
        }
        val controller = MultiSelectController(helper, CoroutineScope(Dispatchers.Main.immediate))
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                MultiSelectTopAppBar(controller, EventHorizon(sink))
            }
        }

        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Download").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        assertEquals(MultiSelectViewOverflowMenuShownEvent(source = SourceView.DOWNLOADS.analyticsValue), sink.pollEvent())

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertFalse(helper.isMultiSelecting)
    }
}
