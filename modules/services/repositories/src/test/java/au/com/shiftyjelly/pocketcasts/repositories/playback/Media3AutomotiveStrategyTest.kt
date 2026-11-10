package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import java.util.Date
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
        val strategy = Media3AutomotiveStrategy()

        val layout = strategy.buildLayout(playbackManager, mock(), context, noButton)

        assertTrue(layout.primaryButtons.isEmpty())
        assertTrue(layout.overflowButtons.isEmpty())
    }

    @Test
    fun `does not add the app skip buttons because the car draws previous and next`() {
        val playbackManager = mock<PlaybackManager> {
            on { getCurrentEpisode() } doReturn PodcastEpisode(uuid = "ep-1", publishedDate = Date())
        }
        val visibility = mock<UserSetting<Boolean>> {
            on { value } doReturn false
        }
        val mediaControls = mock<UserSetting<List<MediaNotificationControls>>> {
            on { value } doReturn MediaNotificationControls.All
        }
        val settings = mock<Settings> {
            on { customMediaActionsVisibility } doReturn visibility
            on { mediaControlItems } doReturn mediaControls
        }

        val layout = Media3AutomotiveStrategy().buildLayout(playbackManager, settings, context, noButton)

        assertTrue(layout.primaryButtons.isEmpty())
        assertTrue(layout.overflowButtons.isEmpty())
    }
}
