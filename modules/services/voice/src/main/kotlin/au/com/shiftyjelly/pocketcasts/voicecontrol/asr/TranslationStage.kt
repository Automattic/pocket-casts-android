package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

/**
 * Converts a native-language transcript to English when the source language is not
 * English and the ASR backend did not already translate.
 *
 * The sole implementation is Google ML Kit (on-device translation).
 */
interface TranslationStage {
    /** Download (if needed) the model for a source language and initialize. */
    suspend fun ensureReady(sourceLanguage: String): Result<Unit>

    /** Translate UTF-8 text from [sourceLanguage] to English. */
    suspend fun translate(text: String, sourceLanguage: String): String
}
