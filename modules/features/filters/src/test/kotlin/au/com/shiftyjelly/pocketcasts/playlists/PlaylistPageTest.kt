package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentData
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderButtonData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderData
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import au.com.shiftyjelly.pocketcasts.images.R as IR

@RunWith(RobolectricTestRunner::class)
class PlaylistPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<String>()

    private val listener = PlaylistPageListener(
        onClickBack = { events += "back" },
        onClickOptions = { events += "options" },
        onShowArchivedToggle = { events += "toggle archived" },
        onClickShowArchivedCta = { events += "archived cta" },
        onHideKeyboard = { events += "hide keyboard" },
    )

    private val episodes = listOf(
        PlaylistEpisode.Available(PodcastEpisode(uuid = "1", title = "First episode", publishedDate = Date())),
        PlaylistEpisode.Available(PodcastEpisode(uuid = "2", title = "Second episode", publishedDate = Date())),
    )

    @Test
    fun `shows the header buttons and rows`() {
        setContent(ContentState.HasEpisode, episodes)

        composeTestRule.onNodeWithText("Smart Rules").performClick()
        composeTestRule.onNodeWithText("Play All").performClick()
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("row First episode"))
        composeTestRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("row Second episode"))

        assertEquals(listOf("left", "right"), events)
    }

    @Test
    fun `routes toolbar taps`() {
        setContent(ContentState.HasEpisode, episodes)

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithContentDescription("Options").performClick()

        assertEquals(listOf("back", "options"), events)
    }

    @Test
    fun `shows the no content banner for an empty playlist`() {
        setContent(ContentState.HasNoEpisodes, emptyList())

        composeTestRule.onNodeWithText("Nothing here").assertExists()
        composeTestRule.onNodeWithText("Add Episodes").performClick()

        assertEquals(listOf("cta"), events)
    }

    @Test
    fun `shows the multi-select top bar over the toolbar`() {
        setContent(ContentState.HasEpisode, episodes, multiSelectTopBar = { Text("2 selected") })

        composeTestRule.onNodeWithText("2 selected").assertExists()
    }

    private fun setContent(
        contentState: ContentState,
        episodes: List<PlaylistEpisode>,
        multiSelectTopBar: (@Composable () -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                PlaylistPage(
                    headerData = PlaylistHeaderData(
                        title = "New Releases",
                        metadata = Playlist.Metadata.ForPreview.copy(
                            totalEpisodeCount = episodes.size,
                            displayedEpisodeCount = episodes.size,
                        ),
                    ),
                    episodes = episodes,
                    contentState = contentState,
                    noContentData = NoContentData(
                        title = "Nothing here",
                        body = "Body",
                        iconId = IR.drawable.ic_playlists,
                        primaryButton = NoContentData.Button("Add Episodes") { events += "cta" },
                    ),
                    leftButton = PlaylistHeaderButtonData(IR.drawable.ic_playlist_smart_rules, "Smart Rules") { events += "left" },
                    rightButton = PlaylistHeaderButtonData(IR.drawable.ic_playlist_play, "Play All") { events += "right" },
                    searchState = TextFieldState(),
                    bottomInsetPx = 0,
                    listener = listener,
                    multiSelectTopBar = multiSelectTopBar,
                    episodeRow = { episode -> Text("row ${episode.toPodcastEpisode()?.title}") },
                )
            }
        }
    }
}
