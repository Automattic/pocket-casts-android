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
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getArtworkUrl
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
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
        assertTrue(commands.contains(Player.COMMAND_SEEK_TO_NEXT))
        assertTrue(commands.contains(Player.COMMAND_SEEK_TO_PREVIOUS))
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

    @Test
    fun `seekToNext delegates to skip lambda when provided`() {
        var skipForwardCalled = false
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onSkipForward = { skipForwardCalled = true },
        )

        player.seekToNext()

        assertTrue(skipForwardCalled)
    }

    @Test
    fun `seekToPrevious delegates to skip lambda when provided`() {
        var skipBackCalled = false
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onSkipBack = { skipBackCalled = true },
        )

        player.seekToPrevious()

        assertTrue(skipBackCalled)
    }

    @Test
    fun `seekToNext falls back to super when no lambda`() {
        forwardingPlayer.seekToNext()

        verify(mockPlayer).seekToNext()
    }

    @Test
    fun `seekToPrevious falls back to super when no lambda`() {
        forwardingPlayer.seekToPrevious()

        verify(mockPlayer).seekToPrevious()
    }

    @Test
    fun `stop calls onStop callback instead of wrapped player when provided`() {
        var stopCalled = false
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onStop = { stopCalled = true },
        )

        player.stop()

        assertTrue(stopCalled)
        verify(mockPlayer, never()).stop()
    }

    @Test
    fun `stop delegates to wrapped player when onStop is null`() {
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onStop = null,
        )

        player.stop()

        verify(mockPlayer).stop()
    }

    @Test
    fun `play blocked when playGuard returns false`() {
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            playGuard = { false },
        )

        player.play()

        verify(mockPlayer, never()).play()
    }

    @Test
    fun `play delegates to wrapped player when playGuard true and no onPlay`() {
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            playGuard = { true },
        )

        player.play()

        verify(mockPlayer).play()
    }

    @Test
    fun `play calls onPlay callback instead of wrapped player`() {
        var onPlayCalled = false
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onPlay = { onPlayCalled = true },
        )

        player.play()

        assertTrue(onPlayCalled)
        // onPlay takes precedence — wrapped player's play() should not be called
        verify(mockPlayer, never()).play()
    }

    @Test
    fun `play with guard returning false does not call onPlay`() {
        var onPlayCalled = false
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onPlay = { onPlayCalled = true },
            playGuard = { false },
        )

        player.play()

        assertFalse(onPlayCalled)
        verify(mockPlayer, never()).play()
    }

    @Test
    fun `pause calls onPause callback instead of wrapped player`() {
        var onPauseCalled = false
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onPause = { onPauseCalled = true },
        )

        player.pause()

        assertTrue(onPauseCalled)
        // onPause takes precedence — wrapped player's pause() should not be called
        verify(mockPlayer, never()).pause()
    }

    @Test
    fun `pause delegates to wrapped player when onPause is null`() {
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onPause = null,
        )

        player.pause()

        verify(mockPlayer).pause()
    }

    @Test
    fun `swapPlayer preserves onStop and playGuard`() {
        var stopCalled = false
        var guardChecked = false
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onStop = { stopCalled = true },
            playGuard = {
                guardChecked = true
                false
            },
        )

        val newWrappedPlayer = mock<Player> {
            on { applicationLooper } doReturn Looper.getMainLooper()
        }
        val swapped = player.swapPlayer(newWrappedPlayer)

        swapped.stop()
        swapped.play()

        assertTrue(stopCalled)
        assertTrue(guardChecked)
        verify(newWrappedPlayer, never()).play()
        // onStop takes precedence — wrapped player's stop() should not be called
        verify(newWrappedPlayer, never()).stop()
    }

    @Test
    fun `available commands include COMMAND_STOP`() {
        val commands = forwardingPlayer.availableCommands
        assertTrue(commands.contains(Player.COMMAND_STOP))
    }

    @Test
    fun `updateMetadata does not fire onMediaItemTransition when episode UUID unchanged`() {
        val listener = mock<Player.Listener>()
        forwardingPlayer.addListener(listener)

        val episode = createPodcastEpisode(uuid = "ep-1", title = "First")
        forwardingPlayer.updateMetadata(episode, null)

        verify(listener).onMediaItemTransition(
            forwardingPlayer.currentMediaItem,
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
        )

        val updatedEpisode = createPodcastEpisode(uuid = "ep-1", title = "Updated")
        forwardingPlayer.updateMetadata(updatedEpisode, null)

        verify(listener, times(2)).onMediaMetadataChanged(any())
        verify(listener, times(1)).onMediaItemTransition(any(), any())
    }

    @Test
    fun `updateMetadata fires onMediaItemTransition when episode UUID changes`() {
        val listener = mock<Player.Listener>()
        forwardingPlayer.addListener(listener)

        val episode1 = createPodcastEpisode(uuid = "ep-1", title = "First")
        forwardingPlayer.updateMetadata(episode1, null)

        val episode2 = createPodcastEpisode(uuid = "ep-2", title = "Second")
        forwardingPlayer.updateMetadata(episode2, null)

        verify(listener, times(2)).onMediaItemTransition(any(), any())
    }

    @Test
    fun `seekTo delegates to onSeekTo callback`() {
        var seekPosition: Long? = null
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onSeekTo = { seekPosition = it },
        )

        player.seekTo(42_000L)

        assertEquals(42_000L, seekPosition)
        verify(mockPlayer).seekTo(42_000L)
    }

    @Test
    fun `seekTo with mediaItemIndex delegates to onSeekTo callback`() {
        var seekPosition: Long? = null
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onSeekTo = { seekPosition = it },
        )

        player.seekTo(0, 99_000L)

        assertEquals(99_000L, seekPosition)
        verify(mockPlayer).seekTo(0, 99_000L)
    }

    @Test
    fun `seekTo falls through when no onSeekTo callback`() {
        forwardingPlayer.seekTo(50_000L)

        verify(mockPlayer).seekTo(50_000L)
    }

    @Test
    fun `updateMetadata hides artwork when showArtwork is false`() {
        val episode = createPodcastEpisode(
            uuid = "ep-1",
            title = "Test",
            imageUrl = "https://example.com/art.jpg",
        )
        val podcast = createPodcast(title = "Podcast")

        forwardingPlayer.updateMetadata(episode, podcast, showArtwork = false)

        assertNull(forwardingPlayer.mediaMetadata.artworkUri)
        assertNull(forwardingPlayer.mediaMetadata.artworkData)
    }

    @Test
    fun `updateMetadata shows artwork when showArtwork is true`() {
        val episode = createPodcastEpisode(
            uuid = "ep-1",
            title = "Test",
            imageUrl = "https://example.com/art.jpg",
        )
        val podcast = createPodcast(title = "Podcast")

        forwardingPlayer.updateMetadata(episode, podcast, showArtwork = true)

        assertEquals(Uri.parse("https://example.com/art.jpg"), forwardingPlayer.mediaMetadata.artworkUri)
    }

    @Test
    fun `updateMetadata uses podcast artwork when useEpisodeArtwork is false`() {
        val episode = createPodcastEpisode(
            uuid = "ep-1",
            title = "Test",
            imageUrl = "https://example.com/episode-art.jpg",
        )
        val podcast = createPodcast(title = "Podcast")

        forwardingPlayer.updateMetadata(episode, podcast, useEpisodeArtwork = false)

        val artworkUri = forwardingPlayer.mediaMetadata.artworkUri
        assertEquals(Uri.parse(podcast.getArtworkUrl(480)), artworkUri)
    }

    @Test
    fun `updateMetadata uses episode artwork when useEpisodeArtwork is true`() {
        val episode = createPodcastEpisode(
            uuid = "ep-1",
            title = "Test",
            imageUrl = "https://example.com/art.jpg",
        )
        val podcast = createPodcast(title = "Podcast")

        forwardingPlayer.updateMetadata(episode, podcast, useEpisodeArtwork = true)

        assertEquals(Uri.parse("https://example.com/art.jpg"), forwardingPlayer.mediaMetadata.artworkUri)
    }

    @Test
    fun `updateMetadata falls back to episode artwork when useEpisodeArtwork is false and podcast is null`() {
        val episode = createPodcastEpisode(
            uuid = "ep-1",
            title = "Test",
            imageUrl = "https://example.com/episode-art.jpg",
        )

        forwardingPlayer.updateMetadata(episode, podcast = null, useEpisodeArtwork = false)

        assertEquals(Uri.parse("https://example.com/episode-art.jpg"), forwardingPlayer.mediaMetadata.artworkUri)
    }

    @Test
    fun `swapPlayer preserves onSeekTo callback`() {
        var seekPosition: Long? = null
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onSeekTo = { seekPosition = it },
        )

        val newWrappedPlayer = mock<Player> {
            on { applicationLooper } doReturn Looper.getMainLooper()
        }
        val swapped = player.swapPlayer(newWrappedPlayer)
        swapped.seekTo(77_000L)

        assertEquals(77_000L, seekPosition)
    }

    @Test
    fun `swapPlayer preserves skip lambdas`() {
        var skipForwardCalled = false
        var skipBackCalled = false
        val player = PocketCastsForwardingPlayer(
            wrappedPlayer = mockPlayer,
            onSkipForward = { skipForwardCalled = true },
            onSkipBack = { skipBackCalled = true },
        )

        val newWrappedPlayer = mock<Player> {
            on { applicationLooper } doReturn Looper.getMainLooper()
        }
        val swapped = player.swapPlayer(newWrappedPlayer)

        swapped.seekToNext()
        swapped.seekToPrevious()

        assertTrue(skipForwardCalled)
        assertTrue(skipBackCalled)
    }

    // --- setMediaItems / addMediaItems / prepare interception tests ---

    @Test
    fun `setMediaItems updates currentMediaItem and fires listener events`() {
        val listener = mock<Player.Listener>()
        forwardingPlayer.addListener(listener)

        val metadata = MediaMetadata.Builder()
            .setTitle("Resolved Episode")
            .setArtist("Podcast Name")
            .setIsPlayable(true)
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId("ep-resolved")
            .setMediaMetadata(metadata)
            .build()

        forwardingPlayer.setMediaItems(mutableListOf(mediaItem))

        assertEquals("ep-resolved", forwardingPlayer.currentMediaItem.mediaId)
        assertEquals("Resolved Episode", forwardingPlayer.mediaMetadata.title)
        verify(listener).onMediaMetadataChanged(metadata)
        verify(listener).onMediaItemTransition(
            forwardingPlayer.currentMediaItem,
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED,
        )
    }

    @Test
    fun `setMediaItems does not delegate to wrapped player`() {
        val metadata = MediaMetadata.Builder().setTitle("Test").build()
        val mediaItem = MediaItem.Builder()
            .setMediaId("ep-1")
            .setMediaMetadata(metadata)
            .build()

        forwardingPlayer.setMediaItems(mutableListOf(mediaItem))

        verify(mockPlayer, never()).setMediaItems(any())
        verify(mockPlayer, never()).setMediaItems(any(), any<Boolean>())
        verify(mockPlayer, never()).setMediaItems(any(), any<Int>(), any())
    }

    @Test
    fun `prepare does not delegate to wrapped player`() {
        forwardingPlayer.prepare()

        verify(mockPlayer, never()).prepare()
    }

    @Test
    fun `setMediaItems does not fire onMediaItemTransition when mediaId unchanged`() {
        val listener = mock<Player.Listener>()
        forwardingPlayer.addListener(listener)

        val metadata = MediaMetadata.Builder().setTitle("Test").build()
        val mediaItem = MediaItem.Builder()
            .setMediaId("ep-same")
            .setMediaMetadata(metadata)
            .build()

        forwardingPlayer.setMediaItems(mutableListOf(mediaItem))
        forwardingPlayer.setMediaItems(mutableListOf(mediaItem))

        verify(listener, times(2)).onMediaMetadataChanged(any())
        verify(listener, times(1)).onMediaItemTransition(any(), any())
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
