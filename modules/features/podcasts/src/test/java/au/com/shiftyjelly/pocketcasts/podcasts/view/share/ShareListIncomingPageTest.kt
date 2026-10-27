package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareListIncomingPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val subscribed = Podcast(uuid = "1", title = "Daily News", author = "Newsroom")
    private val notSubscribed = Podcast(uuid = "2", title = "Material", author = "Relay")
    private val events = mutableListOf<String>()

    private val listener = object : ShareListIncomingListener {
        override fun onCloseClick() {
            events += "close"
        }

        override fun onPodcastClick(podcast: Podcast) {
            events += "open ${podcast.uuid}"
        }

        override fun onSubscribeClick(podcast: Podcast) {
            events += "subscribe ${podcast.uuid}"
        }

        override fun onUnsubscribeClick(podcast: Podcast) {
            events += "unsubscribe ${podcast.uuid}"
        }

        override fun onSubscribeToAllClick(podcasts: List<Podcast>) {
            events += "subscribe all ${podcasts.size}"
        }
    }

    @Test
    fun `shows the list header, podcasts and footer`() {
        setContent(ShareState.Loaded(title = "My list", description = "Favourites", podcasts = listOf(subscribed, notSubscribed)))

        composeTestRule.onNodeWithText("Shared list").assertExists()
        composeTestRule.onNodeWithText("My list").assertExists()
        composeTestRule.onNodeWithText("Favourites").assertExists()
        composeTestRule.onNodeWithContentDescription("Daily News by Newsroom button. You are subscribed. ").assertExists()
        composeTestRule.onNodeWithContentDescription("Material by Relay button.").assertExists()
        composeTestRule.onNodeWithText("2 Podcasts").assertExists()
    }

    @Test
    fun `routes row, tick, follow all and close taps`() {
        setContent(ShareState.Loaded(title = "My list", description = null, podcasts = listOf(subscribed, notSubscribed)))

        composeTestRule.onNodeWithContentDescription("Material by Relay button.").performClick()
        composeTestRule.onNodeWithContentDescription("Unsubscribe from Daily News button.").performClick()
        composeTestRule.onNodeWithContentDescription("Subscribe to Material button.").performClick()
        composeTestRule.onNodeWithContentDescription("Unsubscribe from Material button.").assertExists()
        composeTestRule.onNodeWithText("FOLLOW ALL").performClick()
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        assertEquals(listOf("open 2", "unsubscribe 1", "subscribe 2", "subscribe all 2", "close"), events)
    }

    @Test
    fun `hides the header and footer while loading`() {
        setContent(ShareState.Loading)

        composeTestRule.onNodeWithText("FOLLOW ALL").assertDoesNotExist()
        composeTestRule.onNodeWithText("2 Podcasts").assertDoesNotExist()
    }

    private fun setContent(share: ShareState) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                ShareListIncomingPage(
                    share = share,
                    subscribedUuids = setOf(subscribed.uuid),
                    bottomInset = PaddingValues(),
                    listener = listener,
                )
            }
        }
    }
}
