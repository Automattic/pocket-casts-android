package au.com.shiftyjelly.pocketcasts.compose.episode

import android.content.Context
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowData
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import au.com.shiftyjelly.pocketcasts.images.R as IR

@RunWith(RobolectricTestRunner::class)
class EpisodeRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val rowData = EpisodeRowData(
        downloadProgress = 40,
        playbackState = PlaybackState(),
        isInUpNext = true,
        hasBookmarks = false,
        hasHlsAlternateEnclosure = false,
    )

    @Test
    fun `shows the episode and routes taps`() {
        val events = mutableListOf<String>()
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                EpisodeRow(
                    episode = PodcastEpisode(uuid = "1", title = "Episode title", publishedDate = Date(), duration = 600.0),
                    useEpisodeArtwork = false,
                    onClick = { events += "click" },
                    onLongClick = { events += "long click" },
                    trailing = { TextButton(onClick = { events += "play" }) { Text("Play") } },
                )
            }
        }

        composeTestRule.onNodeWithText("Episode title").performClick()
        composeTestRule.onNodeWithText("Episode title").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Play").performClick()

        assertEquals(listOf("click", "long click", "play"), events)
    }

    @Test
    fun `shows a checked checkbox instead of the trailing content while multi-selecting`() {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                EpisodeRow(
                    episode = PodcastEpisode(uuid = "1", title = "Episode title", publishedDate = Date()),
                    useEpisodeArtwork = false,
                    isMultiSelecting = true,
                    isSelected = true,
                    onClick = {},
                    trailing = { TextButton(onClick = {}) { Text("Play") } },
                )
            }
        }

        composeTestRule.onNode(isToggleable()).assertIsOn()
        composeTestRule.onNodeWithText("Play").assertDoesNotExist()
    }

    @Test
    fun `describes the download and up next status`() {
        val downloading = PodcastEpisode(uuid = "1", publishedDate = Date(), downloadStatus = EpisodeDownloadStatus.Downloading)
        val downloaded = PodcastEpisode(uuid = "2", publishedDate = Date(), downloadStatus = EpisodeDownloadStatus.Downloaded, duration = 600.0)

        val downloadingStatus = episodeRowStatus(downloading, rowData, context)
        val downloadedStatus = episodeRowStatus(downloaded, rowData, context)

        assertEquals("Downloading… (40%)", downloadingStatus.text)
        assertEquals(0.4f, downloadingStatus.progress)
        assertEquals(true, downloadingStatus.isInUpNext)
        assertEquals(IR.drawable.ic_downloaded, downloadedStatus.iconId)
        assertEquals(true, downloadedStatus.isIconHighlighted)
    }

    @Test
    fun `shows buffering for the playing episode`() {
        val episode = PodcastEpisode(uuid = "1", publishedDate = Date())
        val buffering = rowData.copy(playbackState = PlaybackState(episodeUuid = "1", isBuffering = true))

        val status = episodeRowStatus(episode, buffering, context)

        assertEquals(true, status.isBuffering)
        assertEquals("Buffering", status.text)
    }
}
