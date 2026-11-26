package au.com.shiftyjelly.pocketcasts.podcasts.view

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListViewModel.State
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@RunWith(RobolectricTestRunner::class)
class ProfileEpisodeListPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<String>()

    private val listener = ProfileEpisodeListListener(
        onNavigationClick = { events += "back" },
        onMoreOptionsClick = { events += "more" },
        onSearchQueryChange = { events += "search $it" },
        onCreateFreeAccountClick = { events += "create account" },
        onDismissFreeAccountBanner = { events += "dismiss banner" },
        onManageDownloadsShown = { events += "manage shown" },
        onManageDownloadsClick = { events += "manage" },
        onDismissManageDownloads = { events += "dismiss manage" },
        onDiscoverClick = { events += "discover" },
    )

    private val episodes = listOf(
        PodcastEpisode(uuid = "1", title = "First episode", publishedDate = Date()),
        PodcastEpisode(uuid = "2", title = "Second episode", publishedDate = Date()),
    )

    @Test
    fun `shows the toolbar, search bar, banner and rows`() {
        setContent(State.Loaded(showSearchBar = true, results = episodes), isFreeAccountBannerVisible = true)

        composeTestRule.onNodeWithText("Listening History").assertExists()
        composeTestRule.onNodeWithText("Search").assertExists()
        composeTestRule.onNodeWithText("Keep track of what you’ve played").assertExists()
        composeTestRule.onNodeWithText("row First episode").assertExists()
        composeTestRule.onNodeWithText("row Second episode").assertExists()
    }

    @Test
    fun `routes toolbar and banner taps`() {
        setContent(State.Loaded(results = episodes), isFreeAccountBannerVisible = true, manageDownloadsSize = 1024L)

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Create a free account").performClick()
        composeTestRule.onNodeWithText("Manage downloads").performClick()

        assertEquals(listOf("manage shown", "back", "more", "create account", "manage"), events)
    }

    @Test
    fun `hides the overflow when the mode has no menu`() {
        setContent(State.Loaded(results = episodes), showMenu = false)

        composeTestRule.onNodeWithContentDescription("More options").assertDoesNotExist()
    }

    @Test
    fun `shows the empty state with the discover button`() {
        val state = State.Empty(
            iconRes = IR.drawable.ic_listen_history,
            titleRes = LR.string.profile_empty_history,
            summaryRes = LR.string.profile_empty_history_summary,
        )
        setContent(state)

        composeTestRule.onNodeWithText("row First episode").assertDoesNotExist()
        composeTestRule.onNodeWithText("Go to Discover").performClick()

        assertEquals(listOf("discover"), events)
    }

    @Test
    fun `shows the multi-select top bar instead of the toolbar`() {
        setContent(State.Loaded(results = episodes), multiSelectTopBar = { Text("2 selected") })

        composeTestRule.onNodeWithText("2 selected").assertExists()
        composeTestRule.onNodeWithText("Listening History").assertDoesNotExist()
    }

    private fun setContent(
        state: State,
        showMenu: Boolean = true,
        isFreeAccountBannerVisible: Boolean = false,
        manageDownloadsSize: Long? = null,
        multiSelectTopBar: (@Composable () -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                ProfileEpisodeListPage(
                    title = "Listening History",
                    state = state,
                    searchQuery = "",
                    showMenu = showMenu,
                    showDiscoverButton = true,
                    isFreeAccountBannerVisible = isFreeAccountBannerVisible,
                    manageDownloadsSize = manageDownloadsSize,
                    bottomInsetPx = 0,
                    listResetKey = 0,
                    listener = listener,
                    multiSelectTopBar = multiSelectTopBar,
                    episodeRow = { episode -> Text("row ${episode.title}") },
                )
            }
        }
    }
}
