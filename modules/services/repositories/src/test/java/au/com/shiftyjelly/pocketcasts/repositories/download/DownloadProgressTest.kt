package au.com.shiftyjelly.pocketcasts.repositories.download

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class DownloadProgressTest {
    @Test
    fun `calculate progress`() {
        val value = DownloadProgress(
            downloadedByteCount = 372,
            contentLength = 1000,
        )

        assertEquals(37, value.percentage)
    }

    @Test
    fun `limit max progress at 100`() {
        val value = DownloadProgress(
            downloadedByteCount = 1000,
            contentLength = 100,
        )

        assertEquals(100, value.percentage)
    }

    @Test
    fun `do not compute progress for negative bytes count`() {
        val value = DownloadProgress(
            downloadedByteCount = -100,
            contentLength = 100,
        )

        assertNull(value.percentage)
    }

    @Test
    fun `do not compute progress for negative content length`() {
        val value = DownloadProgress(
            downloadedByteCount = 100,
            contentLength = -100,
        )

        assertNull(value.percentage)
    }

    @Test
    fun `do not compute progress for 0 content length`() {
        val value = DownloadProgress(
            downloadedByteCount = 100,
            contentLength = 0,
        )

        assertNull(value.percentage)
    }

    @Test
    fun `do not compute progress for null content length`() {
        val value = DownloadProgress(
            downloadedByteCount = 100,
            contentLength = null,
        )

        assertNull(value.percentage)
    }
}
