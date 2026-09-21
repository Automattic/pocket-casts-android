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
            isPlaying = { false },
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
            isPlaying = { false },
        )

        handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY))
        handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY))

        assertEquals(1, immediatePlayCount)

        advanceUntilIdle()
        assertEquals(listOf(MediaEvent.DoubleTap), events)
    }

    @Test
    fun `immediate play failure is reported without losing the resolved double tap`() = runTest {
        val failure = IllegalStateException("Immediate action failed")
        val errors = mutableListOf<Exception>()
        val events = mutableListOf<MediaEvent>()
        val handler = MediaButtonEventHandler(
            scopeProvider = { this },
            onImmediatePlay = { throw failure },
            onMediaEvent = events::add,
            isPlaying = { false },
            onError = errors::add,
        )

        assertTrue(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)))
        assertTrue(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)))

        advanceUntilIdle()
        assertEquals(listOf(failure), errors)
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
            isPlaying = { false },
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
            isPlaying = { false },
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
            isPlaying = { false },
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
            isPlaying = { false },
        )

        assertFalse(handler.handle(keyEvent(KeyEvent.KEYCODE_VOLUME_UP)))
        assertFalse(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.ACTION_UP)))
    }

    @Test
    fun `toggle keys run the immediate action while paused`() = runTest {
        for (keyCode in listOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK)) {
            var immediatePlayCount = 0
            val events = mutableListOf<MediaEvent>()
            val handler = MediaButtonEventHandler(
                scopeProvider = { this },
                onImmediatePlay = { immediatePlayCount++ },
                onMediaEvent = events::add,
                isPlaying = { false },
            )

            assertTrue(handler.handle(keyEvent(keyCode)))
            assertEquals(1, immediatePlayCount)

            advanceUntilIdle()
            assertEquals(emptyList<MediaEvent>(), events)
        }
    }

    @Test
    fun `toggle keys wait for the tap window while playing`() = runTest {
        for (keyCode in listOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK)) {
            var immediatePlayCount = 0
            val events = mutableListOf<MediaEvent>()
            val handler = MediaButtonEventHandler(
                scopeProvider = { this },
                onImmediatePlay = { immediatePlayCount++ },
                onMediaEvent = events::add,
                isPlaying = { true },
            )

            assertTrue(handler.handle(keyEvent(keyCode)))
            assertEquals(0, immediatePlayCount)

            advanceUntilIdle()
            assertEquals(listOf(MediaEvent.SingleTap), events)
        }
    }

    @Test
    fun `rapid toggle keys while paused run the immediate action once and emit a double tap`() = runTest {
        var immediatePlayCount = 0
        val events = mutableListOf<MediaEvent>()
        val handler = MediaButtonEventHandler(
            scopeProvider = { this },
            onImmediatePlay = { immediatePlayCount++ },
            onMediaEvent = events::add,
            isPlaying = { false },
        )

        handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))

        assertEquals(1, immediatePlayCount)

        advanceUntilIdle()
        assertEquals(listOf(MediaEvent.DoubleTap), events)
    }

    private fun keyEvent(
        keyCode: Int,
        action: Int = KeyEvent.ACTION_DOWN,
    ) = KeyEvent(action, keyCode)
}
