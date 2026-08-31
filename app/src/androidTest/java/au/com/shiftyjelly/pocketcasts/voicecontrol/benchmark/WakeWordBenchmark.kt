package au.com.shiftyjelly.pocketcasts.voicecontrol.benchmark

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.OpenWakeWordDetector
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device wake word benchmark harness — see core repo
 * docs/specs/wakeword-device-benchmark.md. Driven by
 * training/wakeword/device_benchmark.py, which stages benchmark_manifest.json,
 * its SHA-256 sidecar, and clips into filesDir/wakeword_bench/ before pulling
 * device_result.json back.
 */
@RunWith(AndroidJUnit4::class)
class WakeWordBenchmark {

    private lateinit var context: Context
    private lateinit var detector: OpenWakeWordDetector
    private lateinit var benchDir: File
    private lateinit var manifestSha256: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        benchDir = File(context.filesDir, "wakeword_bench")
        val manifestFile = File(benchDir, "benchmark_manifest.json")
        val sidecarFile = File(benchDir, "benchmark_manifest.sha256")
        assertTrue(
            "benchmark manifest and sidecar not staged — run device_benchmark.py",
            manifestFile.isFile && sidecarFile.isFile,
        )
        manifestSha256 = sidecarFile.readText().trim()
        require(manifestSha256.matches(Regex("[0-9a-f]{64}"))) {
            "invalid benchmark_manifest.sha256"
        }
        require(sha256(manifestFile.readBytes()) == manifestSha256) {
            "benchmark manifest SHA-256 mismatch"
        }
        detector = OpenWakeWordDetector(context)
        assertTrue("wake word detector failed to initialize", detector.isReady)
    }

    @Test
    fun runBenchmark() {
        val manifest = JSONObject(File(benchDir, "benchmark_manifest.json").readText())
        require(manifest.getInt("version") == 3) { "unsupported benchmark manifest version" }
        val clips = manifest.getJSONArray("clips")
        Log.i(TAG, "Scoring ${clips.length()} clips (threshold=${detector.detectionThreshold})")

        val results = JSONArray()
        for (i in 0 until clips.length()) {
            val entry = clips.getJSONObject(i)
            val id = entry.getString("id")
            val clipFile = File(benchDir, id)
            require(sha256OfFile(clipFile) == entry.getString("audio_sha256")) {
                "staged clip SHA-256 mismatch: $id"
            }
            val result = JSONObject()
                .put("id", id)
                .put("role", entry.getString("role"))
                .put("slice", entry.getString("slice"))
            val startNs = SystemClock.elapsedRealtimeNanos()
            try {
                val samples = readWavMono16k(clipFile)
                val detection = runBlocking {
                    detector.detect(
                        samples,
                        sampleRateHz = 16000,
                        speechOnsetSample = entry.getInt("speech_onset_sample"),
                    )
                }
                val detectMs = (SystemClock.elapsedRealtimeNanos() - startNs) / 1e6
                result.put("detect_ms", detectMs)
                if (detection.error) {
                    result.put("status", "error").put("error_code", "detector_error")
                } else {
                    result.put("status", if (detection.detected) "detected" else "not_detected")
                        .put("score", detection.confidence.toDouble())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Clip $id failed", e)
                result.put(
                    "detect_ms",
                    (SystemClock.elapsedRealtimeNanos() - startNs) / 1e6,
                ).put("status", "error")
                    .put("error_code", "harness_exception")
            }
            results.put(result)
            if ((i + 1) % 200 == 0) Log.i(TAG, "Scored ${i + 1}/${clips.length()}")
        }

        val output = JSONObject()
            .put("version", 2)
            .put(
                "device",
                JSONObject()
                    .put("platform", "android")
                    .put("model", Build.MODEL)
                    .put("os_version", Build.VERSION.RELEASE)
                    .put("architecture", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
                    .put(
                        "app_version",
                        context.packageManager.getPackageInfo(context.packageName, 0)
                            .versionName ?: "unknown",
                    ),
            )
            .put("threshold", detector.detectionThreshold.toDouble())
            .put(
                "asset_hashes",
                JSONObject().apply {
                    for (asset in OWW_ASSETS) put(asset, sha256OfAsset("oww/$asset"))
                },
            )
            .put("dataset_fingerprint", manifest.getString("dataset_fingerprint"))
            .put("benchmark_manifest_sha256", manifestSha256)
            .put("results", results)

        File(benchDir, "device_result.json").writeText(output.toString())
        Log.i(TAG, "Wrote ${results.length()} results to $benchDir/device_result.json")
    }

    private fun sha256OfAsset(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        context.assets.open(path).use { input ->
            val buffer = ByteArray(65536)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256OfFile(file: File): String = sha256(file.readBytes())

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    /** Minimal RIFF/WAVE reader for the benchmark corpus: 16-bit PCM mono 16 kHz. */
    private fun readWavMono16k(file: File): FloatArray {
        val bytes = file.readBytes()
        require(bytes.size > 44 && String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WAVE") {
            "${file.name}: not a RIFF/WAVE file"
        }
        var pos = 12
        var dataOffset = -1
        var dataSize = 0
        var sampleRate = 0
        var channels = 0
        var bitsPerSample = 0
        while (pos + 8 <= bytes.size) {
            val chunkId = String(bytes, pos, 4)
            val chunkSize = readLeInt(bytes, pos + 4)
            when (chunkId) {
                "fmt " -> {
                    channels = readLeShort(bytes, pos + 10)
                    sampleRate = readLeInt(bytes, pos + 12)
                    bitsPerSample = readLeShort(bytes, pos + 22)
                }

                "data" -> {
                    dataOffset = pos + 8
                    dataSize = chunkSize
                }
            }
            pos += 8 + chunkSize + (chunkSize and 1)
        }
        require(dataOffset > 0) { "${file.name}: no data chunk" }
        require(channels == 1 && sampleRate == 16000 && bitsPerSample == 16) {
            "${file.name}: expected 16-bit mono 16kHz, got ${bitsPerSample}bit ${channels}ch ${sampleRate}Hz"
        }
        val sampleCount = dataSize / 2
        val samples = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            val lo = bytes[dataOffset + 2 * i].toInt() and 0xFF
            val hi = bytes[dataOffset + 2 * i + 1].toInt()
            samples[i] = ((hi shl 8) or lo).toShort().toInt() / 32768f
        }
        return samples
    }

    private fun readLeInt(b: ByteArray, o: Int): Int = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or
        ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)

    private fun readLeShort(b: ByteArray, o: Int): Int = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    companion object {
        private const val TAG = "WakeWordBenchmark"
        private val OWW_ASSETS = listOf(
            "melspectrogram.onnx",
            "embedding_model.onnx",
            "auris.onnx",
            "auris_eval.json",
        )
    }
}
