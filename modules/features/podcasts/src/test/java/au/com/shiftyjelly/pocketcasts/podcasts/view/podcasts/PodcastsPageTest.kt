package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PodcastsPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<String>()

    private val listener = PodcastsPageListener(
        onNavigationClick = { events += "back" },
        onFoldersLockedClick = { events += "locked" },
        onCreateFolderClick = { events += "create folder" },
        onSearchClick = { events += "search" },
        onOptionsClick = { events += "options" },
        onRefresh = { events += "refresh" },
        onPodcastClick = { events += "podcast ${it.uuid}" },
        onFolderClick = { events += "folder ${it.uuid}" },
        onAdClick = { events += "ad" },
        onAdOptionsClick = { events += "ad options" },
        onAdImpression = { events += "ad impression" },
        onReorder = { events += "reorder" },
    )

    private val podcast = Podcast(uuid = "p1", title = "Daily News", author = "Newsroom")
    private val folder = Folder("f1", "Morning", 0, Date(), 0, PodcastsSortType.NAME_A_TO_Z, deleted = false, syncModified = 0)
    private val items = listOf(FolderItem.Podcast(podcast), FolderItem.Folder(folder, listOf(podcast)))

    @Test
    fun `routes root toolbar taps`() {
        setContent(toolbar(showFoldersLocked = true))

        composeTestRule.onNodeWithText("Podcasts").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Create folder").performClick()
        composeTestRule.onNodeWithContentDescription("Search podcasts").performClick()
        composeTestRule.onNodeWithContentDescription("More options").performClick()

        assertEquals(listOf("locked", "search", "options"), events)
    }

    @Test
    fun `folder toolbar has back and no search`() {
        setContent(toolbar(title = "Morning", showBack = true, showSearch = false))

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithContentDescription("Search podcasts").assertDoesNotExist()

        assertEquals(listOf("back"), events)
    }

    @Test
    fun `shows the grid items`() {
        setContent(toolbar(), PodcastGridLayoutType.LIST_VIEW)

        composeTestRule.onNodeWithText("Daily News").performClick()
        composeTestRule.onNodeWithText("Morning").performClick()

        assertEquals(listOf("podcast p1", "folder f1"), events)
    }

    @Test
    fun `shows the empty content instead of the grid`() {
        setContent(toolbar(), showEmpty = true)

        composeTestRule.onNodeWithText("empty").assertExists()
        composeTestRule.onNodeWithText("Morning").assertDoesNotExist()
    }

    private fun toolbar(
        title: String = "Podcasts",
        showBack: Boolean = false,
        showFoldersLocked: Boolean = false,
        showSearch: Boolean = true,
    ) = PodcastsToolbarState(
        title = title,
        colors = ToolbarColors(iconColor = 0, titleColor = 0, backgroundColor = 0),
        showBack = showBack,
        showFoldersLocked = showFoldersLocked,
        showCreateFolder = false,
        showSearch = showSearch,
    )

    private fun setContent(
        toolbar: PodcastsToolbarState,
        layout: PodcastGridLayoutType = PodcastGridLayoutType.LARGE_ARTWORK,
        showEmpty: Boolean = false,
    ) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                PodcastsPage(
                    toolbar = toolbar,
                    content = PodcastsContent(items, layout, BadgeType.ALL_UNFINISHED, mapOf("p1" to 3), activeAd = null),
                    showContent = !showEmpty,
                    showEmpty = showEmpty,
                    isRefreshing = false,
                    gridState = LazyGridState(),
                    bottomInsetPx = 0,
                    imageRequestFactory = PocketCastsImageRequestFactory(LocalContext.current),
                    listener = listener,
                    castButton = {},
                    emptyContent = { Text("empty") },
                )
            }
        }
    }
}
