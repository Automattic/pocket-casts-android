package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import android.content.Context
import au.com.shiftyjelly.pocketcasts.servers.di.NoCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    @ApplicationContext private val context: Context,
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
                delay(delayMs)
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.w("FingerprintReferenceRetriever: fetch failed for $episodeUuid, attempt ${attempt + 1}/$MAX_RETRIES — ${e.message}")
                if (attempt == MAX_RETRIES - 1) return@withContext null
            }
        }

        Timber.w("FingerprintReferenceRetriever: exhausted retries for $episodeUuid")
        null
    }

    suspend fun saveReferenceData(data: ByteArray, audioFilePath: String) = withContext(Dispatchers.IO) {
        val path = referencePath(audioFilePath)
        try {
            File(path).writeBytes(data)
            Timber.d("FingerprintReferenceRetriever: saved reference to $path")
        } catch (e: Exception) {
            Timber.w(e, "FingerprintReferenceRetriever: failed to save reference to $path")
        }
    }

    suspend fun loadReferenceData(audioFilePath: String): ByteArray? = withContext(Dispatchers.IO) {
        val path = referencePath(audioFilePath)
        val file = File(path)
        if (file.exists()) file.readBytes() else null
    }

    suspend fun loadCachedReference(episodeUuid: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val file = cachedReferenceFile(episodeUuid)
            if (!file.exists()) return@runCatching null
            file.setLastModified(System.currentTimeMillis())
            file.readBytes()
        }.getOrNull()
    }

    suspend fun saveCachedReference(episodeUuid: String, data: ByteArray) = withContext(Dispatchers.IO) {
        runCatching {
            val file = cachedReferenceFile(episodeUuid)
            file.parentFile?.mkdirs()
            file.writeBytes(data)
            pruneCachedReferences()
        }.onFailure { Timber.w(it, "FingerprintReferenceRetriever: failed to cache reference for $episodeUuid") }
        Unit
    }

    private fun cachedReferenceFile(episodeUuid: String) = File(File(context.cacheDir, CACHE_DIR_NAME), "$episodeUuid.ref.fp.json")

    private fun pruneCachedReferences() {
        val files = File(context.cacheDir, CACHE_DIR_NAME).listFiles() ?: return
        files.sortedByDescending { it.lastModified() }
            .drop(REFERENCE_CACHE_MAX_FILES)
            .forEach { it.delete() }
    }

    companion object {
        private const val MAX_RETRIES = 3
        private const val CACHE_DIR_NAME = "episode_fingerprints"
        private const val REFERENCE_CACHE_MAX_FILES = 20

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
