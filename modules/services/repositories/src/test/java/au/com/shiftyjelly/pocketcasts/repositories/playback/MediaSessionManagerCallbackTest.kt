package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Intent
import android.view.KeyEvent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.automattic.eventhorizon.EventHorizon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MediaSessionManagerCallbackTest {
    @Test
    fun `legacy KEYCODE_MEDIA_PLAY runs play-only action before the multi-tap window expires`() = runTest {
        val playbackManager = mock<PlaybackManager>()
        val episodeManager = mock<EpisodeManager>()
        val manager = createManager(this, playbackManager, episodeManager)
        val callback = manager.createCallback(this, playbackManager, episodeManager)

        callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_PLAY))

        verify(playbackManager).playIfNotPlaying(SourceView.MEDIA_BUTTON_BROADCAST_ACTION)
        verify(playbackManager, never()).playPause(any())

        advanceUntilIdle()
        verify(playbackManager, never()).playPause(any())
    }

    @Test
    fun `legacy KEYCODE_MEDIA_NEXT suppresses a following KEYCODE_MEDIA_PLAY`() = runTest {
        val playbackManager = mock<PlaybackManager>()
        val episodeManager = mock<EpisodeManager>()
        val settings = mock<Settings>()
        val nextAction = mock<UserSetting<HeadphoneAction>>()
        whenever(settings.headphoneControlsNextAction).thenReturn(nextAction)
        whenever(nextAction.value).thenReturn(HeadphoneAction.SKIP_FORWARD)
        whenever(playbackManager.isPlaying()).thenReturn(true)
        val manager = createManager(this, playbackManager, episodeManager, settings)
        val queuedCommands = mutableListOf<String>()
        val callback = manager.createCallback(
            scope = this,
            playbackManager = playbackManager,
            episodeManager = episodeManager,
            enqueueCommand = { tag, _ -> queuedCommands += tag },
        )

        callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_NEXT))
        callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_PLAY))
        advanceUntilIdle()

        verify(playbackManager, never()).playIfNotPlaying(any())
        assertEquals(listOf("skip forwards"), queuedCommands)
    }

    @Test
    fun `legacy KEYCODE_HEADSETHOOK resolves to play pause`() = runTest {
        val playbackManager = mock<PlaybackManager>()
        val episodeManager = mock<EpisodeManager>()
        val manager = createManager(this, playbackManager, episodeManager)
        val callback = manager.createCallback(this, playbackManager, episodeManager)

        callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_HEADSETHOOK))
        advanceUntilIdle()

        verify(playbackManager).playPause(SourceView.MEDIA_BUTTON_BROADCAST_ACTION)
        verify(playbackManager, never()).playIfNotPlaying(any())
    }

    @Test
    fun `legacy rapid KEYCODE_MEDIA_PLAY resolves previous action after immediate play`() = runTest {
        val playbackManager = mock<PlaybackManager>()
        val episodeManager = mock<EpisodeManager>()
        val settings = mock<Settings>()
        val previousAction = mock<UserSetting<HeadphoneAction>>()
        whenever(settings.headphoneControlsPreviousAction).thenReturn(previousAction)
        whenever(previousAction.value).thenReturn(HeadphoneAction.SKIP_BACK)
        val manager = createManager(this, playbackManager, episodeManager, settings)
        val queuedCommands = mutableListOf<String>()
        val callback = manager.createCallback(
            scope = this,
            playbackManager = playbackManager,
            episodeManager = episodeManager,
            enqueueCommand = { tag, _ -> queuedCommands += tag },
        )

        repeat(3) {
            callback.onMediaButtonEvent(mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_PLAY))
        }

        verify(playbackManager).playIfNotPlaying(SourceView.MEDIA_BUTTON_BROADCAST_ACTION)

        advanceUntilIdle()

        assertEquals(listOf("skip backwards"), queuedCommands)
        verify(playbackManager, never()).playPause(any())
    }

    private fun createManager(
        scope: CoroutineScope,
        playbackManager: PlaybackManager,
        episodeManager: EpisodeManager,
        settings: Settings = mock(),
    ) = MediaSessionManager(
        playbackManager = playbackManager,
        podcastManager = mock<PodcastManager>(),
        episodeManager = episodeManager,
        playlistManager = mock<PlaylistManager>(),
        settings = settings,
        context = RuntimeEnvironment.getApplication(),
        eventHorizon = mock<EventHorizon>(),
        bookmarkManager = mock<BookmarkManager>(),
        browseTreeProvider = mock<BrowseTreeProvider>(),
        applicationScope = scope,
    )

    private fun MediaSessionManager.createCallback(
        scope: CoroutineScope,
        playbackManager: PlaybackManager,
        episodeManager: EpisodeManager,
        enqueueCommand: (String, suspend () -> Unit) -> Unit = { _, _ -> },
    ) = MediaSessionCallback(
        playbackManager = playbackManager,
        episodeManager = episodeManager,
        enqueueCommand = enqueueCommand,
        scopeProvider = { scope },
    )

    private fun mediaButtonIntent(keyCode: Int) = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
        putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    }
}
