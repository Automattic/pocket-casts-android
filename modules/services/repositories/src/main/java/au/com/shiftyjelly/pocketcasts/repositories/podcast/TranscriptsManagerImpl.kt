package au.com.shiftyjelly.pocketcasts.repositories.podcast

import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.toTranscript
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptCacheServer
import au.com.shiftyjelly.pocketcasts.utils.NetworkWrapper
import au.com.shiftyjelly.pocketcasts.utils.exception.EmptyDataException
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.exception.ParsingException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Headers
import timber.log.Timber

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
    private val _failedTranscriptFormats = MutableStateFlow(emptyMap<String, List<TranscriptFormat>>())
    val failedTranscriptFormats = _failedTranscriptFormats.asStateFlow()

    override suspend fun updateTranscripts(
        podcastUuid: String,
        episodeUuid: String,
        transcripts: List<Transcript>,
        loadTranscriptSource: LoadTranscriptSource,
        fromUpdateAlternativeTranscript: Boolean,
    ) {
        if (transcripts.isEmpty()) return
        if (!fromUpdateAlternativeTranscript) {
            _failedTranscriptFormats.update { it.minus(episodeUuid) }
        }

        findBestTranscript(transcripts)?.let { bestTranscript ->
            transcriptDao.insert(bestTranscript)

            if (loadTranscriptSource == LoadTranscriptSource.DOWNLOAD_EPISODE) {
                loadTranscriptCuesInfo(
                    podcastUuid = podcastUuid,
                    transcript = bestTranscript,
                    source = loadTranscriptSource,
                    forceRefresh = true,
                )
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
        podcastUuid: String,
        transcript: Transcript,
        source: LoadTranscriptSource,
        forceRefresh: Boolean,
    ) = withContext(Dispatchers.IO) {
        val result = fetchTranscript(forceRefresh, transcript, source)
        try {
            transcriptCuesInfoBuilder.build(transcript, result)
        } catch (e: Exception) {
            when (e) {
                is EmptyDataException, is ParsingException -> {
                    updateFailedFormatsAndTryAlternative(
                        podcastUuid = podcastUuid,
                        transcript = transcript,
                        source = source,
                    )
                }

                else -> LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Failed to build transcript ${transcript.url}", e)
            }
            if (source == LoadTranscriptSource.DOWNLOAD_EPISODE) emptyList() else throw e // fail silently if built as part of episode download
        }
    }

    private suspend fun fetchTranscript(
        forceRefresh: Boolean,
        transcript: Transcript,
        source: LoadTranscriptSource,
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

    private suspend fun updateFailedFormatsAndTryAlternative(
        podcastUuid: String,
        transcript: Transcript,
        source: LoadTranscriptSource,
    ) {
        try {
            _failedTranscriptFormats.update { currentMap ->
                val format = TranscriptFormat.fromType(transcript.type) ?: return@update currentMap
                currentMap + (transcript.episodeUuid to (currentMap[transcript.episodeUuid]?.plus(format) ?: listOf(format)))
            }
            val episodeFailedFormats = _failedTranscriptFormats.value[transcript.episodeUuid]
            if (!episodeFailedFormats.isNullOrEmpty()) {
                // Get available transcripts from show notes
                serverShowNotesManager.loadShowNotes(
                    podcastUuid = podcastUuid,
                    episodeUuid = transcript.episodeUuid,
                ) { showNotes ->
                    val transcriptsAvailable = showNotes.podcast?.episodes
                        ?.firstOrNull { it.uuid == transcript.episodeUuid }
                        ?.transcripts
                        ?.mapNotNull { it.takeIf { it.url != null && it.type != null }?.toTranscript(transcript.episodeUuid) } ?: emptyList()
                    // Try alternative from filtered transcripts
                    val filteredTranscripts = transcriptsAvailable.filter { it.type !in episodeFailedFormats.map { format -> format.mimeType } }
                    scope.launch {
                        updateTranscripts(
                            podcastUuid = podcastUuid,
                            episodeUuid = transcript.episodeUuid,
                            transcripts = filteredTranscripts,
                            loadTranscriptSource = source,
                            fromUpdateAlternativeTranscript = true,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            val message = "Failed to update alternative transcript for episode ${transcript.episodeUuid}"
            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, message, e)
            Timber.e(e, message)
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
