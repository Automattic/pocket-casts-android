package au.com.shiftyjelly.pocketcasts.repositories.podcast

import androidx.annotation.VisibleForTesting
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.toTranscript
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptCacheServer
import au.com.shiftyjelly.pocketcasts.utils.NetworkWrapper
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Headers

class TranscriptsManagerImpl @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val service: TranscriptCacheServer,
    private val networkWrapper: NetworkWrapper,
    private val serverShowNotesManager: ServerShowNotesManager,
    @ApplicationScope private val scope: CoroutineScope,
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
            var response = if (forceRefresh && networkWrapper.isConnected()) {
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

    override suspend fun updateAlternativeTranscript(
        podcastUuid: String,
        episodeUuid: String,
        failedFormats: List<TranscriptFormat>,
        source: LoadTranscriptSource,
    ) {
        serverShowNotesManager.loadShowNotes(
            podcastUuid = podcastUuid,
            episodeUuid = episodeUuid,
        ) { showNotes ->
            val transcripts = showNotes.podcast?.episodes
                ?.firstOrNull { it.uuid == episodeUuid }
                ?.transcripts
                ?.mapNotNull { it.takeIf { it.url != null && it.type != null }?.toTranscript(episodeUuid) } ?: emptyList()
            val transcriptsAvailable = transcripts.filter { it.type !in failedFormats.map { it.mimeType } }
            scope.launch {
                updateTranscripts(episodeUuid, transcriptsAvailable, source)
            }
        }
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
