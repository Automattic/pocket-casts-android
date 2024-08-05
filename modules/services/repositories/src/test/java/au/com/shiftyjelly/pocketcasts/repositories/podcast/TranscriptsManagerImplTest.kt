package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class TranscriptsManagerImplTest {
    private val transcriptDao: TranscriptDao = mock()
    private val podcastCacheServer: PodcastCacheServer = mock()
    private val transcriptsManager = TranscriptsManagerImpl(transcriptDao, podcastCacheServer)

    @Test
    fun `findBestTranscript returns first supported transcript`() = runTest {
        val transcripts = listOf(
            Transcript("1", "url_0", "un-supported"),
            Transcript("1", "url_1", "application/srt"),
            Transcript("1", "url_2", "text/vtt"),
        )
        val result = transcriptsManager.findBestTranscript(transcripts)

        assertEquals(transcripts[1], result)
    }

    @Test
    fun `findBestTranscript returns null when no transcripts available`() = runTest {
        val transcripts = emptyList<Transcript>()

        val result = transcriptsManager.findBestTranscript(transcripts)

        assertNull(result)
    }

    @Test
    fun `findBestTranscript returns first un-supported transcript when no other transcript is supported`() = runTest {
        val transcripts = listOf(
            Transcript("1", "url_1", "un-supported"),
            Transcript("1", "url_2", "un-supported"),
        )

        val result = transcriptsManager.findBestTranscript(transcripts)

        assertEquals(transcripts[0], result)
    }

    @Test
    fun `updateTranscripts inserts best transcript when available`() = runTest {
        val transcripts = listOf(
            Transcript("1", "url_1", "application/srt"),
            Transcript("1", "url_2", "text/vtt"),
        )

        transcriptsManager.updateTranscripts("1", transcripts, LoadTranscriptSource.DEFAULT)

        verify(transcriptDao).insert(transcripts[0])
    }

    @Test
    fun `updateTranscripts loads transcript if the source is download episode`() = runTest {
        val transcripts = listOf(
            Transcript("1", "url_1", "application/srt"),
        )

        transcriptsManager.updateTranscripts("1", transcripts, LoadTranscriptSource.DOWNLOAD_EPISODE)

        verify(podcastCacheServer).getTranscript("url_1")
    }

    @Test
    fun `updateTranscripts does not load transcript if the source is not download episode`() = runTest {
        val transcripts = listOf(
            Transcript("1", "url_1", "application/srt"),
        )

        transcriptsManager.updateTranscripts("1", transcripts, LoadTranscriptSource.DEFAULT)

        verifyNoInteractions(podcastCacheServer)
    }
}
