package au.com.shiftyjelly.pocketcasts.onboarding.signin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextCoverTest {

    private val pool = listOf("a", "b", "c")

    @Test
    fun `returns the first cover that is neither visible nor already revealed`() {
        assertEquals("a", nextCover(pool, visible = emptySet(), revealed = emptySet()))
        assertEquals("b", nextCover(pool, visible = setOf("a"), revealed = setOf("a")))
    }

    @Test
    fun `starts a new cycle when every off-screen cover has already been revealed`() {
        assertEquals("a", nextCover(pool, visible = setOf("b", "c"), revealed = setOf("a", "b", "c")))
    }

    @Test
    fun `returns null when every cover is on screen`() {
        assertNull(nextCover(pool, visible = pool.toSet(), revealed = emptySet()))
    }
}
