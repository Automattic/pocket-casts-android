package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonType
import com.automattic.eventhorizon.DiscoverListEpisodePlayEvent
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpisodePlayButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val eventSink = TestEventSink()
    private val calls = mutableListOf<String>()
    private val listener = object : PlayButton.OnClickListener {
        override var source = SourceView.UNKNOWN
        override fun onPlayClicked(episodeUuid: String) = record("play $episodeUuid")
        override fun onPauseClicked() = record("pause")
        override fun onPlayNext(episodeUuid: String) = record("playNext $episodeUuid")
        override fun onPlayLast(episodeUuid: String) = record("playLast $episodeUuid")
        override fun onDownload(episodeUuid: String) = record("download $episodeUuid")
        override fun onStopDownloading(episodeUuid: String) = record("stopDownloading $episodeUuid")
        override fun onPlayedClicked(episodeUuid: String) = record("played $episodeUuid")
    }

    private fun record(call: String) {
        calls += call
    }

    private fun setContent(
        type: PlayButtonType,
        downloadStatus: EpisodeDownloadStatus = EpisodeDownloadStatus.DownloadNotRequested,
        fromListUuid: String? = null,
    ) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                EpisodePlayButton(
                    episode = PodcastEpisode(uuid = "episode", podcastUuid = "podcast", publishedDate = Date(), downloadStatus = downloadStatus),
                    buttonType = type,
                    tint = Color.Red,
                    listener = listener,
                    eventHorizon = EventHorizon(eventSink),
                    fromListUuid = fromListUuid,
                )
            }
        }
    }

    private fun assertClick(type: PlayButtonType, expectedCall: String) {
        setContent(type)

        composeTestRule.onNodeWithContentDescription(type.label).performClick()

        assertEquals(listOf(expectedCall), calls)
    }

    @Test
    fun `play click plays the episode`() = assertClick(PlayButtonType.PLAY, "play episode")

    @Test
    fun `pause click pauses`() = assertClick(PlayButtonType.PAUSE, "pause")

    @Test
    fun `download click downloads`() = assertClick(PlayButtonType.DOWNLOAD, "download episode")

    @Test
    fun `stop download click stops downloading`() = assertClick(PlayButtonType.STOP_DOWNLOAD, "stopDownloading episode")

    @Test
    fun `playback failed click plays the episode`() = assertClick(PlayButtonType.PLAYBACK_FAILED, "play episode")

    @Test
    fun `played click marks unplayed`() = assertClick(PlayButtonType.PLAYED, "played episode")

    @Test
    fun `content descriptions match the view labels`() {
        assertEquals(
            listOf("Download", "Play", "Pause", "Mark unplayed", "Playback failed", "Stop Downloading"),
            PlayButtonType.entries.map { it.label },
        )
    }

    @Test
    fun `play click from a discover list tracks the event`() {
        setContent(PlayButtonType.PLAY, fromListUuid = "list")

        composeTestRule.onNodeWithContentDescription("Play").performClick()

        assertEquals(DiscoverListEpisodePlayEvent(listId = "list", podcastUuid = "podcast"), eventSink.pollEvent())
    }

    @Test
    fun `play click outside a discover list does not track`() {
        setContent(PlayButtonType.PLAY)

        composeTestRule.onNodeWithContentDescription("Play").performClick()

        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `long press menu offers download and runs the selected action`() {
        setContent(PlayButtonType.PLAY)

        composeTestRule.onNodeWithContentDescription("Play").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Play next").assertExists()
        composeTestRule.onNodeWithText("Play last").assertExists()
        composeTestRule.onNodeWithText("Stream").assertDoesNotExist()
        composeTestRule.onNodeWithText("Stop downloading").assertDoesNotExist()
        composeTestRule.onNodeWithText("Download").performClick()

        assertEquals(listOf("download episode"), calls)
        composeTestRule.onNodeWithText("Play next").assertDoesNotExist()
    }

    @Test
    fun `long press menu on download button offers stream`() {
        setContent(PlayButtonType.DOWNLOAD)

        composeTestRule.onNodeWithContentDescription("Download").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Stream").performClick()

        assertEquals(listOf("play episode"), calls)
    }

    @Test
    fun `long press menu while downloading offers stop downloading`() {
        setContent(PlayButtonType.STOP_DOWNLOAD, downloadStatus = EpisodeDownloadStatus.Downloading)

        composeTestRule.onNodeWithContentDescription("Stop Downloading").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Download").assertDoesNotExist()
        composeTestRule.onNodeWithText("Play next").performClick()

        assertEquals(listOf("playNext episode"), calls)
    }

    @Test
    fun `long press menu play last queues the episode`() {
        setContent(PlayButtonType.STOP_DOWNLOAD, downloadStatus = EpisodeDownloadStatus.Queued)

        composeTestRule.onNodeWithContentDescription("Stop Downloading").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Stop downloading").assertExists()
        composeTestRule.onNodeWithText("Play last").performClick()

        assertEquals(listOf("playLast episode"), calls)
    }
}
