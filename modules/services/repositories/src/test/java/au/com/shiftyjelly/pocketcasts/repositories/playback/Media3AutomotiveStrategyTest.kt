package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
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
}
