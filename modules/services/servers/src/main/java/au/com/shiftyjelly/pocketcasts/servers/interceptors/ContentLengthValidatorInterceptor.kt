package au.com.shiftyjelly.pocketcasts.servers.interceptors

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.net.ProtocolException
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer

internal class ContentLengthValidatorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!FeatureFlag.isEnabled(Feature.VALIDATE_CONTENT_LENGTH)) {
            return response
        }
        if (request.method != "GET" || response.code !in VALIDATED_STATUS_CODES) {
            return response
        }
        val body = response.body
        val declaredLength = body.contentLength()
        if (declaredLength <= 0) {
            return response
        }

        val validatingSource = TruncationDetectingSource(body.source(), declaredLength, request.url.redact())
        return response.newBuilder()
            .body(validatingSource.buffer().asResponseBody(body.contentType(), declaredLength))
            .build()
    }

    private companion object {
        val VALIDATED_STATUS_CODES = setOf(200, 206)
    }
}

private class TruncationDetectingSource(
    delegate: Source,
    private val declaredLength: Long,
    private val redactedUrl: String,
) : ForwardingSource(delegate) {
    private var bytesRead = 0L
    private val tolerance = maxOf(MIN_TOLERANCE_BYTES, minOf(declaredLength / 10, MAX_TOLERANCE_BYTES))

    override fun read(sink: Buffer, byteCount: Long): Long {
        val read = super.read(sink, byteCount)
        if (read == -1L) {
            if (declaredLength - bytesRead > tolerance) {
                throw ProtocolException(
                    "unexpected end of stream: received $bytesRead of $declaredLength bytes from $redactedUrl",
                )
            }
        } else {
            bytesRead += read
        }
        return read
    }

    private companion object {
        const val MIN_TOLERANCE_BYTES = 512L * 1024L
        const val MAX_TOLERANCE_BYTES = 2L * 1024L * 1024L
    }
}
