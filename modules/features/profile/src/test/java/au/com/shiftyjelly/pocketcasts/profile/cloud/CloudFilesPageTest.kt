package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.servers.sync.FileAccount
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CloudFilesPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<String>()

    private val listener = CloudFilesListener(
        onNavigationClick = { events += "back" },
        onOptionsClick = { events += "options" },
        onAddFileClick = { events += "add" },
        onRefresh = { events += "refresh" },
    )

    private val episodes = listOf(
        UserEpisode(uuid = "1", title = "First file", publishedDate = Date()),
        UserEpisode(uuid = "2", title = "Second file", publishedDate = Date()),
    )

    @Test
    fun `shows the toolbar, locked header and rows`() {
        setContent(episodes)

        composeTestRule.onNodeWithText("Files").assertExists()
        composeTestRule.onNodeWithText("No Files Uploaded").assertExists()
        composeTestRule.onNodeWithText("row First file").assertExists()
        composeTestRule.onNodeWithText("row Second file").assertExists()
        composeTestRule.onNodeWithText("Listen or watch your own files").assertDoesNotExist()
    }

    @Test
    fun `shows the usage header for an account`() {
        setContent(episodes, account = FileAccount(totalFiles = 3, totalSize = 1000, usedSize = 900))

        composeTestRule.onNodeWithText("3 Files").assertExists()
        composeTestRule.onNodeWithText("90% Full").assertExists()
        composeTestRule.onNodeWithText("No Files Uploaded").assertDoesNotExist()
    }

    @Test
    fun `routes toolbar taps`() {
        setContent(episodes)

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithContentDescription("Options").performClick()

        assertEquals(listOf("back", "options"), events)
    }

    @Test
    fun `shows the empty state only once files are loaded`() {
        setContent(null)
        composeTestRule.onNodeWithText("Listen or watch your own files").assertDoesNotExist()
    }

    @Test
    fun `shows the empty state when there are no files`() {
        setContent(emptyList())
        composeTestRule.onNodeWithText("Listen or watch your own files").assertExists()
    }

    @Test
    fun `shows the multi-select top bar instead of the toolbar`() {
        setContent(episodes, multiSelectTopBar = { Text("2 selected") })

        composeTestRule.onNodeWithText("2 selected").assertExists()
        composeTestRule.onNodeWithText("Files").assertDoesNotExist()
    }

    private fun setContent(
        episodes: List<UserEpisode>?,
        account: FileAccount? = null,
        multiSelectTopBar: (@Composable () -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides ContextThemeWrapper(LocalContext.current, Theme.ThemeType.LIGHT.resourceId)) {
                AppTheme(Theme.ThemeType.LIGHT) {
                    CloudFilesPage(
                        episodes = episodes,
                        account = account,
                        isRefreshEnabled = false,
                        bottomInsetPx = 0,
                        listener = listener,
                        multiSelectTopBar = multiSelectTopBar,
                        episodeRow = { episode -> Text("row ${episode.title}") },
                        castButton = {},
                    )
                }
            }
        }
    }
}
