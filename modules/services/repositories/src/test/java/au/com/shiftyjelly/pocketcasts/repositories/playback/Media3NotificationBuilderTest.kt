package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
class Media3NotificationBuilderTest {

    private lateinit var builder: Media3NotificationBuilder
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var settings: Settings
    private lateinit var mediaButtonReceiverMock: MockedStatic<MediaButtonReceiver>
    private lateinit var compatToken: MediaSessionCompat.Token
    private lateinit var sessionActivity: PendingIntent

    /**
     * Context wrapper that returns unique strings per resource ID for resources from the
     * localization module, which are not available in Robolectric tests for this library module.
     */
    private class TestContext(base: Context) : ContextWrapper(base) {
        private val wrappedResources = FallbackResources(base.resources)

        override fun getResources(): Resources = wrappedResources

        private class FallbackResources(private val delegate: Resources) : Resources(delegate.assets, delegate.displayMetrics, delegate.configuration) {

            override fun getString(id: Int): String {
                return try {
                    delegate.getString(id)
                } catch (_: NotFoundException) {
                    "res_$id"
                }
            }

            override fun getString(id: Int, vararg formatArgs: Any?): String {
                return try {
                    delegate.getString(id, *formatArgs)
                } catch (_: NotFoundException) {
                    "res_$id"
                }
            }
        }
    }

    @Before
    fun setUp() {
        val context = TestContext(RuntimeEnvironment.getApplication())

        val mockPendingIntent: PendingIntent = mock()
        mediaButtonReceiverMock = mockStatic(MediaButtonReceiver::class.java)
        mediaButtonReceiverMock.`when`<PendingIntent> {
            MediaButtonReceiver.buildMediaButtonPendingIntent(any(), any())
        }.thenReturn(mockPendingIntent)

        notificationHelper = mock()
        whenever(notificationHelper.playbackChannelBuilder()).thenReturn(
            NotificationCompat.Builder(context, "test_channel"),
        )

        val skipBackSetting = mock<UserSetting<Int>>()
        whenever(skipBackSetting.value).thenReturn(10)
        val skipForwardSetting = mock<UserSetting<Int>>()
        whenever(skipForwardSetting.value).thenReturn(30)

        settings = mock()
        whenever(settings.skipBackInSecs).thenReturn(skipBackSetting)
        whenever(settings.skipForwardInSecs).thenReturn(skipForwardSetting)

        val mediaSession = MediaSessionCompat(context, "test")
        compatToken = mediaSession.sessionToken
        sessionActivity = mock()

        builder = Media3NotificationBuilder(context, notificationHelper, settings)
    }

    @After
    fun tearDown() {
        mediaButtonReceiverMock.close()
    }

    @Test
    fun `build returns null when no current media item`() {
        val player = mock<Player>()
        whenever(player.currentMediaItem).thenReturn(null)

        assertNull(builder.build(player, compatToken, sessionActivity))
    }

    @Test
    fun `build returns null when mediaId is empty`() {
        val mediaItem = MediaItem.Builder()
            .setMediaId("")
            .build()
        val player = mock<Player>()
        whenever(player.currentMediaItem).thenReturn(mediaItem)

        assertNull(builder.build(player, compatToken, sessionActivity))
    }

    @Test
    fun `build returns notification with correct content titles`() {
        val player = createPlayer(
            mediaId = "episode-1",
            title = "Episode Title",
            artist = "Podcast Name",
        )

        val notification = builder.build(player, compatToken, sessionActivity)

        assertNotNull(notification)
        val extras = notification!!.extras
        assertEquals("Podcast Name", extras.getString(NotificationCompat.EXTRA_TITLE))
        assertEquals("Episode Title", extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString())
    }

    @Test
    fun `build has three actions`() {
        val player = createPlayer(mediaId = "episode-1")

        val notification = builder.build(player, compatToken, sessionActivity)!!
        assertEquals(3, notification.actions.size)
    }

    @Test
    fun `build shows pause action when playing`() {
        val player = createPlayer(
            mediaId = "episode-1",
            isPlaying = true,
        )

        val notification = builder.build(player, compatToken, sessionActivity)!!
        // Middle action (index 1) should be pause when playing
        val pauseTitle = "res_${LR.string.pause}"
        assertEquals(pauseTitle, notification.actions[1].title.toString())
    }

    @Test
    fun `build shows play action when paused`() {
        val player = createPlayer(
            mediaId = "episode-1",
            isPlaying = false,
        )

        val notification = builder.build(player, compatToken, sessionActivity)!!
        // Middle action (index 1) should be play when paused
        val playTitle = "res_${LR.string.play}"
        assertEquals(playTitle, notification.actions[1].title.toString())
    }

    @Test
    fun `build shows pause action when buffering`() {
        val player = createPlayer(
            mediaId = "episode-1",
            isPlaying = false,
            playbackState = Player.STATE_BUFFERING,
        )

        val notification = builder.build(player, compatToken, sessionActivity)!!
        // Buffering is treated as playing, so pause action should show
        val pauseTitle = "res_${LR.string.pause}"
        assertEquals(pauseTitle, notification.actions[1].title.toString())
    }

    @Test
    fun `build sets small icon`() {
        val player = createPlayer(mediaId = "episode-1")

        val notification = builder.build(player, compatToken, sessionActivity)!!
        assertEquals(IR.drawable.notification, notification.smallIcon.resId)
    }

    private fun createPlayer(
        mediaId: String = "test-id",
        title: String = "Test Title",
        artist: String = "Test Artist",
        isPlaying: Boolean = false,
        playbackState: Int = Player.STATE_READY,
        artworkUri: Uri? = null,
    ): Player {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(artworkUri)
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(mediaId)
            .build()

        val player = mock<Player>()
        whenever(player.currentMediaItem).thenReturn(mediaItem)
        whenever(player.mediaMetadata).thenReturn(metadata)
        whenever(player.isPlaying).thenReturn(isPlaying)
        whenever(player.playbackState).thenReturn(playbackState)

        return player
    }
}
