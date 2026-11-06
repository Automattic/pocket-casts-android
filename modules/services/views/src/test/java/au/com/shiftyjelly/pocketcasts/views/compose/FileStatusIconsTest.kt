package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeRowData
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileStatusIconsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun rowData(
        downloadStatus: EpisodeDownloadStatus,
        serverStatus: UserEpisodeServerStatus,
        hasBookmarks: Boolean,
    ) = UserEpisodeRowData(
        episode = UserEpisode(
            uuid = "uuid",
            publishedDate = Date(),
            duration = 3600.0,
            downloadStatus = downloadStatus,
            serverStatus = serverStatus,
        ),
        downloadProgress = 40,
        uploadProgress = 50,
        playbackState = PlaybackState(),
        isInUpNext = false,
        hasBookmarks = hasBookmarks,
    )

    private fun setContent(
        downloadStatus: EpisodeDownloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        serverStatus: UserEpisodeServerStatus = UserEpisodeServerStatus.LOCAL,
        hasBookmarks: Boolean = false,
    ) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                FileStatusIcons(data = rowData(downloadStatus, serverStatus, hasBookmarks), tint = Color.Red)
            }
        }
    }

    @Test
    fun `shows time left and bookmarks description`() {
        setContent(EpisodeDownloadStatus.Downloaded, hasBookmarks = true)

        composeTestRule.onNodeWithText("1h").assertExists()
        composeTestRule.onNodeWithContentDescription("Bookmarks").assertExists()
    }

    @Test
    fun `shows formatted download progress`() {
        setContent(EpisodeDownloadStatus.Downloading)

        composeTestRule.onNodeWithText("Downloading… (40%)").assertExists()
    }

    @Test
    fun `upload progress replaces the download text`() {
        setContent(EpisodeDownloadStatus.Downloading, UserEpisodeServerStatus.UPLOADING)

        composeTestRule.onNodeWithText("Uploading… (50%)").assertExists()
        composeTestRule.onNodeWithContentDescription("Bookmarks").assertDoesNotExist()
    }

    @Test
    fun `status text matches the shown text`() {
        var statusText = ""
        composeTestRule.setContent {
            statusText = fileStatusText(rowData(EpisodeDownloadStatus.Downloading, UserEpisodeServerStatus.LOCAL, hasBookmarks = false))
        }

        assertEquals("Downloading… (40%)", statusText)
    }
}
