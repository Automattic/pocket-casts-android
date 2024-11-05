package au.com.shiftyjelly.pocketcasts.utils

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LowStorageDetectorTest {

    @Test
    fun `test storage is low when available space is less than 10 percent`() = runBlocking {
        val totalStorage = 100L
        val availableStorage = 5L

        val result = LowStorageDetector().isRunningOnLowStorage(totalStorage, availableStorage)

        assertTrue(result)
    }

    @Test
    fun `test storage is not low when available space is more than 10 percent`() = runBlocking {
        val totalStorage = 100L
        val availableStorage = 20L

        val result = LowStorageDetector().isRunningOnLowStorage(totalStorage, availableStorage)

        assertFalse(result)
    }
}
