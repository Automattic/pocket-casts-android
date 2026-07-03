package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.entity

import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.EntityExtractor
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.EntityResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates per-language grammars to extract entities from voice command transcripts.
 * Detects the language via script heuristics and dispatches to the appropriate grammar.
 *
 * Implements [EntityExtractor] to provide grammar-based entity extraction
 * for use by [au.com.shiftyjelly.pocketcasts.voicecontrol.intent.EmbeddingIntentMatcher].
 */
@Singleton
class GrammarEntityExtractor @Inject constructor(
    private val enGrammar: EnGrammar,
    private val zhGrammar: ZhGrammar,
) : EntityExtractor {

    private val grammars: List<LanguageGrammar> = listOf(enGrammar, zhGrammar)
    private val defaultGrammar: LanguageGrammar = enGrammar

    override fun extract(text: String, intentType: String): EntityResult {
        val grammar = detectLanguage(text)

        return when (intentType) {
            "seek_relative_forward" -> {
                val durations = grammar.extractDuration(text)
                EntityResult(deltaSeconds = durations.firstOrNull())
            }

            "seek_relative_backward" -> {
                val durations = grammar.extractDuration(text)
                EntityResult(deltaSeconds = durations.firstOrNull())
            }

            "seek_absolute" -> {
                val durations = grammar.extractDuration(text)
                EntityResult(positionSeconds = durations.firstOrNull())
            }

            "set_speed" -> {
                val numbers = grammar.extractNumber(text)
                EntityResult(speed = numbers.firstOrNull())
            }

            "adjust_speed_up" -> {
                val numbers = grammar.extractNumber(text)
                EntityResult(speedDelta = numbers.firstOrNull())
            }

            "adjust_speed_down" -> {
                val numbers = grammar.extractNumber(text)
                EntityResult(speedDelta = numbers.firstOrNull())
            }

            "set_volume" -> {
                val numbers = grammar.extractNumber(text)
                EntityResult(volume = numbers.firstOrNull()?.toInt())
            }

            "adjust_volume_up" -> {
                val numbers = grammar.extractNumber(text)
                EntityResult(volumeDelta = numbers.firstOrNull()?.toInt())
            }

            "adjust_volume_down" -> {
                val numbers = grammar.extractNumber(text)
                EntityResult(volumeDelta = numbers.firstOrNull()?.toInt())
            }

            "sleep_timer" -> {
                val durations = grammar.extractDuration(text)
                val seconds = durations.firstOrNull()
                EntityResult(sleepMinutes = seconds?.div(60))
            }

            "chapter_by_index" -> {
                val ordinals = grammar.extractOrdinal(text)
                EntityResult(chapterIndex = ordinals.firstOrNull())
            }

            "chapter_by_title" -> {
                // Title is whatever remains after removing known command words.
                // For now, return the full text as the query.
                EntityResult(chapterTitle = text.trim())
            }

            "set_trim" -> {
                EntityResult(trimMode = grammar.extractTrimMode(text))
            }

            "set_volume_boost" -> {
                EntityResult(boostEnabled = grammar.extractBoolean(text))
            }

            "add_bookmark" -> {
                EntityResult(bookmarkTitle = text.trim())
            }

            else -> EntityResult()
        }
    }

    private fun detectLanguage(text: String): LanguageGrammar {
        for (grammar in grammars) {
            if (grammar.canParse(text)) return grammar
        }
        return defaultGrammar
    }
}
