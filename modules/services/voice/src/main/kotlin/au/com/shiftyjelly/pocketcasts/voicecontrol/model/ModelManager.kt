package au.com.shiftyjelly.pocketcasts.voicecontrol.model

import android.content.Context
import android.system.Os
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

internal data class FunctionGemmaAsset(
    val name: String,
    val url: String,
    val bytes: Long,
    val sha256: String,
)

internal data class FunctionGemmaRelease(
    val version: String,
    val requiredAssets: List<FunctionGemmaAsset>,
)

internal fun parseFunctionGemmaManifest(json: String): FunctionGemmaRelease {
    val manifest = JSONObject(json)
    val version = manifest.getString("version")
    val assets = manifest.getJSONObject("assets")
    val requiredNames = assets.keys().asSequence()
        .filter { it == ModelManager.FUNCTION_GEMMA_MODEL_FILENAME || it.startsWith("model.litertlm.xnnpack_cache_") }
        .sorted()
        .toList()
    require(requiredNames.count { it == ModelManager.FUNCTION_GEMMA_MODEL_FILENAME } == 1) {
        "FunctionGemma manifest must contain model.litertlm"
    }
    require(requiredNames.count { it.startsWith("model.litertlm.xnnpack_cache_") } == 1) {
        "FunctionGemma manifest must contain exactly one XNNPack cache"
    }
    val requiredAssets = requiredNames.map { name ->
        require(name == File(name).name) { "Invalid FunctionGemma asset name: $name" }
        val asset = assets.getJSONObject(name)
        FunctionGemmaAsset(
            name = name,
            url = asset.getString("url"),
            bytes = asset.getLong("bytes"),
            sha256 = asset.getString("sha256"),
        )
    }
    return FunctionGemmaRelease(version, requiredAssets)
}

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val downloadMutex = Mutex()
    companion object {
        private const val MOONSHINE_BASE_URL =
            "https://download.moonshine.ai/model/small-streaming-en/quantized"

        private val MOONSHINE_FILES = listOf(
            "adapter.ort",
            "cross_kv.ort",
            "decoder_kv.ort",
            "decoder_kv_with_attention.ort",
            "encoder.ort",
            "frontend.ort",
            "streaming_config.json",
            "tokenizer.bin",
        )

        // Use hf-mirror.com for faster/reliable downloads from HuggingFace
        private const val HF_MIRROR = "https://hf-mirror.com"

        // multilingual-e5-small ONNX (INT8 quantized, ~118 MB)
        private const val EMBEDDING_MODEL_PATH =
            "/nixiesearch/multilingual-e5-small-onnx/resolve/main/model_opt2_QInt8.onnx"
        const val EMBEDDING_MODEL_FILENAME = "model_opt2_QInt8.onnx"

        // HuggingFace tokenizer.json (~16 MB, JSON — parseable in pure Kotlin)
        // Preferred over sentencepiece.bpe.model (~5 MB, protobuf) to avoid
        // needing a protobuf parser in the BpeTokenizer.
        private const val TOKENIZER_PATH =
            "/intfloat/multilingual-e5-small/resolve/main/tokenizer.json"
        const val TOKENIZER_FILENAME = "tokenizer.json"

        internal const val FUNCTION_GEMMA_MODEL_FILENAME = "model.litertlm"
        private const val FUNCTION_GEMMA_MANIFEST_FILENAME = "manifest.json"
        private const val FUNCTION_GEMMA_LATEST_URL =
            "https://download.auris.fm/function-call/latest.json"
    }

    @VisibleForTesting
    internal var filesDir: File = context.filesDir

    val moonshineDir get() = File(filesDir, "moonshine-model")

    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotStarted)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    // -- Embedding model (multilingual-e5-small) ---------------------------

    val embeddingDir get() = File(filesDir, "embedding-model")
    val embeddingModelFile get() = File(embeddingDir, EMBEDDING_MODEL_FILENAME)
    val tokenizerModelFile get() = File(embeddingDir, TOKENIZER_FILENAME)

    fun isEmbeddingModelReady(): Boolean = embeddingModelFile.exists() && tokenizerModelFile.exists()

    suspend fun ensureEmbeddingModel(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isEmbeddingModelReady()) return@withContext Result.success(Unit)
        downloadMutex.withLock {
            if (isEmbeddingModelReady()) return@withContext Result.success(Unit)
            try {
                embeddingDir.mkdirs()
                downloadFile("$HF_MIRROR$EMBEDDING_MODEL_PATH", embeddingModelFile, "Embedding ONNX", "")
                downloadFile("$HF_MIRROR$TOKENIZER_PATH", tokenizerModelFile, "Tokenizer", "")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Embedding model download failed")
                Result.failure(e)
            }
        }
    }

    // -- FunctionGemma model -------------------------------------------------

    val functionGemmaDir get() = File(filesDir, "functiongemma-model")
    val functionGemmaModelFile get() = File(functionGemmaDir, FUNCTION_GEMMA_MODEL_FILENAME)
    private val functionGemmaManifestFile get() = File(functionGemmaDir, FUNCTION_GEMMA_MANIFEST_FILENAME)

    fun isFunctionGemmaModelReady(): Boolean {
        if (!functionGemmaManifestFile.exists()) return false
        return try {
            val release = parseFunctionGemmaManifest(functionGemmaManifestFile.readText())
            release.requiredAssets.all { asset ->
                val file = File(functionGemmaDir, asset.name)
                file.isFile && file.length() == asset.bytes
            }
        } catch (e: Exception) {
            Timber.w(e, "FunctionGemma manifest is invalid")
            false
        }
    }

    fun functionGemmaReleaseVersion(): String? {
        if (!functionGemmaManifestFile.exists()) return null
        return runCatching {
            parseFunctionGemmaManifest(functionGemmaManifestFile.readText()).version
        }.getOrNull()
    }

    suspend fun ensureFunctionGemmaModel(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isFunctionGemmaModelReady()) return@withContext Result.success(Unit)
        downloadMutex.withLock {
            if (isFunctionGemmaModelReady()) return@withContext Result.success(Unit)
            try {
                functionGemmaDir.mkdirs()
                val manifest = downloadText(FUNCTION_GEMMA_LATEST_URL, "FunctionGemma manifest")
                val release = parseFunctionGemmaManifest(manifest)
                release.requiredAssets.forEach { asset ->
                    downloadFile(
                        urlStr = asset.url,
                        dest = File(functionGemmaDir, asset.name),
                        label = "FunctionGemma/${asset.name}",
                        expectedSha256 = asset.sha256,
                        expectedBytes = asset.bytes,
                    )
                }
                writeAtomically(functionGemmaManifestFile, manifest.toByteArray())
                Timber.i("FunctionGemma release %s ready", release.version)
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "FunctionGemma model download failed")
                Result.failure(e)
            }
        }
    }

    // -- Generic ModelSpec download -----------------------------------------

    fun isModelReady(spec: au.com.shiftyjelly.pocketcasts.voicecontrol.asr.ModelSpec): Boolean {
        val targetDir = File(filesDir, spec.targetDir)
        return spec.files.all { File(targetDir, it.filename).exists() }
    }

    /**
     * Download all files in [spec] into [spec.targetDir] under [filesDir].
     * Existing files are skipped (no SHA256 re-check unless sha256 is non-empty in the spec).
     */
    suspend fun ensureModel(spec: au.com.shiftyjelly.pocketcasts.voicecontrol.asr.ModelSpec): Result<Unit> = withContext(Dispatchers.IO) {
        if (isModelReady(spec)) return@withContext Result.success(Unit)
        val targetDir = File(filesDir, spec.targetDir)
        downloadMutex.withLock {
            if (spec.files.all { File(targetDir, it.filename).exists() }) {
                return@withContext Result.success(Unit)
            }
            try {
                targetDir.mkdirs()
                for (file in spec.files) {
                    val dest = File(targetDir, file.filename)
                    downloadFile(file.url, dest, file.filename, file.sha256)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Model download failed for %s", spec.targetDir)
                Result.failure(e)
            }
        }
    }

    // -- Moonshine model ---------------------------------------------------

    fun isMoonshineModelReady(): Boolean = MOONSHINE_FILES.all { File(moonshineDir, it).exists() }

    suspend fun ensureMoonshineModel(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isMoonshineModelReady()) return@withContext Result.success(Unit)
        downloadMutex.withLock {
            if (isMoonshineModelReady()) return@withContext Result.success(Unit)
            try {
                moonshineDir.mkdirs()
                for (file in MOONSHINE_FILES) {
                    val url = "$MOONSHINE_BASE_URL/$file"
                    val dest = File(moonshineDir, file)
                    downloadFile(url, dest, "Moonshine/$file", expectedSha256 = "")
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Moonshine model download failed")
                Result.failure(e)
            }
        }
    }

    private fun downloadFile(
        urlStr: String,
        dest: File,
        label: String,
        expectedSha256: String,
        expectedBytes: Long = -1,
    ) {
        if (dest.exists()) {
            val sizeMatches = expectedBytes < 0 || dest.length() == expectedBytes
            if (sizeMatches && (expectedSha256.isEmpty() || sha256Matches(dest, expectedSha256))) {
                Timber.i("$label model already downloaded (SHA256 verified)")
                return
            }
            Timber.w("$label model file exists but SHA256 mismatch, re-downloading")
        }
        Timber.i("$label model download starting from $urlStr")
        val tmpFile = File(dest.parentFile, "${dest.name}.tmp")
        var maxRetries = 5
        while (maxRetries > 0) {
            try {
                val offset = if (tmpFile.exists()) tmpFile.length() else 0L
                if (offset > 0) Timber.i("$label resuming from byte $offset")
                val connection = URL(urlStr).openConnection() as HttpURLConnection
                connection.connectTimeout = 60000
                connection.readTimeout = 60000
                if (offset > 0) connection.setRequestProperty("Range", "bytes=$offset-")
                val code = connection.responseCode
                if (code != 200 && code != 206) throw Exception("HTTP $code")
                val append = offset > 0 && code == 206
                if (!append && offset > 0) {
                    Timber.w("$label server ignored range request, restarting download")
                }
                val downloadedBeforeRequest = if (append) offset else 0L
                val totalBytes = connection.contentLengthLong + downloadedBeforeRequest
                Timber.i("$label downloading %d bytes total", totalBytes)
                val lastProgressLog = mutableListOf(0)
                connection.inputStream.use { input ->
                    FileOutputStream(tmpFile, append).use { output ->
                        val buffer = ByteArray(65536)
                        var read: Int
                        var downloaded = downloadedBeforeRequest
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            val pct = if (totalBytes > 0) (downloaded * 100 / totalBytes).toInt() else 0
                            val lastPct = lastProgressLog[0]
                            if (pct - lastPct >= 10) {
                                Timber.i("$label download: %d%% (%d/%d MB)", pct, downloaded / 1_000_000, totalBytes / 1_000_000)
                                lastProgressLog[0] = pct
                            }
                            if (totalBytes > 0) {
                                _downloadState.value = ModelDownloadState.Downloading(
                                    progressPercent = pct,
                                    modelLabel = label,
                                )
                            }
                        }
                    }
                }
                connection.disconnect()
                if (expectedBytes >= 0 && tmpFile.length() != expectedBytes) {
                    throw Exception("$label size mismatch: got ${tmpFile.length()}, expected $expectedBytes")
                }
                if (expectedSha256.isNotEmpty()) verifySha256(tmpFile, expectedSha256, label)
                replaceAtomically(tmpFile, dest)
                Timber.i("$label model download complete")
                maxRetries = 0
            } catch (e: Exception) {
                maxRetries--
                if (maxRetries <= 0) {
                    Timber.e(e, "$label download failed after all retries")
                    throw e
                }
                Timber.w(e, "$label download interrupted, retrying (%d left)", maxRetries)
                Thread.sleep(3000)
            }
        }
    }

    private fun downloadText(urlStr: String, label: String): String {
        var lastError: Exception? = null
        repeat(5) { attempt ->
            try {
                val connection = URL(urlStr).openConnection() as HttpURLConnection
                connection.connectTimeout = 60000
                connection.readTimeout = 60000
                val code = connection.responseCode
                if (code != 200) throw Exception("HTTP $code")
                return connection.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                lastError = e
                if (attempt < 4) {
                    Timber.w(e, "$label download interrupted, retrying (%d left)", 4 - attempt)
                    Thread.sleep(3000)
                }
            }
        }
        throw lastError ?: IllegalStateException("$label download failed")
    }

    private fun writeAtomically(dest: File, bytes: ByteArray) {
        val tmpFile = File(dest.parentFile, "${dest.name}.tmp")
        tmpFile.outputStream().use { it.write(bytes) }
        replaceAtomically(tmpFile, dest)
    }

    private fun replaceAtomically(source: File, dest: File) {
        Os.rename(source.absolutePath, dest.absolutePath)
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(65536)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256Matches(file: File, expected: String): Boolean {
        return try {
            computeSha256(file) == expected
        } catch (e: Exception) {
            false
        }
    }

    private fun verifySha256(file: File, expected: String, label: String) {
        val actual = computeSha256(file)
        if (actual != expected) {
            file.delete()
            Timber.e("$label model hash mismatch — got $actual, expected $expected")
            throw Exception("$label model corrupt (SHA256 mismatch)")
        }
        Timber.i("$label model hash verified")
    }
}

sealed interface ModelDownloadState {
    data object NotStarted : ModelDownloadState
    data class Downloading(val progressPercent: Int, val modelLabel: String = "") : ModelDownloadState
    data object Ready : ModelDownloadState
    data class Failed(val reason: String) : ModelDownloadState
}
