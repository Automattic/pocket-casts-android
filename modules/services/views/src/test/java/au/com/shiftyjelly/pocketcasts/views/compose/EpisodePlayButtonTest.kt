package au.com.shiftyjelly.pocketcasts.views.compose

import android.widget.PopupMenu
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPopupMenu

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
        modifier: Modifier = Modifier,
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
                    modifier = modifier,
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

    private fun longPressMenu(contentDescription: String): PopupMenu {
        composeTestRule.onNodeWithContentDescription(contentDescription).performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        return ShadowPopupMenu.getLatestPopupMenu()
    }

    private fun PopupMenu.visibleTitles() = (0 until menu.size()).map { menu.getItem(it) }.filter { it.isVisible }.map { it.title.toString() }

    private fun PopupMenu.select(title: String) {
        val item = (0 until menu.size()).map { menu.getItem(it) }.first { it.title == title }
        shadowOf(this).onMenuItemClickListener.onMenuItemClick(item)
    }

    @Test
    fun `long press menu offers download and runs the selected action`() {
        setContent(PlayButtonType.PLAY)

        val popup = longPressMenu("Play")
        assertTrue(shadowOf(popup).isShowing)
        assertEquals(listOf("Play next", "Play last", "Download"), popup.visibleTitles())
        popup.select("Download")

        assertEquals(listOf("download episode"), calls)
    }

    @Test
    fun `hovering off the button after a long press does not reopen the menu`() {
        setContent(PlayButtonType.PLAY, modifier = Modifier.height(48.dp))
        val popup = longPressMenu("Play")
        popup.dismiss()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Play").performMouseInput {
            moveTo(center)
            moveTo(Offset(width * 4f, height * 4f))
        }
        composeTestRule.waitForIdle()

        assertFalse(shadowOf(popup).isShowing)
    }

    @Test
    fun `long press menu on download button offers stream`() {
        setContent(PlayButtonType.DOWNLOAD)

        val popup = longPressMenu("Download")
        assertEquals(listOf("Play next", "Play last", "Stream"), popup.visibleTitles())
        popup.select("Stream")

        assertEquals(listOf("play episode"), calls)
    }

    @Test
    fun `long press menu while downloading offers stop downloading`() {
        setContent(PlayButtonType.STOP_DOWNLOAD, downloadStatus = EpisodeDownloadStatus.Downloading)

        val popup = longPressMenu("Stop Downloading")
        assertEquals(listOf("Play next", "Play last", "Stop downloading"), popup.visibleTitles())
        popup.select("Play next")
        popup.select("Stop downloading")

        assertEquals(listOf("playNext episode", "stopDownloading episode"), calls)
    }

    @Test
    fun `long press menu play last queues the episode`() {
        setContent(PlayButtonType.PLAY, downloadStatus = EpisodeDownloadStatus.Downloaded)

        val popup = longPressMenu("Play")
        assertEquals(listOf("Play next", "Play last"), popup.visibleTitles())
        popup.select("Play last")

        assertEquals(listOf("playLast episode"), calls)
    }
}
