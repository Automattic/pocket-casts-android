package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.advanceTimeBy
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
    fun `immediate play failure still resolves the single tap action`() = runTest {
        for (keyCode in listOf(KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK)) {
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

            assertTrue(handler.handle(keyEvent(keyCode)))

            advanceUntilIdle()
            assertEquals(listOf(failure), errors)
            assertEquals(listOf(MediaEvent.SingleTap), events)
        }
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
        assertFalse(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE, repeatCount = 1)))
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

    @Test
    fun `held KEYCODE_MEDIA_PLAY while paused plays once without a multi tap`() = runTest {
        var immediatePlayCount = 0
        val events = mutableListOf<MediaEvent>()
        val handler = MediaButtonEventHandler(
            scopeProvider = { this },
            onImmediatePlay = { immediatePlayCount++ },
            onMediaEvent = events::add,
            isPlaying = { false },
        )

        assertTrue(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)))
        advanceTimeBy(490)
        for (repeatCount in 1..3) {
            assertTrue(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY, repeatCount = repeatCount)))
            advanceTimeBy(50)
        }

        advanceUntilIdle()
        assertEquals(1, immediatePlayCount)
        assertEquals(emptyList<MediaEvent>(), events)
    }

    @Test
    fun `held toggle keys while playing resolve a single tap`() = runTest {
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
            advanceTimeBy(400)
            for (repeatCount in 1..3) {
                assertTrue(handler.handle(keyEvent(keyCode, repeatCount = repeatCount)))
                advanceTimeBy(50)
            }

            advanceUntilIdle()
            assertEquals(0, immediatePlayCount)
            assertEquals(listOf(MediaEvent.SingleTap), events)
        }
    }

    @Test
    fun `held toggle keys while paused play once without a multi tap`() = runTest {
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
            advanceTimeBy(400)
            assertTrue(handler.handle(keyEvent(keyCode, repeatCount = 1)))

            advanceUntilIdle()
            assertEquals(1, immediatePlayCount)
            assertEquals(emptyList<MediaEvent>(), events)
        }
    }

    @Test
    fun `held toggle key repeats after the tap window do not start a new tap`() = runTest {
        val events = mutableListOf<MediaEvent>()
        val handler = MediaButtonEventHandler(
            scopeProvider = { this },
            onImmediatePlay = {},
            onMediaEvent = events::add,
            isPlaying = { true },
        )

        handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        advanceTimeBy(700)
        for (repeatCount in 1..3) {
            assertTrue(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, repeatCount = repeatCount)))
            advanceTimeBy(50)
        }

        advanceUntilIdle()
        assertEquals(listOf(MediaEvent.SingleTap), events)
    }

    @Test
    fun `tap followed by a hold within the tap window resolves a double tap`() = runTest {
        val events = mutableListOf<MediaEvent>()
        val handler = MediaButtonEventHandler(
            scopeProvider = { this },
            onImmediatePlay = {},
            onMediaEvent = events::add,
            isPlaying = { true },
        )

        handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        advanceTimeBy(200)
        handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        advanceTimeBy(300)
        assertTrue(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, repeatCount = 1)))

        advanceUntilIdle()
        assertEquals(listOf(MediaEvent.DoubleTap), events)
    }

    @Test
    fun `held KEYCODE_MEDIA_NEXT still emits each repeat`() = runTest {
        val events = mutableListOf<MediaEvent>()
        val handler = MediaButtonEventHandler(
            scopeProvider = { this },
            onImmediatePlay = {},
            onMediaEvent = events::add,
            isPlaying = { true },
        )

        for (repeatCount in 0..2) {
            assertTrue(handler.handle(keyEvent(KeyEvent.KEYCODE_MEDIA_NEXT, repeatCount = repeatCount)))
            runCurrent()
        }

        advanceUntilIdle()
        assertEquals(List(3) { MediaEvent.DoubleTap }, events)
    }

    private fun keyEvent(
        keyCode: Int,
        action: Int = KeyEvent.ACTION_DOWN,
        repeatCount: Int = 0,
    ) = KeyEvent(0L, 0L, action, keyCode, repeatCount)
}
