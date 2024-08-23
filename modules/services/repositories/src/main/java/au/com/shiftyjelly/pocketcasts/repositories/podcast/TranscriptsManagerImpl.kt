package au.com.shiftyjelly.pocketcasts.repositories.podcast

import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptCacheServer
import au.com.shiftyjelly.pocketcasts.utils.NetworkWrapper
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Headers

@OptIn(UnstableApi::class)
class TranscriptsManagerImpl @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val service: TranscriptCacheServer,
    private val networkWrapper: NetworkWrapper,
    private val serverShowNotesManager: ServerShowNotesManager,
    @ApplicationScope private val scope: CoroutineScope,
    private val transcriptCuesInfoBuilder: TranscriptCuesInfoBuilder,
) : TranscriptsManager {
    private val supportedFormats = listOf(TranscriptFormat.SRT, TranscriptFormat.VTT, TranscriptFormat.JSON_PODCAST_INDEX, TranscriptFormat.HTML)

    override suspend fun updateTranscripts(
        episodeUuid: String,
        transcripts: List<Transcript>,
        loadTranscriptSource: LoadTranscriptSource,
    ) {
        if (transcripts.isEmpty()) return
        findBestTranscript(transcripts)?.let { bestTranscript ->
            transcriptDao.insert(bestTranscript)
            if (loadTranscriptSource == LoadTranscriptSource.DOWNLOAD_EPISODE) {
                loadTranscriptCuesInfo(bestTranscript, loadTranscriptSource, forceRefresh = true)
            }
        }
    }

    override fun observerTranscriptForEpisode(episodeUuid: String) =
        transcriptDao.observerTranscriptForEpisode(episodeUuid)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun findBestTranscript(availableTranscripts: List<Transcript>): Transcript? {
        for (format in supportedFormats) {
            val transcript = availableTranscripts.firstOrNull { it.type == format.mimeType }
            if (transcript != null) {
                return transcript
            }
        }
        return availableTranscripts.firstOrNull()
    }

    @OptIn(UnstableApi::class)
    override suspend fun loadTranscriptCuesInfo(
        transcript: Transcript,
        source: LoadTranscriptSource,
        forceRefresh: Boolean,
    ) = withContext(Dispatchers.IO) {
        val result = fetchTranscript(forceRefresh, transcript, source)
        transcriptCuesInfoBuilder.build(transcript, result)
    }

    private suspend fun fetchTranscript(
        forceRefresh: Boolean,
        transcript: Transcript,
        source: LoadTranscriptSource
    ) = try {
        var response = if (forceRefresh && networkWrapper.isConnected()) {
            service.getTranscript(transcript.url, CacheControl.FORCE_NETWORK)
        } else {
            service.getTranscript(transcript.url, CacheControl.parse(Headers.headersOf("Cache-Control", "only-if-cached, max-stale=7776000")))
        }
        if (response.isSuccessful) {
            response.body()
        } else {
            if (!networkWrapper.isConnected()) {
                throw NoNetworkException()
            } else {
                response = service.getTranscript(transcript.url, CacheControl.FORCE_NETWORK)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to load transcript from ${transcript.url}: ${response.errorBody()}")
                    response.errorBody()
                }
            }
        }
    } catch (e: UnknownHostException) {
        LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to load transcript from $${transcript.url}", e)
        if (source == LoadTranscriptSource.DOWNLOAD_EPISODE) null else throw NoNetworkException() // fail silently if loaded as part of episode download
    } catch (e: Exception) {
        LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to load transcript from $${transcript.url}", e)
        if (source == LoadTranscriptSource.DOWNLOAD_EPISODE) null else throw e // fail silently if loaded as part of episode download
    }
}

enum class TranscriptFormat(val mimeType: String) {
    SRT("application/srt"),
    VTT("text/vtt"),
    JSON_PODCAST_INDEX("application/json"),
    HTML("text/html"),
    ;

    companion object {
        fun fromType(type: String) =
            entries.firstOrNull { it.mimeType == type }
    }
}
