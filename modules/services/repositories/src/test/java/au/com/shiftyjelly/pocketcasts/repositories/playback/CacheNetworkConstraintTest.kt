package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Test

class CacheNetworkConstraintTest {

    @Test
    fun `should require an unmetered network when the data warning is enabled`() {
        assertEquals(NetworkType.UNMETERED, cacheNetworkConstraint(warnOnMeteredNetwork = true))
    }

    @Test
    fun `should allow any connected network when the data warning is disabled`() {
        assertEquals(NetworkType.CONNECTED, cacheNetworkConstraint(warnOnMeteredNetwork = false))
    }
}
