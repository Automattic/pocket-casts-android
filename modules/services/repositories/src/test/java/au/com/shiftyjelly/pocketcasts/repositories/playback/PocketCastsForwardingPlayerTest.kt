package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.Uri
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.HeartRating
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PocketCastsForwardingPlayerTest {

    private lateinit var mockPlayer: Player
    private lateinit var forwardingPlayer: PocketCastsForwardingPlayer

    @Before
    fun setUp() {
        mockPlayer = mock {
            on { seekForwardIncrement } doReturn 30_000L
            on { seekBackIncrement } doReturn 10_000L
            on { duration } doReturn C.TIME_UNSET
            on { applicationLooper } doReturn Looper.getMainLooper()
        }
        shadowOf(Looper.getMainLooper()).idle()
        forwardingPlayer = PocketCastsForwardingPlayer(mockPlayer)
    }

    @Test
    fun `returns empty media item by default`() {
        assertEquals(MediaItem.EMPTY, forwardingPlayer.currentMediaItem)
    }

    @Test
    fun `returns empty media metadata by default`() {
        assertEquals(MediaMetadata.EMPTY, forwardingPlayer.mediaMetadata)
    }

    @Test
    fun `updateMetadata sets current media item with podcast episode`() {
        val episode = createPodcastEpisode(
            uuid = "ep-123",
            title = "My Episode",
            durationMs = 3600_000,
            imageUrl = "https://example.com/episode.jpg",
        )
        val podcast = createPodcast(
            title = "My Podcast",
            author = "Author Name",
        )

        forwardingPlayer.updateMetadata(episode, podcast)

        val mediaItem = forwardingPlayer.currentMediaItem
        assertEquals("ep-123", mediaItem.mediaId)

        val metadata = mediaItem.mediaMetadata
        assertEquals("My Episode", metadata.title)
        assertEquals("My Podcast", metadata.artist)
        assertEquals("Author Name", metadata.albumTitle)
        assertEquals("Podcast", metadata.genre)
        assertEquals(Uri.parse("https://example.com/episode.jpg"), metadata.artworkUri)
        assertEquals(3600_000L, metadata.durationMs)
        assertEquals(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE, metadata.mediaType)
        assertFalse(metadata.isBrowsable!!)
        assertTrue(metadata.isPlayable!!)
    }

    @Test
    fun `updateMetadata uses podcast artwork when episode has no image`() {
        val episode = createPodcastEpisode(
            uuid = "ep-456",
            title = "No Image Episode",
            imageUrl = null,
        )
        val podcast = createPodcast(title = "Podcast")

        forwardingPlayer.updateMetadata(episode, podcast)

        val artworkUri = forwardingPlayer.mediaMetadata.artworkUri
        assertTrue(artworkUri.toString().contains("480"))
    }

    @Test
    fun `updateMetadata with user episode`() {
        val episode = createUserEpisode(
            uuid = "user-ep-1",
            title = "My Upload",
            artworkUrl = "https://example.com/user-art.jpg",
        )

        forwardingPlayer.updateMetadata(episode, null)

        val metadata = forwardingPlayer.mediaMetadata
        assertEquals("My Upload", metadata.title)
        assertEquals(Uri.parse("https://example.com/user-art.jpg"), metadata.artworkUri)
    }

    @Test
    fun `updateMetadata with null podcast omits album title`() {
        val episode = createPodcastEpisode(uuid = "ep-1", title = "Test")

        forwardingPlayer.updateMetadata(episode, null)

        assertNull(forwardingPlayer.mediaMetadata.albumTitle)
    }

    @Test
    fun `updateMetadata sets starred rating`() {
        val episode = createPodcastEpisode(uuid = "ep-1", title = "Starred", isStarred = true)

        forwardingPlayer.updateMetadata(episode, null)

        val rating = forwardingPlayer.mediaMetadata.userRating as HeartRating
        assertTrue(rating.isHeart)
    }

    @Test
    fun `updateMetadata sets unstarred rating`() {
        val episode = createPodcastEpisode(uuid = "ep-1", title = "Unstarred", isStarred = false)

        forwardingPlayer.updateMetadata(episode, null)

        val rating = forwardingPlayer.mediaMetadata.userRating as HeartRating
        assertFalse(rating.isHeart)
    }

    @Test
    fun `updateMetadata notifies listeners of metadata change`() {
        val listener = mock<Player.Listener>()
        forwardingPlayer.addListener(listener)

        val episode = createPodcastEpisode(uuid = "ep-1", title = "Test")
        forwardingPlayer.updateMetadata(episode, null)

        verify(listener).onMediaMetadataChanged(forwardingPlayer.mediaMetadata)
        verify(listener).onMediaItemTransition(
            forwardingPlayer.currentMediaItem,
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
        )
    }

    @Test
    fun `available commands include expected controls`() {
        val commands = forwardingPlayer.availableCommands

        assertTrue(commands.contains(Player.COMMAND_PLAY_PAUSE))
        assertTrue(commands.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM))
        assertTrue(commands.contains(Player.COMMAND_SEEK_FORWARD))
        assertTrue(commands.contains(Player.COMMAND_SEEK_BACK))
        assertTrue(commands.contains(Player.COMMAND_GET_CURRENT_MEDIA_ITEM))
        assertTrue(commands.contains(Player.COMMAND_GET_METADATA))
    }

    @Test
    fun `duration falls back to metadata when player reports unset`() {
        val episode = createPodcastEpisode(uuid = "ep-1", title = "Test", durationMs = 120_000)
        forwardingPlayer.updateMetadata(episode, null)

        assertEquals(120_000L, forwardingPlayer.duration)
    }

    @Test
    fun `duration prefers player duration when available`() {
        val playerWithDuration = mock<Player> {
            on { duration } doReturn 90_000L
            on { applicationLooper } doReturn Looper.getMainLooper()
        }
        val player = PocketCastsForwardingPlayer(playerWithDuration)

        val episode = createPodcastEpisode(uuid = "ep-1", title = "Test", durationMs = 120_000)
        player.updateMetadata(episode, null)

        assertEquals(90_000L, player.duration)
    }

    @Test
    fun `duration returns TIME_UNSET when nothing available`() {
        assertEquals(C.TIME_UNSET, forwardingPlayer.duration)
    }

    @Test
    fun `isTransientLoss defaults to false`() {
        assertFalse(forwardingPlayer.isTransientLoss)
    }

    @Test
    fun `swapPlayer returns new instance with same metadata`() {
        val episode = createPodcastEpisode(uuid = "ep-1", title = "Preserved")
        forwardingPlayer.updateMetadata(episode, null)
        forwardingPlayer.isTransientLoss = true

        val newWrappedPlayer = mock<Player> {
            on { applicationLooper } doReturn Looper.getMainLooper()
        }
        val swapped = forwardingPlayer.swapPlayer(newWrappedPlayer)

        assertNotSame(forwardingPlayer, swapped)
        assertEquals("ep-1", swapped.currentMediaItem.mediaId)
        assertEquals("Preserved", swapped.mediaMetadata.title)
        assertTrue(swapped.isTransientLoss)
    }

    @Test
    fun `swapPlayer wraps the new player`() {
        val newWrappedPlayer = mock<Player> {
            on { applicationLooper } doReturn Looper.getMainLooper()
            on { seekForwardIncrement } doReturn 45_000L
        }
        val swapped = forwardingPlayer.swapPlayer(newWrappedPlayer)

        assertEquals(45_000L, swapped.seekForwardIncrement)
    }

    private fun createPodcastEpisode(
        uuid: String = "test-uuid",
        title: String = "Test Episode",
        durationMs: Int = 60_000,
        imageUrl: String? = null,
        isStarred: Boolean = false,
    ): PodcastEpisode {
        return PodcastEpisode(
            uuid = uuid,
            title = title,
            duration = durationMs.toDouble() / 1000.0,
            publishedDate = Date(),
            podcastUuid = "podcast-uuid",
            imageUrl = imageUrl,
            isStarred = isStarred,
        )
    }

    private fun createUserEpisode(
        uuid: String = "user-uuid",
        title: String = "User Episode",
        artworkUrl: String? = null,
    ): UserEpisode {
        return UserEpisode(
            uuid = uuid,
            title = title,
            artworkUrl = artworkUrl,
            publishedDate = Date(),
        )
    }

    private fun createPodcast(
        title: String = "Test Podcast",
        author: String = "",
    ): Podcast {
        return Podcast(
            uuid = "podcast-uuid",
            title = title,
            author = author,
        )
    }
}
