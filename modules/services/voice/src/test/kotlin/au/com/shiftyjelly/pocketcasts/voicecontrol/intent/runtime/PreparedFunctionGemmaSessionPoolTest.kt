@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime

import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceIntent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PreparedFunctionGemmaSessionPoolTest {
    @Test
    fun `prepare creates then prefills and reports separate metrics`() = runTest {
        val runtime = FakeRuntime()
        val clock = FakeClock(100, 112, 200, 235)
        val pool = PreparedFunctionGemmaSessionPool(runtime, backgroundScope, elapsedRealtimeMs = clock::elapsed)

        val metrics = pool.prepare("STATIC")

        assertEquals(listOf("create:1", "prefill:1:STATIC"), runtime.calls)
        assertEquals(FunctionGemmaBackend.GPU, pool.backend)
        assertEquals(SessionPreparationMetrics(sessionCreateMs = 12, staticPrefillMs = 35), metrics)
    }

    @Test
    fun `consume reuses conversation when token budget is not exceeded`() = runTest {
        val runtime = FakeRuntime()
        val pool = PreparedFunctionGemmaSessionPool(runtime, this) { testScheduler.currentTime }
        pool.prepare("STATIC")

        val (result, lifecycle) = pool.consume { session ->
            session.prefill("REQUEST")
            session.decode()
        }
        advanceUntilIdle()

        assertEquals("generated:1", result.value)
        assertEquals(0, result.sessionWaitMs)
        assertTrue(lifecycle.reused)
        assertEquals(1, lifecycle.reuseCount)
        assertFalse(lifecycle.rotated)

        assertEquals(2, runtime.createdSessionCount)
        assertEquals(0, runtime.rotationCount)
    }

    @Test
    fun `conversation reused across multiple requests`() = runTest {
        val runtime = FakeRuntime()
        val pool = PreparedFunctionGemmaSessionPool(runtime, this) { testScheduler.currentTime }
        pool.prepare("STATIC")

        pool.consume { it.decode() }
        advanceUntilIdle()

        val (result2, lifecycle2) = pool.consume { it.decode() }
        advanceUntilIdle()

        assertEquals("generated:2", result2.value)
        assertTrue(lifecycle2.reused)
        assertEquals(2, lifecycle2.reuseCount)
        assertEquals(3, runtime.createdSessionCount) // prep + two replacements
        assertEquals(0, runtime.rotationCount)
    }

    @Test
    fun `conversation rotated when token budget is exhausted`() = runTest {
        val runtime = FakeRuntime()
        val pool = PreparedFunctionGemmaSessionPool(
            runtime = runtime,
            scope = this,
            elapsedRealtimeMs = { testScheduler.currentTime },
            maxContextTokens = 2048,
        )
        pool.prepare("STATIC")

        // First request sets baseline at 1700 tokens
        runtime.nextTokenCount = 1700
        val (result1, lifecycle1) = pool.consume { it.decode() }
        advanceUntilIdle()
        assertEquals("generated:1", result1.value)
        assertTrue(lifecycle1.reused)

        // Second request: turn tokens exceed 80% of available (2048-1700=348; 80%=278)
        // 2000 - 1700 = 300 > 278 → rotation
        runtime.nextTokenCount = 2000
        val (result2, lifecycle2) = pool.consume { it.decode() }
        advanceUntilIdle()

        assertEquals("generated:2", result2.value)
        assertTrue(lifecycle2.rotated)
        assertEquals("token_limit", lifecycle2.rotationCause)
        assertTrue(runtime.rotationCount > 0)
    }

    @Test
    fun `consume handles null block return without exception`() = runTest {
        val runtime = FakeRuntime()
        val pool = PreparedFunctionGemmaSessionPool(runtime, this) { testScheduler.currentTime }
        pool.prepare("STATIC")

        // Block that returns null — simulates parse failure
        val (result, lifecycle) = pool.consume<VoiceIntent?> { session ->
            session.prefill("REQUEST")
            session.decode()
            null // parse returned null
        }

        assertNull(result.value)
        assertTrue(lifecycle.reused)
        // No exception thrown; replacement scheduled normally
        advanceUntilIdle()
        assertEquals(2, runtime.createdSessionCount)
    }

    @Test
    fun `empty decode does not count as reuse turn`() = runTest {
        val runtime = FakeRuntime()
        val pool = PreparedFunctionGemmaSessionPool(runtime, this) { testScheduler.currentTime }
        pool.prepare("STATIC")

        pool.consume { it.decode() }
        advanceUntilIdle()
        val (_, lifecycle2) = pool.consume { it.decode() }
        advanceUntilIdle()

        assertTrue(lifecycle2.reused)
        assertEquals(2, lifecycle2.reuseCount)
    }

    @Test
    fun `second consumer waits for blocked replacement`() = runTest {
        val replacementGate = BlockingGate()
        val runtime = FakeRuntime(prefillGates = mapOf(2 to replacementGate))
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pool = PreparedFunctionGemmaSessionPool(runtime, workerScope) { testScheduler.currentTime }
        try {
            pool.prepare("STATIC")

            // Advance past timeout to trigger rotation → replacement is scheduled
            testScheduler.advanceTimeBy(11_000)
            pool.consume { it.decode() }
            replacementGate.awaitEntered()

            val second = async { pool.consume { it.decode() } }
            yield()

            assertFalse(second.isCompleted)
            assertEquals(1, runtime.decodeCount)

            replacementGate.release()
            assertEquals("generated:2", second.await().first.value)
        } finally {
            replacementGate.release()
            pool.close()
            workerScope.cancel()
        }
    }

    @Test
    fun `cancelled consumer closes its session and replenishes`() = runTest {
        val runtime = FakeRuntime()
        val pool = PreparedFunctionGemmaSessionPool(runtime, this) { testScheduler.currentTime }
        pool.prepare("STATIC")

        try {
            pool.consume<Nothing> { throw CancellationException("cancelled") }
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            advanceUntilIdle()
        }

        assertEquals(listOf(1), runtime.closedSessionIdsSnapshot)
        assertEquals(2, runtime.createdSessionCount)
        assertEquals(2, runtime.staticPrefillCount)
    }

    @Test
    fun `close during preparation cancels waiter and closes session and runtime`() = runTest {
        val preparationGate = BlockingGate()
        val waiterStarted = CountDownLatch(1)
        val runtime = FakeRuntime(prefillGates = mapOf(1 to preparationGate))
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pool = PreparedFunctionGemmaSessionPool(
            runtime = runtime,
            scope = workerScope,
            elapsedRealtimeMs = { testScheduler.currentTime },
            maxContextTokens = 2048,
            beforePrepareEngineLock = {},
            beforeAvailabilityAwait = waiterStarted::countDown,
        )
        val executor = Executors.newFixedThreadPool(3)
        try {
            val preparation = executor.submit<Throwable?> {
                runCatching { runBlocking { pool.prepare("STATIC") } }.exceptionOrNull()
            }
            preparationGate.awaitEntered()
            val waiter = executor.submit<Throwable?> {
                runCatching { runBlocking { pool.consume { it.decode() } } }.exceptionOrNull()
            }
            check(waiterStarted.await(5, TimeUnit.SECONDS))
            val closing = executor.submit { pool.close() }

            assertTrue(waiter.get(5, TimeUnit.SECONDS) is CancellationException)
            preparationGate.release()

            val preparationFailure = requireNotNull(preparation.get(5, TimeUnit.SECONDS))
            assertTrue(preparationFailure is IllegalStateException)
            assertEquals("FunctionGemma session pool is closing", preparationFailure.message)
            closing.get(5, TimeUnit.SECONDS)

            assertEquals(listOf(1), runtime.closedSessionIdsSnapshot)
            assertEquals(1, runtime.closeCount)
        } finally {
            preparationGate.release()
            pool.close()
            workerScope.cancel()
            executor.shutdownNow()
        }
    }

    @Test
    fun `prepare does not create a session when close wins before engine lock`() = runTest {
        val prepareEngineGate = BlockingGate()
        val runtime = FakeRuntime()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pool = PreparedFunctionGemmaSessionPool(
            runtime = runtime,
            scope = workerScope,
            elapsedRealtimeMs = { testScheduler.currentTime },
            maxContextTokens = 2048,
            beforePrepareEngineLock = prepareEngineGate::block,
        )
        try {
            val preparation = workerScope.async { pool.prepare("STATIC") }
            prepareEngineGate.awaitEntered()

            pool.close()
            prepareEngineGate.release()

            try {
                preparation.await()
                fail("Expected preparation to fail after close")
            } catch (_: IllegalStateException) {
                // Expected because close won before session creation.
            }

            assertEquals(listOf("runtime-close"), runtime.calls)
            assertEquals(0, runtime.createdSessionCount)
            assertEquals(1, runtime.closeCount)
        } finally {
            prepareEngineGate.release()
            pool.close()
            workerScope.cancel()
        }
    }

    @Test
    fun `close during replenishment cancels waiter without leaking replacement`() = runTest {
        val publishedGate = BlockingGate()
        val runtime = FakeRuntime()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pool = PreparedFunctionGemmaSessionPool(
            runtime = runtime,
            scope = workerScope,
            elapsedRealtimeMs = { testScheduler.currentTime },
            maxContextTokens = 2048,
            beforePrepareEngineLock = {},
            afterReplacementPublication = publishedGate::block,
        )
        try {
            pool.prepare("STATIC")

            testScheduler.advanceTimeBy(11_000)
            pool.consume { it.decode() }
            publishedGate.awaitEntered()
            val waiter = async { pool.consume { it.decode() } }
            val closing = async(Dispatchers.Default) { pool.close() }

            awaitCondition { waiter.isCompleted }
            assertTrue(waiter.isCancelled)

            publishedGate.release()
            withTimeout(5_000) { closing.await() }

            assertEquals(listOf(1, 2), runtime.closedSessionIdsSnapshot.sorted())
            assertEquals(1, runtime.closeCount)
        } finally {
            publishedGate.release()
            pool.close()
            workerScope.cancel()
        }
    }

    @Test
    fun `preparation exception closes failed session and propagates`() = runTest {
        val failure = IllegalStateException("prefill failed")
        val runtime = FakeRuntime(prefillFailures = mapOf(1 to failure))
        val pool = PreparedFunctionGemmaSessionPool(runtime, this) { testScheduler.currentTime }

        try {
            pool.prepare("STATIC")
            fail("Expected preparation failure")
        } catch (actual: IllegalStateException) {
            assertEquals(failure, actual)
        }

        assertEquals(listOf(1), runtime.closedSessionIdsSnapshot)
    }

    @Test
    fun `preparation failure fails waiter and rejects repeated prepare`() = runTest {
        val failure = IllegalStateException("prefill failed")
        val runtime = FakeRuntime(prefillFailures = mapOf(1 to failure))
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pool = PreparedFunctionGemmaSessionPool(runtime, workerScope) { testScheduler.currentTime }
        try {
            val waiter = workerScope.async { pool.consume { it.decode() } }

            assertSameFailure(failure) { pool.prepare("STATIC") }
            assertSameFailure(failure) { waiter.await() }
            assertSameFailure(failure) { pool.prepare("STATIC") }

            pool.close()
            assertEquals(1, runtime.closeCount)
        } finally {
            pool.close()
            workerScope.cancel()
        }
    }

    @Test
    fun `duplicate prepare does not poison an already prepared session`() = runTest {
        val runtime = FakeRuntime()
        val pool = PreparedFunctionGemmaSessionPool(runtime, backgroundScope) { testScheduler.currentTime }
        pool.prepare("STATIC")

        try {
            pool.prepare("OTHER")
            fail("Expected duplicate prepare rejection")
        } catch (_: IllegalStateException) {
            // Expected.
        }

        assertEquals("generated:1", pool.consume { it.decode() }.first.value)
        pool.close()
    }

    @Test
    fun `cancelled prepare does not start native work and fails readiness`() = runTest {
        val prepareGate = BlockingGate()
        val runtime = FakeRuntime()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pool = PreparedFunctionGemmaSessionPool(
            runtime = runtime,
            scope = workerScope,
            elapsedRealtimeMs = { testScheduler.currentTime },
            maxContextTokens = 2048,
            beforePrepareEngineLock = prepareGate::block,
        )
        try {
            val preparation = workerScope.async { pool.prepare("STATIC") }
            prepareGate.awaitEntered()
            preparation.cancel()
            prepareGate.release()
            preparation.join()

            assertEquals(0, runtime.createdSessionCount)
            try {
                pool.consume { it.decode() }
                fail("Expected cancelled readiness")
            } catch (_: CancellationException) {
                // Expected.
            }
        } finally {
            prepareGate.release()
            pool.close()
            workerScope.cancel()
        }
    }

    @Test
    fun `cancelled owner scope closes replacement created by non-preemptible native work`() = runTest {
        val publishedGate = BlockingGate()
        val runtime = FakeRuntime()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pool = PreparedFunctionGemmaSessionPool(
            runtime = runtime,
            scope = workerScope,
            elapsedRealtimeMs = { testScheduler.currentTime },
            maxContextTokens = 2048,
            beforePrepareEngineLock = {},
            afterReplacementPublication = publishedGate::block,
        )
        try {
            pool.prepare("STATIC")

            testScheduler.advanceTimeBy(11_000)
            pool.consume { it.decode() }
            publishedGate.awaitEntered()
            val waiter = workerScope.async { pool.consume { it.decode() } }

            workerScope.cancel()
            publishedGate.release()
            awaitCondition { 2 in runtime.closedSessionIdsSnapshot }

            try {
                waiter.await()
                fail("Expected replacement cancellation")
            } catch (_: CancellationException) {
                // Expected.
            }
            assertEquals(listOf(1, 2), runtime.closedSessionIdsSnapshot.sorted())
        } finally {
            publishedGate.release()
            pool.close()
        }
    }

    @Test
    fun `already cancelled owner scope fails replacement readiness`() = runTest {
        val runtime = FakeRuntime()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pool = PreparedFunctionGemmaSessionPool(runtime, workerScope) { testScheduler.currentTime }
        try {
            pool.prepare("STATIC")

            pool.consume {
                workerScope.cancel()
                it.decode()
            }

            // Replacement runs in cancelled scope → pool enters failed state
            val failure = withTimeout(5_000) {
                runCatching { pool.consume { it.decode() } }.exceptionOrNull()
            }
            assertTrue(failure is CancellationException)
        } finally {
            pool.close()
        }
    }

    @Test
    fun `cancellation after replacement publication removes and closes published session`() = runTest {
        val publishedGate = BlockingGate()
        val runtime = FakeRuntime()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pool = PreparedFunctionGemmaSessionPool(
            runtime = runtime,
            scope = workerScope,
            elapsedRealtimeMs = { testScheduler.currentTime },
            maxContextTokens = 2048,
            beforePrepareEngineLock = {},
            afterReplacementPublication = publishedGate::block,
        )
        try {
            pool.prepare("STATIC")

            testScheduler.advanceTimeBy(11_000)
            pool.consume { it.decode() }
            publishedGate.awaitEntered()

            workerScope.cancel()
            publishedGate.release()
            awaitCondition { 2 in runtime.closedSessionIdsSnapshot }

            val failure = runCatching { pool.consume { it.decode() } }.exceptionOrNull()
            assertTrue(failure is CancellationException)
            assertEquals(listOf(1, 2), runtime.closedSessionIdsSnapshot.sorted())
            assertEquals(1, runtime.decodeCount)
        } finally {
            publishedGate.release()
            pool.close()
        }
    }

    @Test
    fun `concurrent close waits for first close to finish`() = runTest {
        val runtimeCloseGate = BlockingGate()
        val runtime = FakeRuntime(runtimeCloseGate = runtimeCloseGate)
        val pool = PreparedFunctionGemmaSessionPool(runtime, backgroundScope) { testScheduler.currentTime }
        pool.prepare("STATIC")
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit { pool.close() }
            runtimeCloseGate.awaitEntered()
            val second = executor.submit { pool.close() }

            assertFalse(second.isDone)
            runtimeCloseGate.release()
            first.get(5, TimeUnit.SECONDS)
            second.get(5, TimeUnit.SECONDS)

            assertEquals(1, runtime.closeCount)
        } finally {
            runtimeCloseGate.release()
            executor.shutdownNow()
        }
    }

    @Test
    fun `reentrant close is rejected until consumer block unwinds`() = runTest {
        val runtime = FakeRuntime()
        val pool = PreparedFunctionGemmaSessionPool(runtime, backgroundScope) { testScheduler.currentTime }
        pool.prepare("STATIC")

        pool.consume {
            try {
                pool.close()
                fail("Expected reentrant close rejection")
            } catch (error: IllegalStateException) {
                assertEquals("Cannot close FunctionGemma session pool from an active consumer", error.message)
            }
            assertEquals(0, runtime.closeCount)
            it.decode()
        }

        pool.close()
        assertEquals(1, runtime.closeCount)
    }

    @Test
    fun `session wait measures only prepared availability wait`() = runTest {
        var now = 0L
        val firstConsumerGate = BlockingGate()
        val replacementGate = BlockingGate()
        val consumerCount = AtomicInteger()
        val secondAwaitStarted = CountDownLatch(1)
        val runtime = FakeRuntime(prefillGates = mapOf(2 to replacementGate))
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pool = PreparedFunctionGemmaSessionPool(
            runtime = runtime,
            scope = workerScope,
            elapsedRealtimeMs = { now },
            maxContextTokens = 2048,
            beforePrepareEngineLock = {},
            beforeConsumerMutex = {
                consumerCount.incrementAndGet()
            },
            beforeAvailabilityAwait = {
                if (consumerCount.get() == 2) {
                    secondAwaitStarted.countDown()
                }
            },
        )
        try {
            pool.prepare("STATIC")

            val first = workerScope.async {
                pool.consume {
                    firstConsumerGate.block()
                    it.decode()
                }
            }
            firstConsumerGate.awaitEntered()

            now = 12_000
            val second = workerScope.async { pool.consume { it.decode() } }
            now = 13_000
            firstConsumerGate.release()
            replacementGate.awaitEntered()
            check(secondAwaitStarted.await(5, TimeUnit.SECONDS))
            now = 14_000
            replacementGate.release()

            first.await()
            assertTrue(second.await().first.sessionWaitMs >= 0)
        } finally {
            firstConsumerGate.release()
            replacementGate.release()
            pool.close()
            workerScope.cancel()
        }
    }

    @Test
    fun `session close failure fails waiter and preserves primary block failure`() = runTest {
        val blockFailure = IllegalArgumentException("block failed")
        val closeFailure = IllegalStateException("close failed")
        val runtime = FakeRuntime(sessionCloseFailures = mapOf(1 to closeFailure))
        val consumerGate = BlockingGate()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pool = PreparedFunctionGemmaSessionPool(runtime, workerScope) { testScheduler.currentTime }
        try {
            pool.prepare("STATIC")

            val first = workerScope.async<Throwable> {
                var captured: Throwable? = null
                try {
                    pool.consume<Nothing> {
                        consumerGate.block()
                        throw blockFailure
                    }
                } catch (error: Throwable) {
                    captured = error
                }
                checkNotNull(captured)
            }
            consumerGate.awaitEntered()
            val waiter = workerScope.async { pool.consume { it.decode() } }
            consumerGate.release()

            val actual = first.await()
            assertTrue(actual is IllegalArgumentException)
            assertEquals(blockFailure.message, actual.message)
            assertEquals(listOf(closeFailure.message), actual.suppressed.map { it.message })
            assertSameFailure(closeFailure) { waiter.await() }

            pool.close()
            assertEquals(1, runtime.closeCount)
        } finally {
            consumerGate.release()
            pool.close()
            workerScope.cancel()
        }
    }

    @Test
    fun `close is idempotent and closes prepared session and runtime once`() = runTest {
        val runtime = FakeRuntime()
        val pool = PreparedFunctionGemmaSessionPool(runtime, this) { testScheduler.currentTime }
        pool.prepare("STATIC")

        pool.close()
        pool.close()

        assertEquals(listOf(1), runtime.closedSessionIdsSnapshot)
        assertEquals(1, runtime.closeCount)
    }

    private suspend fun awaitCondition(condition: () -> Boolean) {
        withTimeout(5_000) {
            while (!condition()) {
                yield()
            }
        }
    }

    private suspend fun assertSameFailure(
        expected: Throwable,
        block: suspend () -> Unit,
    ) {
        val actual = runCatching { block() }.exceptionOrNull() ?: throw AssertionError("Expected failure")
        assertEquals(expected::class, actual::class)
        assertEquals(expected.message, actual.message)
    }

    private class FakeClock(
        vararg values: Long,
    ) {
        private val values = ArrayDeque(values.toList())

        fun elapsed(): Long = values.removeFirst()
    }

    private class BlockingGate {
        private val entered = CountDownLatch(1)
        private val released = CountDownLatch(1)

        fun block() {
            entered.countDown()
            check(released.await(5, TimeUnit.SECONDS)) { "Timed out waiting for gate release" }
        }

        suspend fun awaitEntered() {
            withTimeout(5_000) {
                while (entered.count > 0) {
                    yield()
                }
            }
        }

        fun release() {
            released.countDown()
        }
    }

    private class FakeRuntime(
        private val prefillGates: Map<Int, BlockingGate> = emptyMap(),
        private val prefillFailures: Map<Int, RuntimeException> = emptyMap(),
        private val sessionCloseFailures: Map<Int, RuntimeException> = emptyMap(),
        private val runtimeCloseGate: BlockingGate? = null,
    ) : FunctionGemmaRuntime {
        override val backend = FunctionGemmaBackend.GPU

        val calls = mutableListOf<String>()
        private val closedSessionIds = mutableListOf<Int>()
        private val nextSessionId = AtomicInteger()
        private val createdSessions = AtomicInteger()
        private val staticPrefills = AtomicInteger()
        private val decodes = AtomicInteger()
        private val closes = AtomicInteger()

        var nextTokenCount: Int = 0
        var rotationCount: Int = 0

        val createdSessionCount get() = createdSessions.get()
        val staticPrefillCount get() = staticPrefills.get()
        val decodeCount get() = decodes.get()
        val closeCount get() = closes.get()
        val closedSessionIdsSnapshot get() = synchronized(closedSessionIds) { closedSessionIds.toList() }

        override fun createSession(): FunctionGemmaSession {
            val id = nextSessionId.incrementAndGet()
            createdSessions.incrementAndGet()
            record("create:$id")
            return FakeSession(id)
        }

        override fun createSessionWithNewConversation(systemInstruction: String): FunctionGemmaSession {
            rotationCount++
            val id = nextSessionId.incrementAndGet()
            createdSessions.incrementAndGet()
            record("rotate:$id:$systemInstruction")
            // Treat the system instruction like a static prefill so replacement gates work
            return FakeSession(id).also {
                it.prefill("STATIC")
            }
        }

        override fun close() {
            closes.incrementAndGet()
            record("runtime-close")
            runtimeCloseGate?.block()
        }

        private fun record(call: String) {
            synchronized(calls) {
                calls += call
            }
        }

        private inner class FakeSession(
            private val id: Int,
        ) : FunctionGemmaSession {
            override val tokenCount: Int
                get() = nextTokenCount

            override fun prefill(text: String) {
                record("prefill:$id:$text")
                if (text == "STATIC") {
                    staticPrefills.incrementAndGet()
                    prefillGates[id]?.block()
                    prefillFailures[id]?.let { throw it }
                }
            }

            override fun decode(): String {
                decodes.incrementAndGet()
                record("decode:$id")
                return "generated:$id"
            }

            override fun close() {
                synchronized(closedSessionIds) {
                    closedSessionIds += id
                }
                record("close:$id")
                sessionCloseFailures[id]?.let { throw it }
            }
        }
    }
}
