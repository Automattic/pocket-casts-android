package au.com.shiftyjelly.pocketcasts.repositories.podcast

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptCuesInfo
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesEpisode
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesPodcast
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesResponse
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesTranscript
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptCacheServer
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.NetworkWrapper
import au.com.shiftyjelly.pocketcasts.utils.exception.EmptyDataException
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.exception.ParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import okhttp3.CacheControl
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptsManagerImplTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()
    private val transcriptDao: TranscriptDao = mock()
    private val transcriptCacheServer: TranscriptCacheServer = mock()
    private val networkWrapper: NetworkWrapper = mock()
    private val transcriptCuesInfoBuilder: TranscriptCuesInfoBuilder = mock()
    private val serverShowNotesManager: ServerShowNotesManager = mock()
    private val podcastId = "podcast_id"
    private val transcript = Transcript("1", "url_1", "application/srt")
    private val alternateTranscript = Transcript("1", "url_2", "application/json")

    private val transcriptsManager = TranscriptsManagerImpl(
        transcriptDao = transcriptDao,
        service = transcriptCacheServer,
        networkWrapper = networkWrapper,
        serverShowNotesManager = serverShowNotesManager,
        scope = CoroutineScope(SupervisorJob() + coroutineRule.testDispatcher),
        transcriptCuesInfoBuilder = transcriptCuesInfoBuilder,
    )

    @Before
    fun setUp() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(true)

        val response = mock<Response<ResponseBody>>()
        whenever(response.isSuccessful).thenReturn(true)
        whenever(response.body()).thenReturn(mock())
        whenever(transcriptCacheServer.getTranscript(any(), any())).thenReturn(response)

        val showNotesResponse = mock<ShowNotesResponse>()
        val showNotesTranscript1 = mock<ShowNotesTranscript>().apply {
            whenever(this.url).thenReturn(transcript.url)
            whenever(this.type).thenReturn(transcript.type)
        }
        val showNotesTranscript2 = mock<ShowNotesTranscript>().apply {
            whenever(this.url).thenReturn(alternateTranscript.url)
            whenever(this.type).thenReturn(alternateTranscript.type)
        }
        val showNotesEpisode = mock<ShowNotesEpisode>().apply {
            whenever(this.transcripts).thenReturn(listOf(showNotesTranscript1, showNotesTranscript2))
            whenever(this.uuid).thenReturn(transcript.episodeUuid)
        }
        val showNotesPodcast = mock<ShowNotesPodcast>().apply {
            whenever(this.episodes).thenReturn(listOf(showNotesEpisode))
        }
        whenever(showNotesResponse.podcast).thenReturn(showNotesPodcast)
        whenever(serverShowNotesManager.loadShowNotes(any(), any(), any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<(ShowNotesResponse) -> Unit>(2)
            callback(showNotesResponse)
        }
    }

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

        transcriptsManager.updateTranscripts(podcastId, "1", transcripts, LoadTranscriptSource.DEFAULT)

        verify(transcriptDao).insert(transcripts[0])
    }

    @Test
    fun `if force refresh is true, and internet available, loadTranscript loads transcript from network `() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(true)
        val response = mock<Response<ResponseBody>>()
        whenever(response.isSuccessful).thenReturn(true)
        whenever(transcriptCacheServer.getTranscript(any(), any())).thenReturn(response)

        transcriptsManager.loadTranscriptCuesInfo(podcastId, transcript, forceRefresh = true)

        verify(transcriptCacheServer).getTranscript("url_1", CacheControl.FORCE_NETWORK)
    }

    @Test
    fun `if force refresh is true, and internet not available, loadTranscript loads transcript from cache `() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(false)
        val response = mock<Response<ResponseBody>>()
        whenever(response.isSuccessful).thenReturn(true)
        whenever(transcriptCacheServer.getTranscript(any(), any())).thenReturn(response)

        transcriptsManager.loadTranscriptCuesInfo(podcastId, transcript, forceRefresh = true)

        verify(transcriptCacheServer).getTranscript(eq("url_1"), argWhere { it.onlyIfCached })
    }

    @Test
    fun `if force refresh is false, loadTranscript loads transcript from cache`() = runTest {
        val response = mock<Response<ResponseBody>>()
        whenever(response.isSuccessful).thenReturn(true)
        whenever(transcriptCacheServer.getTranscript(any(), any())).thenReturn(response)

        transcriptsManager.loadTranscriptCuesInfo(podcastId, transcript)

        verify(transcriptCacheServer).getTranscript(eq("url_1"), argWhere { it.onlyIfCached })
    }

    @Test
    fun `if cache response not found, and internet available, loadTranscript loads transcript from network`() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(true)
        val response = mock<Response<ResponseBody>>()
        whenever(response.isSuccessful).thenReturn(false)
        whenever(transcriptCacheServer.getTranscript(any(), any())).thenReturn(response)

        transcriptsManager.loadTranscriptCuesInfo(podcastId, transcript)

        verify(transcriptCacheServer).getTranscript("url_1", CacheControl.FORCE_NETWORK)
    }

    @Test
    fun `if cache response not found, and internet not available, loadTranscript returns no network exception`() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(false)
        val response = mock<Response<ResponseBody>>()
        whenever(response.isSuccessful).thenReturn(false)
        whenever(transcriptCacheServer.getTranscript(any(), any())).thenReturn(response)

        try {
            transcriptsManager.loadTranscriptCuesInfo(podcastId, transcript)
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

        transcriptsManager.updateTranscripts(podcastId, "1", transcripts, LoadTranscriptSource.DOWNLOAD_EPISODE)

        verify(transcriptCacheServer).getTranscript("url_1", CacheControl.FORCE_NETWORK)
    }

    @Test
    fun `updateTranscripts does not load transcript if the source is not download episode`() = runTest {
        whenever(networkWrapper.isConnected()).thenReturn(true)
        val transcripts = listOf(
            Transcript("1", "url_1", "application/srt"),
        )

        transcriptsManager.updateTranscripts(podcastId, "1", transcripts, LoadTranscriptSource.DEFAULT)

        verifyNoInteractions(transcriptCacheServer)
    }

    @Test
    fun `loadTranscriptCuesInfo returns cues info when successful`() = runTest {
        val cues = listOf(mock<TranscriptCuesInfo>())
        whenever(transcriptCuesInfoBuilder.build(any(), any())).thenReturn(cues)

        val result = transcriptsManager.loadTranscriptCuesInfo(podcastId, transcript, LoadTranscriptSource.DEFAULT)

        assertEquals(cues, result)
    }

    @Test
    fun `loadTranscriptCuesInfo tries alternative on EmptyDataException on default source`() = runTest {
        given(transcriptCuesInfoBuilder.build(any(), anyOrNull())).willAnswer { throw EmptyDataException("") }

        try {
            transcriptsManager.loadTranscriptCuesInfo(podcastId, transcript, LoadTranscriptSource.DEFAULT)
        } catch (e: Exception) {
            assertTrue(e is EmptyDataException)
        }

        transcriptsManager.failedTranscriptFormats.test {
            awaitItem()
            verify(transcriptDao).insert(alternateTranscript)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadTranscriptCuesInfo tries alternative on ParsingException on default source`() = runTest {
        given(transcriptCuesInfoBuilder.build(any(), anyOrNull())).willAnswer { throw ParsingException("") }

        try {
            transcriptsManager.loadTranscriptCuesInfo(podcastId, transcript, LoadTranscriptSource.DEFAULT)
        } catch (e: Exception) {
            assertTrue(e is ParsingException)
        }

        transcriptsManager.failedTranscriptFormats.test {
            awaitItem()
            verify(transcriptDao).insert(alternateTranscript)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadTranscriptCuesInfo tries alternative on EmptyDataException on download source`() = runTest {
        given(transcriptCuesInfoBuilder.build(any(), anyOrNull())).willAnswer { throw EmptyDataException("") }

        transcriptsManager.loadTranscriptCuesInfo(podcastId, transcript, LoadTranscriptSource.DOWNLOAD_EPISODE)

        transcriptsManager.failedTranscriptFormats.test {
            awaitItem()
            verify(transcriptDao).insert(alternateTranscript)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `loadTranscriptCuesInfo tries alternative on ParsingException on download source`() = runTest {
        given(transcriptCuesInfoBuilder.build(any(), anyOrNull())).willAnswer { throw ParsingException("") }

        transcriptsManager.loadTranscriptCuesInfo(podcastId, transcript, LoadTranscriptSource.DOWNLOAD_EPISODE)

        transcriptsManager.failedTranscriptFormats.test {
            awaitItem()
            verify(transcriptDao).insert(alternateTranscript)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
