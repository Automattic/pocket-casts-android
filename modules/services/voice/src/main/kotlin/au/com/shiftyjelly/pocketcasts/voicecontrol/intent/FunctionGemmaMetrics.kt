package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import au.com.shiftyjelly.pocketcasts.voicecontrol.BuildConfig
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.FunctionGemmaBackend
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

data class FunctionGemmaInferenceMetrics(
    val backend: FunctionGemmaBackend,
    val modelRelease: String,
    val sessionWaitMs: Long,
    val requestPrefillMs: Long,
    val decodeMs: Long,
    val parseResolveMs: Long,
    val totalMs: Long,
    val inputCharacters: Int,
    val outputCharacters: Int,
    val fallbackReason: String?,
    val conversationReused: Boolean,
    val reuseCount: Int,
    val conversationRotated: Boolean,
    val rotationCause: String?,
)

interface FunctionGemmaMetrics {
    fun prepared(
        backend: FunctionGemmaBackend,
        modelRelease: String,
        engineInitMs: Long,
        sessionCreateMs: Long,
        staticPrefillMs: Long,
    )

    fun inference(metrics: FunctionGemmaInferenceMetrics)

    fun backendFallback(reason: String, error: Throwable)
}

@Singleton
class TimberFunctionGemmaMetrics @Inject constructor() : FunctionGemmaMetrics {
    override fun prepared(
        backend: FunctionGemmaBackend,
        modelRelease: String,
        engineInitMs: Long,
        sessionCreateMs: Long,
        staticPrefillMs: Long,
    ) {
        Timber.i(
            "FunctionGemma prepared backend=%s model=%s runtime=%s engineInitMs=%d " +
                "sessionCreateMs=%d staticPrefillMs=%d",
            backend,
            modelRelease,
            BuildConfig.LITERTLM_VERSION,
            engineInitMs,
            sessionCreateMs,
            staticPrefillMs,
        )
    }

    override fun inference(metrics: FunctionGemmaInferenceMetrics) {
        Timber.i(
            "FunctionGemma inference backend=%s model=%s runtime=%s wait=%d pref=%d dec=%d " +
                "parse=%d total=%d inCh=%d outCh=%d fallback=%s reused=%s reuseCt=%d " +
                "rotated=%s rotCause=%s",
            metrics.backend,
            metrics.modelRelease,
            BuildConfig.LITERTLM_VERSION,
            metrics.sessionWaitMs,
            metrics.requestPrefillMs,
            metrics.decodeMs,
            metrics.parseResolveMs,
            metrics.totalMs,
            metrics.inputCharacters,
            metrics.outputCharacters,
            metrics.fallbackReason,
            metrics.conversationReused,
            metrics.reuseCount,
            metrics.conversationRotated,
            metrics.rotationCause,
        )
    }

    override fun backendFallback(reason: String, error: Throwable) {
        Timber.w(
            "FunctionGemma fallback reason=%s error=%s message=%s",
            reason,
            error::class.java.simpleName,
            error.message.orEmpty().take(MAX_LOGGED_ERROR_CHARS),
        )
    }

    private companion object {
        const val MAX_LOGGED_ERROR_CHARS = 200
    }
}
