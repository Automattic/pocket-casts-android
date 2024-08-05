package au.com.shiftyjelly.pocketcasts.repositories.podcast

import androidx.annotation.VisibleForTesting
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServer
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranscriptsManagerImpl @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val service: PodcastCacheServer,
) : TranscriptsManager {
    private val supportedFormats = listOf(TranscriptFormat.SRT, TranscriptFormat.VTT, TranscriptFormat.HTML)

    override suspend fun updateTranscripts(
        episodeUuid: String,
        transcripts: List<Transcript>,
    ) {
        if (transcripts.isEmpty()) return
        findBestTranscript(transcripts)?.let { bestTranscript ->
            transcriptDao.insert(bestTranscript)
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

    override suspend fun loadTranscript(url: String, source: LoadTranscriptSource) = withContext(Dispatchers.IO) {
        try {
            val response = service.getTranscript(url)
            if (response.isSuccessful) {
                response.body()
            } else {
                response.errorBody()
            }
        } catch (e: UnknownHostException) {
            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to load transcript from $url", e)
            throw NoNetworkException()
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to load transcript from $url", e)
            throw e
        }
    }
}

enum class TranscriptFormat(val mimeType: String) {
    SRT("application/srt"),
    VTT("text/vtt"),
    HTML("text/html"),
}

enum class LoadTranscriptSource {
    DEFAULT,
}
