package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.servers.di.NoCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

@Singleton
class FingerprintReferenceRetriever @Inject constructor(
    @NoCache private val okHttpClient: OkHttpClient,
) {
    private val inFlightRequests = mutableMapOf<String, Deferred<ByteArray?>>()
    private val requestMutex = Mutex()

    suspend fun fetchReferenceData(
        baseUrl: String,
        podcastUuid: String,
        episodeUuid: String,
    ): ByteArray? = coroutineScope {
        val key = "$podcastUuid/$episodeUuid"

        val deferred = requestMutex.withLock {
            val existing = inFlightRequests[key]
            if (existing != null && existing.isActive) {
                existing
            } else {
                async {
                    try {
                        performFetch(baseUrl, podcastUuid, episodeUuid)
                    } finally {
                        withContext(NonCancellable) { requestMutex.withLock { inFlightRequests.remove(key) } }
                    }
                }.also { inFlightRequests[key] = it }
            }
        }
        deferred.await()
    }

    private suspend fun performFetch(
        baseUrl: String,
        podcastUuid: String,
        episodeUuid: String,
    ): ByteArray? = withContext(Dispatchers.IO) {
        val url = "${baseUrl}$podcastUuid/$episodeUuid-fingerprints.json.gz"

        for (attempt in 0 until MAX_RETRIES) {
            coroutineContext.ensureActive()

            if (attempt > 0) {
                val delayMs = (1L shl attempt) * 1000L
                kotlinx.coroutines.delay(delayMs)
            }

            try {
                val request = Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    val statusCode = response.code

                    if (statusCode == 404 || statusCode == 403) {
                        Timber.d("FingerprintReferenceRetriever: no reference for $episodeUuid ($statusCode)")
                        return@withContext null
                    }

                    if (statusCode != 200) {
                        if (statusCode >= 500) {
                            Timber.w("FingerprintReferenceRetriever: server error $statusCode for $episodeUuid, attempt ${attempt + 1}/$MAX_RETRIES")
                            continue
                        }
                        Timber.w("FingerprintReferenceRetriever: unexpected status $statusCode for $episodeUuid")
                        return@withContext null
                    }

                    val body = response.body.bytes()
                    val jsonData = decompressGzipIfNeeded(body)

                    Timber.d("FingerprintReferenceRetriever: reference fetched for $episodeUuid (${jsonData.size} bytes)")
                    return@withContext jsonData
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: java.io.IOException) {
                Timber.w("FingerprintReferenceRetriever: fetch failed for $episodeUuid, attempt ${attempt + 1}/$MAX_RETRIES — ${e.message}")
                if (attempt == MAX_RETRIES - 1) return@withContext null
            }
        }

        Timber.w("FingerprintReferenceRetriever: exhausted retries for $episodeUuid")
        null
    }

    fun saveReferenceData(data: ByteArray, audioFilePath: String) {
        val path = referencePath(audioFilePath)
        try {
            File(path).writeBytes(data)
            Timber.d("FingerprintReferenceRetriever: saved reference to $path")
        } catch (e: Exception) {
            Timber.w(e, "FingerprintReferenceRetriever: failed to save reference to $path")
        }
    }

    fun loadReferenceData(audioFilePath: String): ByteArray? {
        val path = referencePath(audioFilePath)
        val file = File(path)
        return if (file.exists()) file.readBytes() else null
    }

    companion object {
        private const val MAX_RETRIES = 3

        fun referencePath(audioFilePath: String): String {
            val dotIndex = audioFilePath.lastIndexOf('.')
            val base = if (dotIndex > 0) audioFilePath.substring(0, dotIndex) else audioFilePath
            return "$base.ref.fp.json"
        }

        fun decompressGzipIfNeeded(data: ByteArray): ByteArray {
            if (data.size < 2) return data
            if (data[0] == 0x1f.toByte() && data[1] == 0x8b.toByte()) {
                return decompressGzip(data)
            }
            return data
        }

        private fun decompressGzip(data: ByteArray): ByteArray {
            return GZIPInputStream(data.inputStream()).use { gzip ->
                val output = ByteArrayOutputStream()
                gzip.copyTo(output)
                output.toByteArray()
            }
        }
    }
}
