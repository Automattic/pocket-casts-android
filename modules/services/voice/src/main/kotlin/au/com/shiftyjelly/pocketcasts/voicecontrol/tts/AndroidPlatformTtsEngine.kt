package au.com.shiftyjelly.pocketcasts.voicecontrol.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber

class AndroidPlatformTtsEngine @Inject constructor(
    @ApplicationContext context: Context,
) : TtsEngine {
    private var tts: TextToSpeech? = null
    private var initialized = false
    private var released = false

    init {
        tts = TextToSpeech(context) { status ->
            initialized = (status == TextToSpeech.SUCCESS)
            if (!initialized) {
                Timber.w("TTS engine initialization failed with status $status")
            }
        }
    }

    override suspend fun warmUp(language: String) {
        withContext(Dispatchers.Main) {
            val locale = localeForLanguageTag(language)
            tts?.language = locale
        }
        val start = System.currentTimeMillis()
        while (!initialized && (System.currentTimeMillis() - start) < 5000) {
            delay(50)
        }
    }

    override suspend fun speak(text: String, language: String) {
        if (released || tts == null || !initialized) return
        suspendCancellableCoroutine { continuation ->
            val locale = localeForLanguageTag(language)
            tts?.let { engine ->
                val result = engine.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Timber.w("TTS language $language not available, falling back to default")
                    engine.language = Locale.getDefault()
                }

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onDone(utteranceId: String?) {
                        continuation.resume(Unit)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Timber.w("TTS utterance error for utteranceId=$utteranceId")
                        continuation.resume(Unit)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStart(utteranceId: String?) {}
                })

                val utteranceId = System.currentTimeMillis().toString()
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } ?: continuation.resume(Unit)
        }
    }

    override fun release() {
        released = true
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun localeForLanguageTag(tag: String): Locale = Locale.forLanguageTag(tag)
}
