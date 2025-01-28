package au.com.shiftyjelly.pocketcasts.repositories.playback

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaEventQueueTest {
    @Test
    fun `single tap event`() = runTest {
        val handler = MediaEventQueue(this)

        val event = handler.consumeEvent(MediaEvent.SingleTap)

        assertEquals(MediaEvent.SingleTap, event)
    }

    @Test
    fun `double tap event`() = runTest {
        val handler = MediaEventQueue(this)

        val event = handler.consumeEvent(MediaEvent.DoubleTap)

        assertEquals(MediaEvent.DoubleTap, event)
    }

    @Test
    fun `triple tap event`() = runTest {
        val handler = MediaEventQueue(this)

        val event = handler.consumeEvent(MediaEvent.TripleTap)

        assertEquals(MediaEvent.TripleTap, event)
    }

    @Test
    fun `map single tap events to double tap event`() = runTest {
        val handler = MediaEventQueue(this)

        val firstEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        yield()
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        assertEquals(MediaEvent.DoubleTap, firstEvent.await())
    }

    @Test
    fun `map single tap events to triple tap event`() = runTest {
        val handler = MediaEventQueue(this)

        val firstEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        yield()
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        assertEquals(MediaEvent.TripleTap, firstEvent.await())
    }

    @Test
    fun `map single tap events to triple tap event when event count is higher`() = runTest {
        val handler = MediaEventQueue(this)

        val firstEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        yield()
        repeat(100) {
            assertNull(handler.consumeEvent(MediaEvent.SingleTap))
        }

        assertEquals(MediaEvent.TripleTap, firstEvent.await())
    }

    @Test
    fun `map single tap events to multi tap event in time window`() = runTest {
        val handler = MediaEventQueue(this)

        val firstEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        delay(600)
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        assertEquals(MediaEvent.DoubleTap, firstEvent.await())
    }

    @Test
    fun `map single tap events to single tap events outside of time window`() = runTest {
        val handler = MediaEventQueue(this)

        val firstEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        delay(601)
        val secondEvent = handler.consumeEvent(MediaEvent.SingleTap)
        assertEquals(MediaEvent.SingleTap, secondEvent)

        assertEquals(MediaEvent.SingleTap, firstEvent.await())
    }

    @Test
    fun `do not reset single tap time window with each new event`() = runTest {
        val handler = MediaEventQueue(this)

        val firstEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        delay(250)
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        delay(250)
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        delay(250)
        val secondEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        yield()
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        assertEquals(MediaEvent.TripleTap, firstEvent.await())
        assertEquals(MediaEvent.DoubleTap, secondEvent.await())
    }

    @Test
    fun `ignore single tap events while double tap window is active`() = runTest {
        val handler = MediaEventQueue(this)

        handler.consumeEvent(MediaEvent.DoubleTap)

        delay(250)
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        delay(1)
        val event = handler.consumeEvent(MediaEvent.SingleTap)
        assertEquals(MediaEvent.SingleTap, event)
    }

    @Test
    fun `ignore single tap events while triple tap window is active`() = runTest {
        val handler = MediaEventQueue(this)

        handler.consumeEvent(MediaEvent.TripleTap)

        delay(250)
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        delay(1)
        val event = handler.consumeEvent(MediaEvent.SingleTap)
        assertEquals(MediaEvent.SingleTap, event)
    }
}
