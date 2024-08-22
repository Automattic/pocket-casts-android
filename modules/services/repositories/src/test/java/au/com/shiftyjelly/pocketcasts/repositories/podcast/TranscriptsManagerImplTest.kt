package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptCacheServer
import au.com.shiftyjelly.pocketcasts.utils.NetworkWrapper
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import kotlinx.coroutines.test.runTest
import okhttp3.CacheControl
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import retrofit2.Response

class TranscriptsManagerImplTest {
    private val transcriptDao: TranscriptDao = mock()
    private val transcriptCacheServer: TranscriptCacheServer = mock()
    private val networkWrapper: NetworkWrapper = mock()
    private val transcriptsManager = TranscriptsManagerImpl(
        transcriptDao = transcriptDao,
        service = transcriptCacheServer,
        networkWrapper = networkWrapper,
        serverShowNotesManager = mock(),
        scope = mock(),
    )

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
    fun `if force refresh is true, and internet available, loadTranscript loads transcript from network `() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(true)
        val response = mock<Response<ResponseBody>>()
        whenever(response.isSuccessful).thenReturn(true)
        whenever(transcriptCacheServer.getTranscript(any(), any())).thenReturn(response)

        transcriptsManager.loadTranscript("url_1", forceRefresh = true)

        verify(transcriptCacheServer).getTranscript("url_1", CacheControl.FORCE_NETWORK)
    }

    @Test
    fun `if force refresh is true, and internet not available, loadTranscript loads transcript from cache `() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(false)
        val response = mock<Response<ResponseBody>>()
        whenever(response.isSuccessful).thenReturn(true)
        whenever(transcriptCacheServer.getTranscript(any(), any())).thenReturn(response)

        transcriptsManager.loadTranscript("url_1", forceRefresh = true)

        verify(transcriptCacheServer).getTranscript(eq("url_1"), argWhere { it.onlyIfCached })
    }

    @Test
    fun `if force refresh is false, loadTranscript loads transcript from cache`() = runTest {
        val response = mock<Response<ResponseBody>>()
        whenever(response.isSuccessful).thenReturn(true)
        whenever(transcriptCacheServer.getTranscript(any(), any())).thenReturn(response)

        transcriptsManager.loadTranscript("url_1")

        verify(transcriptCacheServer).getTranscript(eq("url_1"), argWhere { it.onlyIfCached })
    }

    @Test
    fun `if cache response not found, and internet available, loadTranscript loads transcript from network`() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(true)
        val response = mock<Response<ResponseBody>>()
        whenever(response.isSuccessful).thenReturn(false)
        whenever(transcriptCacheServer.getTranscript(any(), any())).thenReturn(response)

        transcriptsManager.loadTranscript("url_1")

        verify(transcriptCacheServer).getTranscript("url_1", CacheControl.FORCE_NETWORK)
    }

    @Test
    fun `if cache response not found, and internet not available, loadTranscript returns no network exception`() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(false)
        val response = mock<Response<ResponseBody>>()
        whenever(response.isSuccessful).thenReturn(false)
        whenever(transcriptCacheServer.getTranscript(any(), any())).thenReturn(response)

        try {
            transcriptsManager.loadTranscript("url_1")
        } catch (e: Exception) {
            assertTrue(e is NoNetworkException)
        }
    }

    @Test
    fun `updateTranscripts loads transcript if the source is download episode`() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(true)
        val transcripts = listOf(
            Transcript("1", "url_1", "application/srt"),
        )

        transcriptsManager.updateTranscripts("1", transcripts, LoadTranscriptSource.DOWNLOAD_EPISODE)

        verify(transcriptCacheServer).getTranscript("url_1", CacheControl.FORCE_NETWORK)
    }

    @Test
    fun `updateTranscripts does not load transcript if the source is not download episode`() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(true)
        val transcripts = listOf(
            Transcript("1", "url_1", "application/srt"),
        )

        transcriptsManager.updateTranscripts("1", transcripts, LoadTranscriptSource.DEFAULT)

        verifyNoInteractions(transcriptCacheServer)
    }
}
