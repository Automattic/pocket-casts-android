package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackManagerPlayIfNotPlayingTest {

    private val playbackManager = mock<PlaybackManager>().also {
        whenever(it.playIfNotPlaying(any())).thenCallRealMethod()
    }

    @Test
    fun `does nothing while already playing`() {
        whenever(playbackManager.isPlaying()).thenReturn(true)

        playbackManager.playIfNotPlaying(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION)

        verify(playbackManager, never()).playQueue(any(), any())
        verify(playbackManager, never()).pause(any(), any())
    }

    @Test
    fun `plays the queue while not playing`() {
        whenever(playbackManager.isPlaying()).thenReturn(false)

        playbackManager.playIfNotPlaying(sourceView = SourceView.MEDIA_BUTTON_BROADCAST_ACTION)

        verify(playbackManager).playQueue(eq(SourceView.MEDIA_BUTTON_BROADCAST_ACTION), any())
    }
}
