package au.com.shiftyjelly.pocketcasts.views.buttons

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpisodeRowPlayButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

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

    private fun setContent(type: PlayButtonType) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                EpisodeRowPlayButton(
                    episode = PodcastEpisode(uuid = "episode", publishedDate = Date()),
                    buttonType = type,
                    listener = listener,
                )
            }
        }
    }

    @Test
    fun `routes taps for each button type`() {
        var type by mutableStateOf(PlayButtonType.PLAY)
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                EpisodeRowPlayButton(PodcastEpisode(uuid = "episode", publishedDate = Date()), type, listener)
            }
        }

        PlayButtonType.entries.forEach { buttonType ->
            type = buttonType
            composeTestRule.onNodeWithContentDescription(buttonType.label).performClick()
        }

        assertEquals(
            listOf("download episode", "play episode", "pause", "played episode", "play episode", "stopDownloading episode"),
            calls,
        )
    }

    @Test
    fun `long press opens the play next and play last menu`() {
        setContent(PlayButtonType.DOWNLOAD)

        composeTestRule.onNodeWithContentDescription("Download").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Play next").performClick()
        composeTestRule.onNodeWithContentDescription("Download").performTouchInput { longClick() }
        composeTestRule.onNodeWithText("Stream").performClick()

        assertEquals(listOf("playNext episode", "play episode"), calls)
    }
}
