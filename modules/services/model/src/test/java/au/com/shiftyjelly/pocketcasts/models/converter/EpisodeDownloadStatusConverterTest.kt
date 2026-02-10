package au.com.shiftyjelly.pocketcasts.models.converter

import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeDownloadStatusConverterTest {
    val converter = EpisodeDownloadStatusConverter()

    @Test
    fun `each status is mapped to a different value`() {
        val entries = EpisodeDownloadStatus.entries
        val values = entries.map(converter::toInt).distinct()

        assertEquals(entries.size, values.size)
    }

    @Test
    fun `null value`() {
        assertEquals(0, converter.toInt(null))
        assertEquals(EpisodeDownloadStatus.NotDownloaded, converter.fromInt(null))
    }

    @Test
    fun `legacy ordinal values`() {
        assertEquals(0, converter.toInt(EpisodeDownloadStatus.NotDownloaded))
        assertEquals(EpisodeDownloadStatus.NotDownloaded, converter.fromInt(0))

        assertEquals(1, converter.toInt(EpisodeDownloadStatus.Queued))
        assertEquals(EpisodeDownloadStatus.Queued, converter.fromInt(1))

        assertEquals(2, converter.toInt(EpisodeDownloadStatus.Downloading))
        assertEquals(EpisodeDownloadStatus.Downloading, converter.fromInt(2))

        assertEquals(3, converter.toInt(EpisodeDownloadStatus.DownloadFailed))
        assertEquals(EpisodeDownloadStatus.DownloadFailed, converter.fromInt(3))

        assertEquals(4, converter.toInt(EpisodeDownloadStatus.Downloaded))
        assertEquals(EpisodeDownloadStatus.Downloaded, converter.fromInt(4))

        assertEquals(5, converter.toInt(EpisodeDownloadStatus.WaitingForWifi))
        assertEquals(EpisodeDownloadStatus.WaitingForWifi, converter.fromInt(5))

        assertEquals(6, converter.toInt(EpisodeDownloadStatus.WaitingForPower))
        assertEquals(EpisodeDownloadStatus.WaitingForPower, converter.fromInt(6))
    }
}
