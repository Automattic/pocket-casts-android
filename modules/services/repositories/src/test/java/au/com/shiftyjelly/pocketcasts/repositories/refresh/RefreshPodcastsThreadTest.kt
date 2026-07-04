package au.com.shiftyjelly.pocketcasts.repositories.refresh

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.minutes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPodcastsThreadTest {

    @After
    fun tearDown() {
        RefreshPodcastsThread.releaseRefreshSlotForTesting()
        RefreshPodcastsThread.clearLastRefreshTime()
    }

    @Test
    fun `refresh slot is blocked inside throttle window`() {
        val throttleMs = 5.minutes.inWholeMilliseconds

        assertTrue(
            RefreshPodcastsThread.tryAcquireRefreshSlotForTesting(
                throttleMs = throttleMs,
                now = 10.minutes.inWholeMilliseconds,
            ),
        )
        RefreshPodcastsThread.releaseRefreshSlotForTesting()

        assertFalse(
            RefreshPodcastsThread.tryAcquireRefreshSlotForTesting(
                throttleMs = throttleMs,
                now = 14.minutes.inWholeMilliseconds,
            ),
        )
        assertTrue(
            RefreshPodcastsThread.tryAcquireRefreshSlotForTesting(
                throttleMs = throttleMs,
                now = 16.minutes.inWholeMilliseconds,
            ),
        )
    }

    @Test
    fun `refresh slot is blocked while refresh is already in progress`() {
        val throttleMs = 5.minutes.inWholeMilliseconds

        assertTrue(
            RefreshPodcastsThread.tryAcquireRefreshSlotForTesting(
                throttleMs = throttleMs,
                now = 10.minutes.inWholeMilliseconds,
            ),
        )
        assertFalse(
            RefreshPodcastsThread.tryAcquireRefreshSlotForTesting(
                throttleMs = throttleMs,
                now = 16.minutes.inWholeMilliseconds,
            ),
        )

        RefreshPodcastsThread.releaseRefreshSlotForTesting()

        assertTrue(
            RefreshPodcastsThread.tryAcquireRefreshSlotForTesting(
                throttleMs = throttleMs,
                now = 16.minutes.inWholeMilliseconds,
            ),
        )
    }

    @Test
    fun `refresh slot is claimed by only one concurrent caller`() {
        val callerCount = 32
        val executor = Executors.newFixedThreadPool(callerCount)
        val start = CountDownLatch(1)
        val done = CountDownLatch(callerCount)
        val allowedCount = AtomicInteger()

        try {
            repeat(callerCount) {
                executor.execute {
                    start.await()
                    if (
                        RefreshPodcastsThread.tryAcquireRefreshSlotForTesting(
                            throttleMs = 5.minutes.inWholeMilliseconds,
                            now = 10.minutes.inWholeMilliseconds,
                        )
                    ) {
                        allowedCount.incrementAndGet()
                    }
                    done.countDown()
                }
            }

            start.countDown()

            assertTrue("Timed out waiting for refresh slot callers", done.await(5, TimeUnit.SECONDS))
            assertEquals(1, allowedCount.get())
        } finally {
            executor.shutdownNow()
        }
    }
}
