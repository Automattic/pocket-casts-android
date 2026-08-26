package au.com.shiftyjelly.pocketcasts.onboarding.signin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextDistinctCoverIndexTest {

    private val pool = listOf("a", "b", "c", "d")

    @Test
    fun `returns the starting index when nothing is mounted`() {
        assertEquals(0, nextDistinctCoverIndex(pool, from = 0, mounted = emptySet()))
        assertEquals(2, nextDistinctCoverIndex(pool, from = 2, mounted = emptySet()))
    }

    @Test
    fun `skips indexes whose cover is already mounted`() {
        assertEquals(1, nextDistinctCoverIndex(pool, from = 0, mounted = setOf("a")))
        assertEquals(2, nextDistinctCoverIndex(pool, from = 1, mounted = setOf("b")))
    }

    @Test
    fun `wraps around to find a distinct cover`() {
        assertEquals(1, nextDistinctCoverIndex(pool, from = 3, mounted = setOf("d", "a")))
        assertEquals(0, nextDistinctCoverIndex(pool, from = 3, mounted = setOf("d")))
    }

    @Test
    fun `returns null when every cover is mounted`() {
        assertNull(nextDistinctCoverIndex(pool, from = 1, mounted = pool.toSet()))
    }
}
