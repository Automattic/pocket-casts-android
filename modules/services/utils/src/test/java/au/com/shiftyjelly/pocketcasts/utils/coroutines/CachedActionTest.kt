package au.com.shiftyjelly.pocketcasts.utils.coroutines

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedActionTest {
    @Test
    fun `complete job when action is done`() = runTest {
        val emitter = MutableSharedFlow<Unit>()
        val action = CachedAction<Unit, Unit> { emitter.first() }

        val job = action.run(scope = this)
        assertTrue(job.isActive)
        yield()

        emitter.emit(Unit)
        assertTrue(job.isCompleted)
    }

    @Test
    fun `run a single job`() = runTest {
        val action = CachedAction<Unit, Unit> { awaitCancellation() }

        val job1 = action.run(scope = this)
        val job2 = action.run(scope = this)

        assertEquals(job1, job2)

        job1.cancelAndJoin()
    }

    @Test
    fun `memoize result`() = runTest {
        val emitter = MutableSharedFlow<Int>()
        val action = CachedAction<Unit, Int> { emitter.first() }

        action.run(scope = this)
        yield()
        emitter.emit(1)

        val job = action.run(scope = this)
        assertTrue(job.isCompleted)
        assertEquals(1, job.await())
    }

    @Test
    fun `cancel the job when reset`() = runTest {
        val action = CachedAction<Unit, Unit> { awaitCancellation() }

        val job = action.run(scope = this)
        yield()

        action.reset()

        assertTrue(job.isCancelled)
    }

    @Test
    fun `clear the result when reset`() = runTest {
        val emitter = MutableSharedFlow<Int>()
        val action = CachedAction<Unit, Int> { emitter.first() }

        action.run(scope = this)
        yield()
        emitter.emit(1)

        action.reset()

        val job = action.run(scope = this)
        assertFalse(job.isCompleted)

        job.cancelAndJoin()
    }

    @Test
    fun `allow to run again after reset`() = runTest {
        val emitter = MutableSharedFlow<Int>()
        val action = CachedAction<Unit, Int> { emitter.first() }

        action.run(scope = this)
        yield()
        action.reset()

        val job = action.run(scope = this)
        yield()
        emitter.emit(10)

        assertEquals(10, job.await())
    }
}
