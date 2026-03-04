package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.ParserException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo
import au.com.shiftyjelly.pocketcasts.repositories.playback.PocketCastsLoadErrorHandlingPolicy.Companion.BASE_DELAY_MS
import au.com.shiftyjelly.pocketcasts.repositories.playback.PocketCastsLoadErrorHandlingPolicy.Companion.MAX_DELAY_MS
import au.com.shiftyjelly.pocketcasts.repositories.playback.PocketCastsLoadErrorHandlingPolicy.Companion.MAX_RETRIES_MANIFEST
import au.com.shiftyjelly.pocketcasts.repositories.playback.PocketCastsLoadErrorHandlingPolicy.Companion.MAX_RETRIES_MEDIA
import au.com.shiftyjelly.pocketcasts.repositories.playback.PocketCastsLoadErrorHandlingPolicy.Companion.MAX_RETRIES_OTHER
import au.com.shiftyjelly.pocketcasts.repositories.playback.PocketCastsLoadErrorHandlingPolicy.ErrorClassification
import java.io.FileNotFoundException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@UnstableApi
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class PocketCastsLoadErrorHandlingPolicyTest {

    private val policy = PocketCastsLoadErrorHandlingPolicy()

    // region Error Classification — HTTP codes

    @Test
    fun `HTTP 404 is non-retryable`() {
        val classification = policy.classifyError(createHttpException(404))
        assertEquals(ErrorClassification.NonRetryable, classification)
    }

    @Test
    fun `HTTP 410 is non-retryable`() {
        val classification = policy.classifyError(createHttpException(410))
        assertEquals(ErrorClassification.NonRetryable, classification)
    }

    @Test
    fun `HTTP 401 is non-retryable`() {
        val classification = policy.classifyError(createHttpException(401))
        assertEquals(ErrorClassification.NonRetryable, classification)
    }

    @Test
    fun `HTTP 403 is non-retryable`() {
        val classification = policy.classifyError(createHttpException(403))
        assertEquals(ErrorClassification.NonRetryable, classification)
    }

    @Test
    fun `HTTP 400 is non-retryable`() {
        val classification = policy.classifyError(createHttpException(400))
        assertEquals(ErrorClassification.NonRetryable, classification)
    }

    @Test
    fun `HTTP 416 is non-retryable`() {
        val classification = policy.classifyError(createHttpException(416))
        assertEquals(ErrorClassification.NonRetryable, classification)
    }

    @Test
    fun `HTTP 408 is retryable`() {
        val classification = policy.classifyError(createHttpException(408))
        assertEquals(ErrorClassification.RetryableHttp(408), classification)
    }

    @Test
    fun `HTTP 429 is retryable`() {
        val classification = policy.classifyError(createHttpException(429))
        assertEquals(ErrorClassification.RetryableHttp(429), classification)
    }

    @Test
    fun `HTTP 500 is retryable`() {
        val classification = policy.classifyError(createHttpException(500))
        assertEquals(ErrorClassification.RetryableHttp(500), classification)
    }

    @Test
    fun `HTTP 502 is retryable`() {
        val classification = policy.classifyError(createHttpException(502))
        assertEquals(ErrorClassification.RetryableHttp(502), classification)
    }

    @Test
    fun `HTTP 503 is retryable`() {
        val classification = policy.classifyError(createHttpException(503))
        assertEquals(ErrorClassification.RetryableHttp(503), classification)
    }

    @Test
    fun `HTTP 599 is retryable`() {
        val classification = policy.classifyError(createHttpException(599))
        assertEquals(ErrorClassification.RetryableHttp(599), classification)
    }

    // endregion

    // region Error Classification — Exception types

    @Test
    fun `ParserException is non-retryable`() {
        val exception = ParserException.createForMalformedContainer("bad data", null)
        val classification = policy.classifyError(exception)
        assertEquals(ErrorClassification.NonRetryable, classification)
    }

    @Test
    fun `FileNotFoundException is non-retryable`() {
        val exception = FileNotFoundException("episode.mp3")
        val classification = policy.classifyError(exception)
        assertEquals(ErrorClassification.NonRetryable, classification)
    }

    @Test
    fun `generic IOException is retryable network error`() {
        val exception = IOException("Connection reset")
        val classification = policy.classifyError(exception)
        assertEquals(ErrorClassification.RetryableNetwork, classification)
    }

    // endregion

    // region Exponential Backoff (no jitter)

    @Test
    fun `exponential backoff follows expected progression`() {
        assertEquals(1_000L, policy.exponentialBackoff(1))
        assertEquals(2_000L, policy.exponentialBackoff(2))
        assertEquals(4_000L, policy.exponentialBackoff(3))
        assertEquals(8_000L, policy.exponentialBackoff(4))
        assertEquals(16_000L, policy.exponentialBackoff(5))
        assertEquals(30_000L, policy.exponentialBackoff(6))
    }

    @Test
    fun `exponential backoff caps at max delay`() {
        assertEquals(MAX_DELAY_MS, policy.exponentialBackoff(10))
        assertEquals(MAX_DELAY_MS, policy.exponentialBackoff(100))
    }

    // endregion

    // region getRetryDelayMsFor — integration

    @Test
    fun `HTTP 416 returns TIME_UNSET on first attempt`() {
        val loadErrorInfo = createLoadErrorInfo(
            exception = createHttpException(416),
            errorCount = 1,
            dataType = C.DATA_TYPE_MEDIA,
        )
        assertEquals(C.TIME_UNSET, policy.getRetryDelayMsFor(loadErrorInfo))
    }

    @Test
    fun `HTTP 500 returns positive delay on first attempt`() {
        val loadErrorInfo = createLoadErrorInfo(
            exception = createHttpException(500),
            errorCount = 1,
            dataType = C.DATA_TYPE_MEDIA,
        )
        val delay = policy.getRetryDelayMsFor(loadErrorInfo)
        assertTrue("Delay should be positive, was $delay", delay > 0)
    }

    @Test
    fun `media data type allows up to MAX_RETRIES_MEDIA retries`() {
        val exception = createHttpException(500)
        val lastAllowedRetry = createLoadErrorInfo(
            exception = exception,
            errorCount = MAX_RETRIES_MEDIA,
            dataType = C.DATA_TYPE_MEDIA,
        )
        assertTrue(policy.getRetryDelayMsFor(lastAllowedRetry) > 0)

        val exhaustedRetry = createLoadErrorInfo(
            exception = exception,
            errorCount = MAX_RETRIES_MEDIA + 1,
            dataType = C.DATA_TYPE_MEDIA,
        )
        assertEquals(C.TIME_UNSET, policy.getRetryDelayMsFor(exhaustedRetry))
    }

    @Test
    fun `manifest data type allows up to MAX_RETRIES_MANIFEST retries`() {
        val exception = createHttpException(503)
        val lastAllowedRetry = createLoadErrorInfo(
            exception = exception,
            errorCount = MAX_RETRIES_MANIFEST,
            dataType = C.DATA_TYPE_MANIFEST,
        )
        assertTrue(policy.getRetryDelayMsFor(lastAllowedRetry) > 0)

        val exhaustedRetry = createLoadErrorInfo(
            exception = exception,
            errorCount = MAX_RETRIES_MANIFEST + 1,
            dataType = C.DATA_TYPE_MANIFEST,
        )
        assertEquals(C.TIME_UNSET, policy.getRetryDelayMsFor(exhaustedRetry))
    }

    @Test
    fun `other data type allows up to MAX_RETRIES_OTHER retries`() {
        val exception = IOException("timeout")
        val lastAllowedRetry = createLoadErrorInfo(
            exception = exception,
            errorCount = MAX_RETRIES_OTHER,
            dataType = C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE,
        )
        assertTrue(policy.getRetryDelayMsFor(lastAllowedRetry) > 0)

        val exhaustedRetry = createLoadErrorInfo(
            exception = exception,
            errorCount = MAX_RETRIES_OTHER + 1,
            dataType = C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE,
        )
        assertEquals(C.TIME_UNSET, policy.getRetryDelayMsFor(exhaustedRetry))
    }

    @Test
    fun `network IOException uses exponential backoff delay`() {
        val errorCount = 3
        val loadErrorInfo = createLoadErrorInfo(
            exception = IOException("network"),
            errorCount = errorCount,
            dataType = C.DATA_TYPE_MEDIA,
        )
        assertEquals(
            policy.exponentialBackoff(errorCount),
            policy.getRetryDelayMsFor(loadErrorInfo),
        )
    }

    // endregion

    // region getMinimumLoadableRetryCount

    @Test
    fun `getMinimumLoadableRetryCount returns MAX_VALUE for media`() {
        assertEquals(Int.MAX_VALUE, policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA))
    }

    @Test
    fun `getMinimumLoadableRetryCount returns MAX_VALUE for manifest`() {
        assertEquals(Int.MAX_VALUE, policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MANIFEST))
    }

    @Test
    fun `getMinimumLoadableRetryCount returns MAX_VALUE for other`() {
        assertEquals(Int.MAX_VALUE, policy.getMinimumLoadableRetryCount(C.DATA_TYPE_DRM))
    }

    // endregion

    // region Helpers

    private fun createHttpException(responseCode: Int): InvalidResponseCodeException {
        val dataSpec = DataSpec(Uri.parse("https://example.com/episode.mp3"))
        return InvalidResponseCodeException(
            responseCode,
            "HTTP $responseCode",
            null,
            emptyMap(),
            dataSpec,
            ByteArray(0),
        )
    }

    private fun createLoadErrorInfo(
        exception: IOException,
        errorCount: Int,
        dataType: Int,
    ): LoadErrorInfo {
        val dataSpec = DataSpec(Uri.parse("https://example.com/episode.mp3"))
        val loadEventInfo = LoadEventInfo(
            0L,
            dataSpec,
            SystemClock.elapsedRealtime(),
        )
        val mediaLoadData = MediaLoadData(dataType)
        return LoadErrorInfo(
            loadEventInfo,
            mediaLoadData,
            exception,
            errorCount,
        )
    }

    // endregion
}
