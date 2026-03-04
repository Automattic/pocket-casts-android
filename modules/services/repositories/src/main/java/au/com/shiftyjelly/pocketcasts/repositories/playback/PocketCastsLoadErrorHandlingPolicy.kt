package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.annotation.VisibleForTesting
import androidx.media3.common.C
import androidx.media3.common.ParserException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.io.FileNotFoundException

@UnstableApi
class PocketCastsLoadErrorHandlingPolicy : LoadErrorHandlingPolicy {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorInfo): Long {
        val exception = loadErrorInfo.exception
        val errorCount = loadErrorInfo.errorCount
        val dataType = loadErrorInfo.mediaLoadData.dataType

        val maxRetries = maxRetriesForDataType(dataType)

        if (errorCount > maxRetries) {
            LogBuffer.e(
                LogBuffer.TAG_PLAYBACK,
                exception,
                "Load error retry limit reached (%d/%d) for data type %d: %s",
                errorCount,
                maxRetries,
                dataType,
                exception.message.orEmpty(),
            )
            return C.TIME_UNSET
        }

        return when (val classification = classifyError(exception)) {
            ErrorClassification.NonRetryable -> {
                LogBuffer.e(
                    LogBuffer.TAG_PLAYBACK,
                    exception,
                    "Non-retryable load error: %s",
                    exception.message.orEmpty(),
                )
                C.TIME_UNSET
            }

            is ErrorClassification.RetryableHttp -> {
                val delay = exponentialBackoff(errorCount)
                LogBuffer.i(
                    LogBuffer.TAG_PLAYBACK,
                    exception,
                    "Retryable HTTP %d error, attempt %d/%d, delay %dms",
                    classification.responseCode,
                    errorCount,
                    maxRetries,
                    delay,
                )
                delay
            }

            ErrorClassification.RetryableNetwork -> {
                val delay = exponentialBackoff(errorCount)
                LogBuffer.i(
                    LogBuffer.TAG_PLAYBACK,
                    exception,
                    "Retryable network error, attempt %d/%d, delay %dms: %s",
                    errorCount,
                    maxRetries,
                    delay,
                    exception.message.orEmpty(),
                )
                delay
            }
        }
    }

    override fun getFallbackSelectionFor(
        fallbackOptions: LoadErrorHandlingPolicy.FallbackOptions,
        loadErrorInfo: LoadErrorInfo,
    ): LoadErrorHandlingPolicy.FallbackSelection? {
        return null
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int {
        return Int.MAX_VALUE
    }

    @VisibleForTesting
    internal fun classifyError(exception: java.io.IOException): ErrorClassification {
        if (exception is ParserException) {
            return ErrorClassification.NonRetryable
        }
        if (exception is FileNotFoundException) {
            return ErrorClassification.NonRetryable
        }
        if (exception is InvalidResponseCodeException) {
            return classifyHttpError(exception.responseCode)
        }
        return ErrorClassification.RetryableNetwork
    }

    private fun classifyHttpError(responseCode: Int): ErrorClassification {
        return when (responseCode) {
            408, 429 -> ErrorClassification.RetryableHttp(responseCode)
            in 500..599 -> ErrorClassification.RetryableHttp(responseCode)
            in 400..499 -> ErrorClassification.NonRetryable
            else -> ErrorClassification.RetryableHttp(responseCode)
        }
    }

    @VisibleForTesting
    internal fun exponentialBackoff(errorCount: Int): Long {
        val delay = BASE_DELAY_MS * (1L shl (errorCount - 1).coerceAtMost(MAX_SHIFT))
        return delay.coerceAtMost(MAX_DELAY_MS)
    }

    private fun maxRetriesForDataType(dataType: Int): Int {
        return when (dataType) {
            C.DATA_TYPE_MEDIA -> MAX_RETRIES_MEDIA
            C.DATA_TYPE_MANIFEST -> MAX_RETRIES_MANIFEST
            else -> MAX_RETRIES_OTHER
        }
    }

    @VisibleForTesting
    internal sealed interface ErrorClassification {
        data object NonRetryable : ErrorClassification
        data class RetryableHttp(val responseCode: Int) : ErrorClassification
        data object RetryableNetwork : ErrorClassification
    }

    internal companion object {
        const val BASE_DELAY_MS = 1_000L
        const val MAX_DELAY_MS = 30_000L
        const val MAX_RETRIES_MEDIA = 6
        const val MAX_RETRIES_MANIFEST = 4
        const val MAX_RETRIES_OTHER = 3
        private const val MAX_SHIFT = 20
    }
}
