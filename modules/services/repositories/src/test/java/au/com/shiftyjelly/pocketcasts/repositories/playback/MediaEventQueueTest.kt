package au.com.shiftyjelly.pocketcasts.repositories.playback

import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaEventQueueTest {
    @Test
    fun `single tap event`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })

        val event = handler.consumeEvent(MediaEvent.SingleTap)

        assertEquals(MediaEvent.SingleTap, event)
    }

    @Test
    fun `double tap event`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })

        val event = handler.consumeEvent(MediaEvent.DoubleTap)

        assertEquals(MediaEvent.DoubleTap, event)
    }

    @Test
    fun `triple tap event`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })

        val event = handler.consumeEvent(MediaEvent.TripleTap)

        assertEquals(MediaEvent.TripleTap, event)
    }

    @Test
    fun `map single tap events to double tap event`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })

        val firstEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        yield()
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        assertEquals(MediaEvent.DoubleTap, firstEvent.await())
    }

    @Test
    fun `map single tap events to triple tap event`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })

        val firstEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        yield()
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        assertEquals(MediaEvent.TripleTap, firstEvent.await())
    }

    @Test
    fun `map single tap events to triple tap event when event count is higher`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })

        val firstEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        yield()
        repeat(100) {
            assertNull(handler.consumeEvent(MediaEvent.SingleTap))
        }

        assertEquals(MediaEvent.TripleTap, firstEvent.await())
    }

    @Test
    fun `handle an immediate single tap before the multi tap window expires`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })
        var isHandled = false

        val event = async {
            handler.consumeEvent(MediaEvent.SingleTap) {
                isHandled = true
            }
        }

        yield()
        assertTrue(isHandled)
        assertNull(event.await())
    }

    @Test
    fun `map immediate single taps to multi tap events`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })
        var immediateTapCount = 0

        val firstEvent = async {
            handler.consumeEvent(MediaEvent.SingleTap) {
                immediateTapCount++
            }
        }

        yield()
        assertNull(
            handler.consumeEvent(MediaEvent.SingleTap) {
                immediateTapCount++
            },
        )

        assertEquals(1, immediateTapCount)
        assertEquals(MediaEvent.DoubleTap, firstEvent.await())
    }

    @Test
    fun `immediate single tap failure does not orphan the tap window`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })
        val failure = IllegalStateException("Immediate action failed")

        val thrown = runCatching {
            handler.consumeEvent(MediaEvent.SingleTap) { throw failure }
        }.exceptionOrNull()

        assertEquals(failure, thrown)
        assertEquals(MediaEvent.SingleTap, handler.consumeEvent(MediaEvent.SingleTap))
    }

    @Test
    fun `handle concurrent immediate single taps exactly once`() = runBlocking {
        val handler = MediaEventQueue(scopeProvider = { this })
        val immediateTapCount = AtomicInteger()
        val eventCount = 8
        val startBarrier = CyclicBarrier(eventCount)
        val dispatcher = Executors.newFixedThreadPool(eventCount).asCoroutineDispatcher()

        dispatcher.use {
            List(eventCount) {
                async(dispatcher) {
                    startBarrier.await()
                    handler.consumeEvent(MediaEvent.SingleTap) {
                        immediateTapCount.incrementAndGet()
                    }
                }
            }.awaitAll()
        }

        assertEquals(1, immediateTapCount.get())
    }

    @Test
    fun `do not handle immediate single tap while multi tap window is active`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })
        var isHandled = false

        handler.consumeEvent(MediaEvent.DoubleTap)

        assertNull(
            handler.consumeEvent(MediaEvent.SingleTap) {
                isHandled = true
            },
        )
        assertFalse(isHandled)
    }

    @Test
    fun `map single tap events to multi tap event in time window`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })

        val firstEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        delay(600)
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        assertEquals(MediaEvent.DoubleTap, firstEvent.await())
    }

    @Test
    fun `map single tap events to single tap events outside of time window`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })

        val firstEvent = async { handler.consumeEvent(MediaEvent.SingleTap) }

        delay(601)
        val secondEvent = handler.consumeEvent(MediaEvent.SingleTap)
        assertEquals(MediaEvent.SingleTap, secondEvent)

        assertEquals(MediaEvent.SingleTap, firstEvent.await())
    }

    @Test
    fun `do not reset single tap time window with each new event`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })

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
        val handler = MediaEventQueue(scopeProvider = { this })

        handler.consumeEvent(MediaEvent.DoubleTap)

        delay(250)
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        delay(1)
        val event = handler.consumeEvent(MediaEvent.SingleTap)
        assertEquals(MediaEvent.SingleTap, event)
    }

    @Test
    fun `ignore single tap events while triple tap window is active`() = runTest {
        val handler = MediaEventQueue(scopeProvider = { this })

        handler.consumeEvent(MediaEvent.TripleTap)

        delay(250)
        assertNull(handler.consumeEvent(MediaEvent.SingleTap))

        delay(1)
        val event = handler.consumeEvent(MediaEvent.SingleTap)
        assertEquals(MediaEvent.SingleTap, event)
    }
}
