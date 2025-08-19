package au.com.shiftyjelly.pocketcasts.repositories.transcript

import androidx.collection.LruCache
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptService
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.CacheControl
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript as DbTranscript

@Singleton
class TranscriptManagerImpl @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val transcriptService: TranscriptService,
    private val episodeManager: EpisodeManager,
    private val parsers: Map<TranscriptType, @JvmSuppressWildcards TranscriptParser>,
) : TranscriptManager {
    private val transcriptUrlBlacklist = ConcurrentHashMap<String, String>()
    private val lruCache = LruCache<String, Transcript>(maxSize = 20)

    override fun observeIsTranscriptAvailable(episodeUuid: String): Flow<Boolean> {
        return transcriptDao.observeTranscripts(episodeUuid).map { it.isNotEmpty() }
    }

    override suspend fun loadTranscript(
        episodeUuid: String,
    ): Transcript? {
        val transcript = lruCache[episodeUuid] ?: findAndCacheTranscript(episodeUuid)

        val message = if (transcript != null) {
            "Transcript loaded for episode $episodeUuid: ${transcript.type} ${transcript.url}"
        } else {
            "Couldn't load transcript for episode $episodeUuid"
        }
        Timber.tag("Transcripts").d(message)

        return transcript
    }

    private suspend fun findAndCacheTranscript(episodeUuid: String): Transcript? {
        val transcript = loadLocalTranscripts(episodeUuid)
            .asFlow()
            .mapNotNull(::associateWithParser)
            .mapNotNull(::readTranscript)
            .firstOrNull()
        if (transcript != null) {
            lruCache.put(episodeUuid, transcript)
        }
        return transcript
    }

    private suspend fun loadLocalTranscripts(episodeUuid: String): List<DbTranscript> {
        // When we request to load transcripts the URLs might still not be processed from the show notes.
        // Setting an arbitriarly large timeout allows us to wait for show notes to be processed.
        // In the worst case scenario transcripts won't be available for the first time if show notes haven't
        // been processed in time.
        val availableTranscripts = withTimeoutOrNull(1.minutes) {
            transcriptDao.observeTranscripts(episodeUuid)
                .filter { it.isNotEmpty() }
                .firstOrNull()
        }
        return availableTranscripts
            ?.filter { it.url !in transcriptUrlBlacklist[episodeUuid].orEmpty() }
            ?.sortedWith(TranscriptsComparator)
            .orEmpty()
    }

    private fun associateWithParser(transcript: DbTranscript): Pair<TranscriptParser, DbTranscript>? {
        val parser = TranscriptType.fromMimeType(transcript.type)?.let(parsers::get)
        if (parser == null) {
            transcriptUrlBlacklist.put(transcript.episodeUuid, transcript.url)
        }
        return parser?.let { it to transcript }
    }

    private suspend fun readTranscript(parserWithTranscript: Pair<TranscriptParser, DbTranscript>): Transcript? {
        val (parser, transcript) = parserWithTranscript
        val podcastUuid = episodeManager.findByUuid(transcript.episodeUuid)?.podcastUuid

        return runCatching { transcriptService.getTranscriptOrThrow(transcript.url) }
            .recoverCatching { transcriptService.getTranscriptOrThrow(transcript.url, CacheControl.FORCE_CACHE) }
            .mapCatching { body ->
                val transcriptEntries = withContext(Dispatchers.Default) {
                    val entries = body.use { parser.parse(it.source()) }.getOrThrow()
                    entries.sanitize()
                }
                Transcript.Text(
                    entries = transcriptEntries,
                    type = parser.type,
                    url = transcript.url,
                    isGenerated = transcript.isGenerated,
                    episodeUuid = transcript.episodeUuid,
                    podcastUuid = podcastUuid,
                )
            }
            .recoverCatching { error ->
                if (error is HtmlParser.ScriptDetectedException) {
                    Transcript.Web(
                        url = transcript.url,
                        isGenerated = transcript.isGenerated,
                        episodeUuid = transcript.episodeUuid,
                        podcastUuid = podcastUuid,
                    )
                } else {
                    throw error
                }
            }
            .onFailure { error ->
                if (error is CancellationException) {
                    throw error
                } else {
                    LogBuffer.e("Transcripts", error, "Failed to load transcript ${transcript.url} for episode ${transcript.episodeUuid}")
                    transcriptUrlBlacklist.put(transcript.episodeUuid, transcript.url)
                }
            }
            .getOrNull()
    }

    override fun resetInvalidTranscripts(
        episodeUuid: String,
    ) {
        transcriptUrlBlacklist.remove(episodeUuid)
    }
}

private val TranscriptsComparator = compareBy<DbTranscript>(
    { it.isGenerated },
    { TranscriptType.fromMimeType(it.type).priority },
)

private val TranscriptType?.priority
    get() = when (this) {
        TranscriptType.Vtt -> 0
        TranscriptType.Json -> 1
        TranscriptType.Srt -> 2
        TranscriptType.Html -> 3
        null -> Int.MAX_VALUE
    }
