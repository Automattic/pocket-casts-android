package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PodcastCellsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<String>()
    private val podcast = Podcast(uuid = "p1", title = "Daily News", author = "Newsroom")
    private val folder = Folder("f1", "Morning", 0, Date(), 0, PodcastsSortType.NAME_A_TO_Z, deleted = false, syncModified = 0)
    private val items = listOf(FolderItem.Podcast(podcast), FolderItem.Folder(folder, listOf(podcast)))

    @Test
    fun `grid cells describe badges and open podcasts and folders`() {
        setContent(PodcastGridLayoutType.LARGE_ARTWORK, BadgeType.ALL_UNFINISHED)

        composeTestRule.onNodeWithContentDescription("Daily News. 3 new episodes.  Open podcast.").performClick()
        composeTestRule.onNodeWithText("Morning").performClick()

        assertEquals(listOf("podcast p1", "folder f1"), events)
    }

    @Test
    fun `badges off leave the count out of the description`() {
        setContent(PodcastGridLayoutType.SMALL_ARTWORK, BadgeType.OFF)

        composeTestRule.onNodeWithContentDescription("Daily News.  Open podcast.").assertExists()
        composeTestRule.onAllNodesWithText("3").assertCountEquals(0)
    }

    @Test
    fun `list rows show the title, author, folder count and badges`() {
        setContent(PodcastGridLayoutType.LIST_VIEW, BadgeType.ALL_UNFINISHED)

        composeTestRule.onNodeWithText("Daily News").assertExists()
        composeTestRule.onNodeWithText("Newsroom").assertExists()
        composeTestRule.onNodeWithText("1 Podcast").assertExists()
        composeTestRule.onAllNodesWithText("3").assertCountEquals(2)
    }

    private fun setContent(layout: PodcastGridLayoutType, badgeType: BadgeType) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                val content = PodcastsContent(items, layout, badgeType, mapOf("p1" to 3), activeAd = null)
                Column(Modifier.width(320.dp)) {
                    items.forEach { item ->
                        FolderItemCell(
                            item = item,
                            content = content,
                            isLifted = false,
                            imageRequestFactory = PocketCastsImageRequestFactory(LocalContext.current),
                            onPodcastClick = { events += "podcast ${it.uuid}" },
                            onFolderClick = { events += "folder ${it.uuid}" },
                        )
                    }
                }
            }
        }
    }
}
