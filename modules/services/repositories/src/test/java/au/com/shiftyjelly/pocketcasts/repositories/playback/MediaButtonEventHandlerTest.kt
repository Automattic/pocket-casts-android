package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MediaButtonEventHandlerTest {
    @Test
    fun `KEYCODE_MEDIA_PLAY runs the immediate action without a delayed single tap`() = runTest {
        var immediatePlayCount = 0
        val events = mutableListOf<MediaEvent>()
        val handler = MediaButtonEventHandler(
            scopeProvider = { this },
            onImmediatePlay = { immediatePlayCount++ },
            onMediaEvent = events::add,
        )

        assertTrue(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)))
        assertEquals(1, immediatePlayCount)
        assertEquals(emptyList<MediaEvent>(), events)

        advanceUntilIdle()
        assertEquals(emptyList<MediaEvent>(), events)
    }

    @Test
    fun `rapid KEYCODE_MEDIA_PLAY events run the immediate action once and emit a double tap`() = runTest {
        var immediatePlayCount = 0
        val events = mutableListOf<MediaEvent>()
        val handler = MediaButtonEventHandler(
            scopeProvider = { this },
            onImmediatePlay = { immediatePlayCount++ },
            onMediaEvent = events::add,
        )

        handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY))
        handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY))

        assertEquals(1, immediatePlayCount)

        advanceUntilIdle()
        assertEquals(listOf(MediaEvent.DoubleTap), events)
    }

    @Test
    fun `KEYCODE_MEDIA_NEXT suppresses a following KEYCODE_MEDIA_PLAY`() = runTest {
        var immediatePlayCount = 0
        val events = mutableListOf<MediaEvent>()
        val handler = MediaButtonEventHandler(
            scopeProvider = { this },
            onImmediatePlay = { immediatePlayCount++ },
            onMediaEvent = events::add,
        )

        handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_NEXT))
        handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY))

        assertEquals(0, immediatePlayCount)

        advanceUntilIdle()
        assertEquals(listOf(MediaEvent.DoubleTap), events)
    }

    @Test
    fun `resolved multi tap actions are deferred beyond event registration`() = runTest {
        val events = mutableListOf<MediaEvent>()
        val handler = MediaButtonEventHandler(
            scopeProvider = { this },
            onImmediatePlay = {},
            onMediaEvent = events::add,
        )

        assertTrue(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_NEXT)))
        assertEquals(emptyList<MediaEvent>(), events)

        runCurrent()
        assertEquals(listOf(MediaEvent.DoubleTap), events)
    }

    @Test
    fun `cancelled scope does not handle events`() = runTest {
        val cancelledJob = Job().apply { cancel() }
        val cancelledScope = CoroutineScope(coroutineContext + cancelledJob)
        var immediatePlayCount = 0
        val events = mutableListOf<MediaEvent>()
        val handler = MediaButtonEventHandler(
            scopeProvider = { cancelledScope },
            onImmediatePlay = { immediatePlayCount++ },
            onMediaEvent = events::add,
        )

        assertTrue(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)))
        assertEquals(0, immediatePlayCount)
        assertEquals(emptyList<MediaEvent>(), events)
    }

    @Test
    fun `unhandled key events return false`() = runTest {
        val handler = MediaButtonEventHandler(
            scopeProvider = { this },
            onImmediatePlay = {},
            onMediaEvent = {},
        )

        assertFalse(handler.handle(keyEvent(KeyEvent.KEYCODE_VOLUME_UP)))
        assertFalse(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.ACTION_UP)))
    }

    private fun keyEvent(
        keyCode: Int,
        action: Int = KeyEvent.ACTION_DOWN,
    ) = KeyEvent(action, keyCode)
}
