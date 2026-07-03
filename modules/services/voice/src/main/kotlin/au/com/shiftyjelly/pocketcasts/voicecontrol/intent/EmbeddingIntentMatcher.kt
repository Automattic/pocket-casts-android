package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding.EditDistance
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding.EmbeddingEngine
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding.TextTokenizer
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognitionContext
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Intent matcher using multilingual embeddings with edit-distance fallback.
 *
 * Pipeline:
 * 1. Embed the transcript and compute cosine similarity against pre-computed
 *    embeddings for English intent keywords.
 * 2. If embedding confidence is below threshold, fall back to normalized
 *    Levenshtein distance against known command strings.
 * 3. For parameterized intents, delegate to [EntityExtractor] for slot filling.
 */
@Singleton
class EmbeddingIntentMatcher @Inject constructor(
    private val tokenizer: TextTokenizer,
    private val embeddingEngine: EmbeddingEngine,
    private val entityExtractor: EntityExtractor,
) : VoiceRecognizer {

    /** Pre-computed intent keyword → averaged embedding vector. */
    private val intentEmbeddings = linkedMapOf<String, FloatArray>()

    /** Whether the model and tokenizer are loaded and keyword embeddings are ready. */
    private var initialized = false

    // -- Intent keyword definitions ----------------------------------------

    /**
     * Maps internal intent type to its English keyword phrases.
     * Keywords are used both for embedding (primary match) and edit distance (fallback).
     * Order matters: more specific intents with entity parameters come first.
     */
    private data class IntentDef(
        val intentType: String,
        val keywords: List<String>,
        val editDistanceStrings: List<String>,
    )

    private val intentDefs = listOf(
        IntentDef("pause", listOf("pause", "stop", "hold"), listOf("pause", "stop", "hold")),
        IntentDef("resume", listOf("resume", "play", "continue", "start"), listOf("resume", "play", "continue", "start")),
        IntentDef("seek_relative_forward", listOf("fast forward", "skip forward", "jump ahead", "skip ahead"), listOf("fast forward", "skip forward", "jump ahead", "skip ahead")),
        IntentDef("seek_relative_backward", listOf("rewind", "go back", "skip back", "jump back"), listOf("rewind", "go back", "skip back", "jump back")),
        IntentDef("next_chapter", listOf("next chapter", "skip chapter"), listOf("next chapter", "skip chapter")),
        IntentDef("previous_chapter", listOf("previous chapter", "last chapter", "prior chapter"), listOf("previous chapter", "last chapter", "prior chapter")),
        IntentDef("next_episode", listOf("next episode"), listOf("next episode")),
        IntentDef("set_speed", listOf("set speed", "change speed to"), listOf("set speed", "change speed")),
        IntentDef("adjust_speed_up", listOf("faster", "speed up", "increase speed"), listOf("faster", "speed up", "increase speed")),
        IntentDef("adjust_speed_down", listOf("slower", "speed down", "decrease speed"), listOf("slower", "speed down", "decrease speed")),
        IntentDef("set_volume", listOf("set volume"), listOf("set volume")),
        IntentDef("adjust_volume_up", listOf("volume up", "louder", "increase volume"), listOf("volume up", "louder", "increase volume")),
        IntentDef("adjust_volume_down", listOf("volume down", "quieter", "decrease volume"), listOf("volume down", "quieter", "decrease volume")),
        IntentDef("sleep_timer", listOf("sleep timer", "set timer", "stop in", "stop after"), listOf("sleep timer", "set timer", "stop in")),
        IntentDef("set_trim", listOf("trim silence", "silence trimming", "set trim"), listOf("trim silence", "silence trimming", "set trim")),
        IntentDef("set_volume_boost", listOf("volume boost", "loudness boost", "boost volume"), listOf("volume boost", "loudness boost")),
        IntentDef("add_bookmark", listOf("bookmark", "save this", "mark this spot", "add bookmark"), listOf("bookmark", "save this", "mark this spot")),
    )

    /** Flattened edit-distance lookup: command string → intent type. */
    private val editDistanceLookup: Map<String, String> = intentDefs.flatMap { def ->
        def.editDistanceStrings.map { it to def.intentType }
    }.toMap()

    // -- Model setup -------------------------------------------------------

    fun initialize(tokenizerPath: String, modelPath: String): Boolean {
        if (!tokenizer.load(tokenizerPath)) {
            Timber.e("Failed to load tokenizer")
            return false
        }
        if (!embeddingEngine.load(modelPath)) {
            Timber.e("Failed to load embedding model")
            return false
        }
        precomputeIntentEmbeddings()
        initialized = true
        Timber.i("EmbeddingIntentMatcher initialized (%d intent types)", intentEmbeddings.size)
        return true
    }

    override suspend fun ensureReady(): Result<Unit> {
        return if (initialized) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("EmbeddingIntentMatcher not initialized"))
        }
    }

    /** Pre-compute embeddings for all intent keywords once. */
    private fun precomputeIntentEmbeddings() {
        for (def in intentDefs) {
            val vectors = def.keywords.map { embedPassage(it) }
            intentEmbeddings[def.intentType] = averageAndNormalize(vectors)
        }
    }

    // -- VoiceRecognizer implementation ------------------------------------

    override suspend fun recognize(
        transcript: String,
        context: VoiceRecognitionContext,
    ): VoiceIntent? = withContext(Dispatchers.IO) {
        if (transcript.isBlank()) return@withContext null
        if (!initialized) {
            Timber.w("EmbeddingIntentMatcher not initialized")
            return@withContext null
        }

        val match = matchIntent(transcript) ?: return@withContext null
        assembleIntent(match.intentType, transcript)
    }

    override fun release() = Unit

    // -- Intent matching ---------------------------------------------------

    /**
     * Match [text] to an intent type using embedding cosine similarity,
     * falling back to edit distance if confidence is low.
     */
    private fun matchIntent(text: String): IntentMatchResult? {
        val queryEmbedding = embedQuery(text)

        // 1. Embedding-based matching
        var bestScore = 0.0
        var bestIntent: String? = null
        for ((intent, intentVec) in intentEmbeddings) {
            val score = cosineSimilarity(queryEmbedding, intentVec)
            if (score > bestScore) {
                bestScore = score
                bestIntent = intent
            }
        }

        if (bestScore >= EMBEDDING_THRESHOLD && bestIntent != null) {
            Timber.i("Intent: %s (embedding=%.3f)", bestIntent, bestScore)
            return IntentMatchResult(bestIntent, bestScore)
        }

        // 2. Edit-distance fallback
        val lowerText = text.lowercase().trim()
        var bestDist = Double.MAX_VALUE
        var bestFallbackIntent: String? = null
        for ((cmd, intent) in editDistanceLookup) {
            val dist = EditDistance.normalized(lowerText, cmd)
            if (dist < EDIT_DISTANCE_THRESHOLD && dist < bestDist) {
                bestDist = dist
                bestFallbackIntent = intent
            }
        }

        if (bestFallbackIntent != null) {
            Timber.i("Intent: %s (edit_distance=%.3f)", bestFallbackIntent, bestDist)
            return IntentMatchResult(bestFallbackIntent, 1.0 - bestDist)
        }

        Timber.d("No intent match (best_embedding=%.3f, best_edit=%.3f)", bestScore, 1.0 - bestDist)
        return null
    }

    // -- Intent assembly ---------------------------------------------------

    /**
     * Build a [VoiceIntent] from the matched intent type and extracted entities.
     * Parameterless intents are returned directly; parameterized intents delegate
     * to [entityExtractor] with defaults for unfound entities.
     */
    private fun assembleIntent(intentType: String, text: String): VoiceIntent? {
        return when (intentType) {
            "pause" -> VoiceIntent.Playback.Pause

            "resume" -> VoiceIntent.Playback.Resume

            "next_chapter" -> VoiceIntent.Chapter.NextChapter

            "previous_chapter" -> VoiceIntent.Chapter.PreviousChapter

            "next_episode" -> VoiceIntent.Playback.NextEpisode

            "seek_relative_forward" -> {
                val entities = entityExtractor.extract(text, intentType)
                val deltaMs = (entities.deltaSeconds ?: 30) * 1000
                VoiceIntent.Playback.SeekRelative(deltaMs)
            }

            "seek_relative_backward" -> {
                val entities = entityExtractor.extract(text, intentType)
                val deltaMs = -(entities.deltaSeconds ?: 30) * 1000
                VoiceIntent.Playback.SeekRelative(deltaMs)
            }

            "set_speed" -> {
                val speed = entityExtractor.extract(text, intentType).speed ?: return null
                if (speed in 0.5..5.0) VoiceIntent.Effects.SetSpeed(speed) else null
            }

            "adjust_speed_up" -> {
                val delta = entityExtractor.extract(text, intentType).speedDelta ?: 0.5
                VoiceIntent.Effects.AdjustSpeed(delta)
            }

            "adjust_speed_down" -> {
                val delta = entityExtractor.extract(text, intentType).speedDelta ?: -0.5
                VoiceIntent.Effects.AdjustSpeed(delta)
            }

            "set_volume" -> {
                val volume = entityExtractor.extract(text, intentType).volume ?: return null
                if (volume in 0..100) VoiceIntent.Volume.SetVolume(volume) else null
            }

            "adjust_volume_up" -> {
                val delta = entityExtractor.extract(text, intentType).volumeDelta ?: 10
                VoiceIntent.Volume.AdjustVolume(delta)
            }

            "adjust_volume_down" -> {
                val delta = entityExtractor.extract(text, intentType).volumeDelta ?: -10
                VoiceIntent.Volume.AdjustVolume(delta)
            }

            "sleep_timer" -> {
                val minutes = entityExtractor.extract(text, intentType).sleepMinutes ?: 30
                VoiceIntent.Sleep.Set(minutes)
            }

            "chapter_by_index" -> {
                val index = entityExtractor.extract(text, intentType).chapterIndex ?: return null
                VoiceIntent.Chapter.ByIndex(index)
            }

            "chapter_by_title" -> {
                val query = entityExtractor.extract(text, intentType).chapterTitle ?: return null
                VoiceIntent.Chapter.ByTitle(query)
            }

            "set_trim" -> {
                val mode = entityExtractor.extract(text, intentType).trimMode ?: return null
                VoiceIntent.Effects.SetTrimMode(mode)
            }

            "set_volume_boost" -> {
                val enabled = entityExtractor.extract(text, intentType).boostEnabled ?: true
                VoiceIntent.Effects.SetVolumeBoost(enabled)
            }

            "add_bookmark" -> {
                val title = entityExtractor.extract(text, intentType).bookmarkTitle ?: return null
                VoiceIntent.Bookmark.Add(title)
            }

            else -> {
                Timber.w("Unknown intent type: %s", intentType)
                null
            }
        }
    }

    // -- Embedding helpers -------------------------------------------------

    /** Embed text as a query (user utterance). */
    private fun embedQuery(text: String): FloatArray {
        val tokenIds = tokenizer.encode("query: $text")
        return embeddingEngine.embed(tokenIds)
    }

    /** Embed text as a passage (intent keyword). */
    private fun embedPassage(text: String): FloatArray {
        val tokenIds = tokenizer.encode("passage: $text")
        return embeddingEngine.embed(tokenIds)
    }

    /** Average multiple embedding vectors and L2-normalize the result. */
    private fun averageAndNormalize(vectors: List<FloatArray>): FloatArray {
        if (vectors.isEmpty()) return FloatArray(embeddingEngine.embeddingDim)
        val dim = embeddingEngine.embeddingDim
        val result = FloatArray(dim)
        for (vec in vectors) {
            for (i in 0 until dim) {
                result[i] += vec[i]
            }
        }
        val count = vectors.size.toFloat()
        for (i in 0 until dim) {
            result[i] /= count
        }
        return l2Normalize(result)
    }

    // -- Math utilities ----------------------------------------------------

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in a.indices) {
            dot += a[i].toDouble() * b[i].toDouble()
            normA += a[i].toDouble() * a[i].toDouble()
            normB += b[i].toDouble() * b[i].toDouble()
        }
        if (normA == 0.0 || normB == 0.0) return 0.0
        return dot / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
    }

    private fun l2Normalize(vec: FloatArray): FloatArray {
        var norm = 0.0
        for (v in vec) norm += v.toDouble() * v.toDouble()
        val scale = if (norm > 0.0) 1.0 / kotlin.math.sqrt(norm) else 1.0
        return FloatArray(vec.size) { i -> (vec[i].toDouble() * scale).toFloat() }
    }

    // -- Types -------------------------------------------------------------

    private data class IntentMatchResult(val intentType: String, val confidence: Double)

    companion object {
        private const val EMBEDDING_THRESHOLD = 0.6
        private const val EDIT_DISTANCE_THRESHOLD = 0.3
    }
}
