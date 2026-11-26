package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.ContextWrapper
import android.content.res.Resources
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class Media3AutomotiveStrategyTest {

    private val context = RuntimeEnvironment.getApplication()
    private val noButton: (MediaNotificationControls, BaseEpisode?) -> CommandButton? = { _, _ -> null }

    @Test
    fun `returns empty layout when there is no current episode`() {
        val playbackManager = mock<PlaybackManager> {
            on { getCurrentEpisode() } doReturn null
        }
        val strategy = Media3AutomotiveStrategy(useCustomSkipButtons = { true })

        val layout = strategy.buildLayout(playbackManager, mock(), context, noButton)

        assertTrue(layout.primaryButtons.isEmpty())
        assertTrue(layout.overflowButtons.isEmpty())
    }

    @Test
    fun `skip buttons take the back and forward slots`() {
        val strategy = Media3AutomotiveStrategy(useCustomSkipButtons = { true })

        val layout = strategy.buildLayout(playbackManagerWithEpisode(), settingsWithNoCustomActions(), labelContext, noButton)

        val (skipBack, skipForward) = layout.primaryButtons
        assertEquals(APP_ACTION_SKIP_BACK, skipBack.sessionCommand?.customAction)
        assertEquals(CommandButton.SLOT_BACK, skipBack.slots[0])
        assertEquals(APP_ACTION_SKIP_FWD, skipForward.sessionCommand?.customAction)
        assertEquals(CommandButton.SLOT_FORWARD, skipForward.slots[0])
        assertEquals(CommandButton.SLOT_OVERFLOW, skipBack.slots[1])
        assertEquals(CommandButton.SLOT_OVERFLOW, skipForward.slots[1])
    }

    @Test
    fun `no skip buttons when custom skip buttons are disabled`() {
        val strategy = Media3AutomotiveStrategy(useCustomSkipButtons = { false })

        val layout = strategy.buildLayout(playbackManagerWithEpisode(), settingsWithNoCustomActions(), labelContext, noButton)

        assertTrue(layout.primaryButtons.isEmpty())
    }

    // Localized strings aren't packaged for this module's unit tests
    private val labelContext = object : ContextWrapper(context) {
        private val labelResources = mock<Resources> { on { getString(any()) } doReturn "label" }

        override fun getResources() = labelResources
    }

    private fun playbackManagerWithEpisode(): PlaybackManager {
        val episode = PodcastEpisode(uuid = "episode", publishedDate = Date())
        return mock<PlaybackManager> {
            on { getCurrentEpisode() } doReturn episode
        }
    }

    private fun settingsWithNoCustomActions() = mock<Settings> {
        on { customMediaActionsVisibility } doReturn UserSetting.Mock(false, mock())
        on { mediaControlItems } doReturn UserSetting.Mock(emptyList(), mock())
    }
}
