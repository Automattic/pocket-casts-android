package au.com.shiftyjelly.pocketcasts.repositories.podcast

import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.findTranscripts
import au.com.shiftyjelly.pocketcasts.servers.ShowNotesServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptCacheService
import au.com.shiftyjelly.pocketcasts.utils.NetworkWrapper
import au.com.shiftyjelly.pocketcasts.utils.exception.EmptyDataException
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.exception.ParsingException
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Headers
import timber.log.Timber

@OptIn(UnstableApi::class)
class TranscriptsManagerImpl @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val service: TranscriptCacheService,
    private val networkWrapper: NetworkWrapper,
    private val showNotesServiceManager: ShowNotesServiceManager,
    @ApplicationScope private val scope: CoroutineScope,
    private val transcriptCuesInfoBuilder: TranscriptCuesInfoBuilder,
) : TranscriptsManager {
    private val supportedFormats = listOf(TranscriptFormat.SRT, TranscriptFormat.VTT, TranscriptFormat.JSON_PODCAST_INDEX, TranscriptFormat.HTML)
    private val invalidTranscriptUrls = ConcurrentHashMap<String, Set<String>>()

    override suspend fun updateTranscripts(
        podcastUuid: String,
        episodeUuid: String,
        transcripts: List<Transcript>,
        loadTranscriptSource: LoadTranscriptSource,
        fromUpdateAlternativeTranscript: Boolean,
    ) {
        if (transcripts.isEmpty()) return
        if (!fromUpdateAlternativeTranscript) {
            invalidTranscriptUrls.remove(episodeUuid)
        }

        findBestTranscript(transcripts)?.let { bestTranscript ->
            transcriptDao.insertBlocking(bestTranscript)

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

    override fun observeTranscriptForEpisode(episodeUuid: String) = transcriptDao
        .observeTranscriptForEpisode(episodeUuid)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun findBestTranscript(transcripts: List<Transcript>): Transcript? {
        val availableTranscripts = transcripts.sortedBy { it.isGenerated }
        for (format in supportedFormats) {
            val transcript = availableTranscripts.firstOrNull { it.type in format.possibleMimeTypes() }
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
            val invalidUrls = invalidTranscriptUrls.merge(transcript.episodeUuid, setOf(transcript.url)) { old, new -> old + new }

            if (!invalidUrls.isNullOrEmpty()) {
                // Get available transcripts from show notes
                showNotesServiceManager.loadShowNotes(
                    podcastUuid = podcastUuid,
                    episodeUuid = transcript.episodeUuid,
                ) { showNotes ->
                    val availableTranscripts = showNotes
                        .findTranscripts(transcript.episodeUuid)
                        ?.filter { it.url !in invalidUrls }
                        .orEmpty()
                    scope.launch {
                        updateTranscripts(
                            podcastUuid = podcastUuid,
                            episodeUuid = transcript.episodeUuid,
                            transcripts = availableTranscripts,
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

    fun possibleMimeTypes() = when (this) {
        SRT -> listOf(mimeType, "application/x-subrip")
        else -> listOf(mimeType)
    }

    companion object {
        fun fromType(type: String) =
            entries.firstOrNull { it.possibleMimeTypes().contains(type) }
    }
}
