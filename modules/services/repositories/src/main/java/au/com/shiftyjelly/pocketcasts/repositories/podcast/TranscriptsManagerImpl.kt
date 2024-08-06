package au.com.shiftyjelly.pocketcasts.repositories.podcast

import androidx.annotation.VisibleForTesting
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServer
import au.com.shiftyjelly.pocketcasts.utils.NetworkWrapper
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Headers

class TranscriptsManagerImpl @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val service: PodcastCacheServer,
    private val networkWrapper: NetworkWrapper,
) : TranscriptsManager {
    private val supportedFormats = listOf(TranscriptFormat.SRT, TranscriptFormat.VTT, TranscriptFormat.HTML)

    override suspend fun updateTranscripts(
        episodeUuid: String,
        transcripts: List<Transcript>,
        loadTranscriptSource: LoadTranscriptSource,
    ) {
        if (transcripts.isEmpty()) return
        findBestTranscript(transcripts)?.let { bestTranscript ->
            transcriptDao.insert(bestTranscript)
            if (loadTranscriptSource == LoadTranscriptSource.DOWNLOAD_EPISODE) {
                loadTranscript(bestTranscript.url, loadTranscriptSource, forceRefresh = true)
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

    override suspend fun loadTranscript(
        url: String,
        source: LoadTranscriptSource,
        forceRefresh: Boolean,
    ) = withContext(Dispatchers.IO) {
        try {
            var response = if (forceRefresh) {
                service.getTranscript(url, CacheControl.FORCE_NETWORK)
            } else {
                service.getTranscript(url, CacheControl.parse(Headers.headersOf("Cache-Control", "only-if-cached, max-stale=7776000")))
            }
            if (response.isSuccessful) {
                response.body()
            } else {
                if (!networkWrapper.isConnected()) {
                    throw NoNetworkException()
                } else {
                    response = service.getTranscript(url, CacheControl.FORCE_NETWORK)
                    if (response.isSuccessful) {
                        response.body()
                    } else {
                        LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to load transcript from $url: ${response.errorBody()}")
                        response.errorBody()
                    }
                }
            }
        } catch (e: UnknownHostException) {
            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to load transcript from $url", e)
            if (source == LoadTranscriptSource.DOWNLOAD_EPISODE) null else throw NoNetworkException() // fail silently if loaded as part of episode download
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to load transcript from $url", e)
            if (source == LoadTranscriptSource.DOWNLOAD_EPISODE) null else throw e // fail silently if loaded as part of episode download
        }
    }
}

enum class TranscriptFormat(val mimeType: String) {
    SRT("application/srt"),
    VTT("text/vtt"),
    HTML("text/html"),
}

enum class LoadTranscriptSource {
    DOWNLOAD_EPISODE, // When transcript is downloaded as part of episode download
    DEFAULT,
}
