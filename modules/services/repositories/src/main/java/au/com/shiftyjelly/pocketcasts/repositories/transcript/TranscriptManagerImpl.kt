package au.com.shiftyjelly.pocketcasts.repositories.transcript

import androidx.collection.LruCache
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
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
        val cachedTranscript = lruCache[episodeUuid]
        if (cachedTranscript != null) {
            return cachedTranscript
        }

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

        return runCatching { transcriptService.getTranscriptOrThrow(transcript.url, CacheControl.FORCE_NETWORK) }
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

private fun List<TranscriptEntry>.sanitize(): List<TranscriptEntry> {
    return map(TranscriptEntry::sanitize)
        .removeTheSameConsecutiveSpeakers()
        .joinSplitSentneces()
        .map(TranscriptEntry::trim)
        .filter(TranscriptEntry::isNotEmpty)
}

private fun TranscriptEntry.sanitize() = when (this) {
    is TranscriptEntry.Speaker -> {
        val newName = name.replace(AnyWhiteSpace, " ")
        copy(name = newName)
    }
    is TranscriptEntry.Text -> {
        val newValue = value
            .replace(TwoOrMoreEmptySpaces, " ")
            .replace(ThreeOrMoreNewLines, "\n\n")
        copy(value = newValue)
    }
}

private val TwoOrMoreEmptySpaces = """[ \t]+""".toRegex()
private val ThreeOrMoreNewLines = """\n{3,}""".toRegex()
private val AnyWhiteSpace = """\s+""".toRegex()

private fun List<TranscriptEntry>.removeTheSameConsecutiveSpeakers(): List<TranscriptEntry> {
    var currentSpeaker: String? = null
    return mapNotNull { entry ->
        when (entry) {
            is TranscriptEntry.Speaker -> {
                if (entry.name != currentSpeaker) {
                    currentSpeaker = entry.name
                    entry
                } else {
                    null
                }
            }
            is TranscriptEntry.Text -> entry
        }
    }
}

private fun List<TranscriptEntry>.joinSplitSentneces(): List<TranscriptEntry> {
    var carryOverText: String? = null
    val newEntries = ArrayList<TranscriptEntry>(size)
    mapNotNullTo(newEntries) { entry ->
        when (entry) {
            is TranscriptEntry.Speaker -> entry
            is TranscriptEntry.Text -> {
                val prependedText = if (carryOverText != null) {
                    "$carryOverText ${entry.value}"
                } else {
                    entry.value
                }

                val textParts = prependedText.split(SentenceStoppers)
                carryOverText = textParts.lastOrNull()?.takeUnless(String::isSentence)?.trim()

                val fullSentences = if (carryOverText != null) {
                    textParts.dropLast(1)
                } else {
                    textParts
                }.joinToString(separator = "").takeIf(String::isNotEmpty)
                fullSentences?.let { entry.copy(value = fullSentences) }
            }
        }
    }
    carryOverText?.let { text ->
        newEntries += TranscriptEntry.Text(text)
    }
    return newEntries
}

private fun String.split(delimiters: List<Char>): List<String> {
    val texts = mutableListOf<String>()
    var builder = StringBuilder()
    for (char in this) {
        builder.append(char)
        if (char in delimiters) {
            texts += builder.toString()
            builder = StringBuilder()
        }
    }
    if (builder.isNotEmpty()) {
        texts += builder.toString()
    }
    return texts
}

private fun String.isSentence() = lastOrNull() in SentenceStoppers

private val SentenceStoppers = listOf('.', '!', '?')

private fun TranscriptEntry.trim() = when (this) {
    is TranscriptEntry.Speaker -> copy(name = name.trim())
    is TranscriptEntry.Text -> copy(value = value.trim())
}

private fun TranscriptEntry.isNotEmpty() = when (this) {
    is TranscriptEntry.Speaker -> name.isNotEmpty()
    is TranscriptEntry.Text -> value.isNotEmpty()
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
