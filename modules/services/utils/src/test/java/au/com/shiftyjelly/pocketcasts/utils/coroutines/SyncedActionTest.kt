package au.com.shiftyjelly.pocketcasts.utils.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncedActionTest {
    @Test
    fun `return the result`() = runTest {
        val action = SyncedAction<Unit, Int> { 100 }

        val result = action.run(scope = this)

        assertEquals(100, result)
    }

    @Test
    fun `run a single job`() = runTest {
        var counter = 0
        val action = SyncedAction<Unit, Int> { counter++ }

        val results = List(10) { async { action.run(scope = this) } }
        advanceUntilIdle()

        assertEquals(List(10) { 0 }, results.awaitAll())
        assertEquals(1, counter)
    }

    @Test
    fun `allow to run again`() = runTest {
        var counter = 0
        val action = SyncedAction<Unit, Int> { counter++ }

        val result1 = action.run(scope = this)
        assertEquals(0, result1)

        val result2 = action.run(scope = this)
        assertEquals(1, result2)
    }
}
