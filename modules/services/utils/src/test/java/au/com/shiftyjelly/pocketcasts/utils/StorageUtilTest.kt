package au.com.shiftyjelly.pocketcasts.utils

import android.os.StatFs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class StorageUtilTest {

    private lateinit var statFs: StatFs

    @Before
    fun setUp() {
        statFs = mock(StatFs::class.java)
    }

    @Test
    fun `test storage is low when available space is less than 10 percent`() {
        whenever(statFs.blockCountLong).thenReturn(100L)
        whenever(statFs.blockSizeLong).thenReturn(1024L)
        whenever(statFs.availableBlocksLong).thenReturn(5L) // 5% available

        val result = isDeviceLowOnFreeSpace(statFs)

        assertTrue(result)
    }

    @Test
    fun `test storage is not low when available space is more than 10 percent`() {
        whenever(statFs.blockCountLong).thenReturn(100L)
        whenever(statFs.blockSizeLong).thenReturn(1024L)
        whenever(statFs.availableBlocksLong).thenReturn(20L) // 20% available

        val result = isDeviceLowOnFreeSpace(statFs)

        assertFalse(result)
    }

    private fun isDeviceLowOnFreeSpace(statFs: StatFs): Boolean {
        val totalStorage = statFs.blockCountLong * statFs.blockSizeLong
        val availableStorage = statFs.availableBlocksLong * statFs.blockSizeLong

        return availableStorage < totalStorage * 0.10
    }
}
