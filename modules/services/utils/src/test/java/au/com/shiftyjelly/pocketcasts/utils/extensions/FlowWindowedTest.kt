package au.com.shiftyjelly.pocketcasts.utils.extensions

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowWindowedTest {
    @Test
    fun `windowed requires positive size`() {
        try {
            flowOf(1).windowed(1)
        } catch (e: IllegalStateException) {
            assertEquals("Window size must be positive: 0", e.message)
        }
    }

    @Test
    fun `windowed retains history`() = runTest {
        val flow = MutableSharedFlow<String>()
        flow.windowed(3).test {
            expectNoEvents()

            flow.emit("a")
            expectNoEvents()

            flow.emit("b")
            expectNoEvents()

            flow.emit("c")
            assertEquals(listOf("a", "b", "c"), awaitItem())

            flow.emit("d")
            assertEquals(listOf("b", "c", "d"), awaitItem())
        }
    }
}
