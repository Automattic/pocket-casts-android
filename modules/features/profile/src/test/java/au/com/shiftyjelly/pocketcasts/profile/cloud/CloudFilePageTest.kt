package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import coil3.request.ImageRequest
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1000dp")
class CloudFilePageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    private val listener = object : CloudFileListener {
        override fun onPlayClick(episode: UserEpisode) {
            clicks += "play"
        }

        override fun onPauseClick() {
            clicks += "pause"
        }

        override fun onUpNextClick(episode: UserEpisode, inUpNext: Boolean) {
            clicks += "upNext:$inUpNext"
        }

        override fun onPlayedClick(episode: UserEpisode) {
            clicks += "played"
        }

        override fun onBookmarksClick() {
            clicks += "bookmarks"
        }

        override fun onEditClick(episode: UserEpisode) {
            clicks += "edit"
        }

        override fun onCloudClick(episode: UserEpisode) {
            clicks += "cloud"
        }

        override fun onLockedCloudClick() {
            clicks += "locked"
        }

        override fun onDeleteClick(episode: UserEpisode) {
            clicks += "delete"
        }
    }

    @Test
    fun `local file signed out shows the actions and the locked upload row`() {
        setContent(episode(), inUpNext = false, cloudRowVisibility = CloudRowVisibility.Locked)

        composeTestRule.onNodeWithText("My Recording").assertExists()
        composeTestRule.onNodeWithText("Add to Up Next").performClick()
        composeTestRule.onNodeWithText("Mark as played").performClick()
        composeTestRule.onNodeWithText("Bookmarks").performClick()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Upload to cloud").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithContentDescription("Play button").performClick()

        assertEquals(listOf("upNext:false", "played", "bookmarks", "edit", "locked", "delete", "play"), clicks)
    }

    @Test
    fun `played file in up next with an upload error shows the alternate labels`() {
        val episode = episode(playingStatus = EpisodePlayingStatus.COMPLETED, serverStatus = UserEpisodeServerStatus.UPLOADED, uploadError = "Server error")
        setContent(episode, inUpNext = true, cloudRowVisibility = CloudRowVisibility.Cloud, isPlaying = true)

        composeTestRule.onNodeWithText("Upload Failed").assertExists()
        composeTestRule.onNodeWithText("Server error").assertExists()
        composeTestRule.onNodeWithText("Remove from Up Next").performClick()
        composeTestRule.onNodeWithText("Mark as unplayed").performClick()
        composeTestRule.onNodeWithText("Download").performClick()
        composeTestRule.onNodeWithContentDescription("Pause button").performClick()

        assertEquals(listOf("upNext:true", "played", "cloud", "pause"), clicks)
    }

    private fun episode(
        playingStatus: EpisodePlayingStatus = EpisodePlayingStatus.NOT_PLAYED,
        serverStatus: UserEpisodeServerStatus = UserEpisodeServerStatus.LOCAL,
        uploadError: String? = null,
    ) = UserEpisode(
        uuid = "uuid",
        title = "My Recording",
        publishedDate = Date(),
        duration = 60.0,
        playingStatus = playingStatus,
        serverStatus = serverStatus,
        uploadErrorDetails = uploadError,
    )

    private fun setContent(
        episode: UserEpisode,
        inUpNext: Boolean,
        cloudRowVisibility: CloudRowVisibility,
        isPlaying: Boolean = false,
    ) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                CloudFilePage(
                    state = BottomSheetState(episode, inUpNext = inUpNext, isPlaying = isPlaying),
                    rowData = null,
                    artwork = ImageRequest.Builder(ApplicationProvider.getApplicationContext()).build(),
                    cloudRowVisibility = cloudRowVisibility,
                    listener = listener,
                )
            }
        }
    }
}
