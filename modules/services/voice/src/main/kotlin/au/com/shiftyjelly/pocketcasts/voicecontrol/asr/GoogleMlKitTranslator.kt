package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Google ML Kit on-device translation (source language -> English).
 *
 * ML Kit downloads a per-language model on first use; [ensureReady] triggers that
 * download for a given source language. Translation is synchronous over the
 * ML Kit Task API, bridged to a suspend function.
 *
 * Language codes follow ML Kit's ISO-639-1 set. Cantonese (`yue`) is not a distinct
 * ML Kit language, so it is routed through Mandarin Chinese (`zh`) as a pragmatic
 * fallback (same script, closest supported model) rather than failing.
 */
@Singleton
class GoogleMlKitTranslator @Inject constructor() : TranslationStage {

    private val translators = ConcurrentHashMap<String, Translator>()

    override suspend fun ensureReady(sourceLanguage: String): Result<Unit> = withContext(Dispatchers.IO) {
        val mlKitLang = mapLanguage(sourceLanguage) ?: return@withContext Result.failure(
            IllegalArgumentException("Unsupported ML Kit source language: $sourceLanguage"),
        )
        try {
            val translator = translatorFor(mlKitLang)
            awaitTask(translator.downloadModelIfNeeded())
            Timber.i("ML Kit translation model ready for %s", sourceLanguage)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "ML Kit translation model download failed for %s", sourceLanguage)
            Result.failure(e)
        }
    }

    override suspend fun translate(text: String, sourceLanguage: String): String {
        val mlKitLang = mapLanguage(sourceLanguage)
            ?: return text // No ML Kit language mapping: pass through unchanged.
        return try {
            val translator = translatorFor(mlKitLang)
            awaitTask(translator.translate(text))
        } catch (e: Exception) {
            Timber.e(e, "ML Kit translation failed for %s: '%s'", sourceLanguage, text)
            // Graceful degradation: fall back to the untranslated text.
            text
        }
    }

    private fun translatorFor(mlKitLang: String): Translator = translators.getOrPut(mlKitLang) {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(mlKitLang)
                .setTargetLanguage(ML_KIT_ENGLISH)
                .build(),
        )
    }

    /**
     * Maps an ISO-639-1 (or SenseVoice LID) language code to the ML Kit translate
     * language constant. Returns null if there is no ML Kit equivalent.
     */
    private fun mapLanguage(language: String): String? {
        return when (language.lowercase(Locale.ROOT)) {
            "zh", "yue" -> "zh"

            // Cantonese -> Mandarin Chinese (closest supported model)
            "ja" -> "ja"

            "ko" -> "ko"

            "de" -> "de"

            "es" -> "es"

            "fr" -> "fr"

            "it" -> "it"

            "pt" -> "pt"

            "ru" -> "ru"

            "ar" -> "ar"

            "nl" -> "nl"

            "sv" -> "sv"

            else -> null
        }
    }

    private suspend fun <T> awaitTask(task: com.google.android.gms.tasks.Task<T>): T = suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener { result ->
            if (cont.isActive) cont.resumeWith(Result.success(result))
        }.addOnFailureListener { e ->
            if (cont.isActive) cont.resumeWith(Result.failure(e))
        }
    }

    companion object {
        private const val ML_KIT_ENGLISH = "en"
    }
}
