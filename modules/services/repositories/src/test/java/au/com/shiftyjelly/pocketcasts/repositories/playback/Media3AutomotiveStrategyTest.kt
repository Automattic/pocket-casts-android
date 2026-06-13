package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import androidx.media3.session.CommandButton
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.ReadSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import au.com.shiftyjelly.pocketcasts.images.R as IR

@RunWith(RobolectricTestRunner::class)
class Media3AutomotiveStrategyTest {

    private lateinit var playbackManager: PlaybackManager
    private lateinit var upNextQueue: UpNextQueue
    private lateinit var settings: Settings
    private lateinit var customMediaActionsVisibility: UserSetting<Boolean>
    private lateinit var mediaControlItems: UserSetting<List<MediaNotificationControls>>
    private lateinit var upNextShuffle: UserSetting<Boolean>
    private lateinit var cachedSubscription: ReadSetting<Subscription?>
    private lateinit var context: Context

    private val strategy = Media3AutomotiveStrategy(useCustomSkipButtons = { false })

    @Before
    fun setUp() {
        playbackManager = mock()
        upNextQueue = mock()
        settings = mock()
        customMediaActionsVisibility = mock()
        mediaControlItems = mock()
        upNextShuffle = mock()
        cachedSubscription = mock()
        context = mock()
        whenever(context.getString(any())).thenReturn("Shuffle")

        whenever(playbackManager.getCurrentEpisode()).thenReturn(null)
        whenever(playbackManager.upNextQueue).thenReturn(upNextQueue)
        whenever(settings.customMediaActionsVisibility).thenReturn(customMediaActionsVisibility)
        whenever(customMediaActionsVisibility.value).thenReturn(false)
        whenever(settings.mediaControlItems).thenReturn(mediaControlItems)
        whenever(mediaControlItems.value).thenReturn(emptyList())
        whenever(settings.upNextShuffle).thenReturn(upNextShuffle)
        whenever(settings.cachedSubscription).thenReturn(cachedSubscription)
    }

    private fun buildLayout() = strategy.buildLayout(
        playbackManager = playbackManager,
        settings = settings,
        context = context,
        buildCustomActionButton = { _, _ -> null },
    )

    private fun shuffleButton(buttons: List<CommandButton>): CommandButton? {
        return buttons.find { it.sessionCommand?.customAction == APP_ACTION_SHUFFLE }
    }

    private fun setPaid(isPaid: Boolean) {
        whenever(cachedSubscription.value).thenReturn(if (isPaid) mock<Subscription>() else null)
    }

    private fun setQueue(vararg episodes: BaseEpisode) {
        whenever(upNextQueue.queueEpisodes).thenReturn(episodes.toList())
    }

    @Test
    fun `shuffle button present for paid user with non-empty queue`() {
        setPaid(true)
        setQueue(episode("ep-1"))
        whenever(upNextShuffle.value).thenReturn(false)

        val button = shuffleButton(buildLayout().primaryButtons)

        assertNotNull(button)
    }

    @Test
    fun `shuffle button hidden for free user`() {
        setPaid(false)
        setQueue(episode("ep-1"))
        whenever(upNextShuffle.value).thenReturn(false)

        val button = shuffleButton(buildLayout().primaryButtons)

        assertNull(button)
    }

    @Test
    fun `shuffle button hidden when queue is empty`() {
        setPaid(true)
        setQueue()
        whenever(upNextShuffle.value).thenReturn(false)

        val button = shuffleButton(buildLayout().primaryButtons)

        assertNull(button)
    }

    @Test
    fun `shuffle button uses disabled icon when shuffle is off`() {
        setPaid(true)
        setQueue(episode("ep-1"))
        whenever(upNextShuffle.value).thenReturn(false)

        val button = shuffleButton(buildLayout().primaryButtons)

        assertEquals(IR.drawable.shuffle, button?.iconResId)
    }

    @Test
    fun `shuffle button uses enabled icon when shuffle is on`() {
        setPaid(true)
        setQueue(episode("ep-1"))
        whenever(upNextShuffle.value).thenReturn(true)

        val button = shuffleButton(buildLayout().primaryButtons)

        assertEquals(IR.drawable.shuffle_enabled, button?.iconResId)
    }

    private fun episode(uuid: String): PodcastEpisode {
        return PodcastEpisode(uuid = uuid, title = "Test", publishedDate = Date(), podcastUuid = "pod-1")
    }
}
