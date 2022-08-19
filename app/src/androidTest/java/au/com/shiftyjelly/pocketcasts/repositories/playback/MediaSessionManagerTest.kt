package au.com.shiftyjelly.pocketcasts.repositories.playback

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MediaSessionManagerTest {
    @Test
    fun testCalculateSearchQueryAttempts() {
        var options = MediaSessionManager.calculateSearchQueryOptions("Daily tech news in")
        assertNotNull(options)
        assertEquals(4, options.size.toLong())
        assertArrayEquals(arrayOf("Daily tech news in", "Daily tech news", "Daily tech", "Daily"), options.toTypedArray())

        options = MediaSessionManager.calculateSearchQueryOptions("Material")
        assertNotNull(options)
        assertEquals(1, options.size.toLong())
        assertArrayEquals(arrayOf("Material"), options.toTypedArray())
    }
}
