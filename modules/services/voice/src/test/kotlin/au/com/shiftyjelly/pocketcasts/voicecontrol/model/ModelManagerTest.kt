package au.com.shiftyjelly.pocketcasts.voicecontrol.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModelManagerTest {
    @get:Rule val tempDir = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `reports not ready when moonshine model not downloaded`() {
        val manager = ModelManager(context).apply {
            filesDir = tempDir.root
        }
        assertFalse(manager.isMoonshineModelReady())
    }

    @Test
    fun `reports ready when all moonshine model files exist`() {
        val moonshineDir = File(tempDir.root, "moonshine-model").apply { mkdirs() }
        // Create all expected Moonshine model files
        val files = listOf(
            "adapter.ort",
            "cross_kv.ort",
            "decoder_kv.ort",
            "decoder_kv_with_attention.ort",
            "encoder.ort",
            "frontend.ort",
            "streaming_config.json",
            "tokenizer.bin",
        )
        for (file in files) {
            File(moonshineDir, file).writeText("fake model")
        }

        val manager = ModelManager(context).apply {
            filesDir = tempDir.root
        }
        assertTrue(manager.isMoonshineModelReady())
    }

    @Test
    fun `parses required FunctionGemma assets from release manifest`() {
        val manifest =
            """
            {
              "version": "2026-06-21-143005",
              "source_commit": "abc123",
              "assets": {
                "model.litertlm": {
                  "bytes": 5,
                  "sha256": "model-sha",
                  "content_type": "application/octet-stream",
                  "url": "https://download.auris.fm/function-call/2026-06-21-143005/model.litertlm"
                },
                "model.litertlm.xnnpack_cache_123": {
                  "bytes": 5,
                  "sha256": "cache-sha",
                  "content_type": "application/octet-stream",
                  "url": "https://download.auris.fm/function-call/2026-06-21-143005/model.litertlm.xnnpack_cache_123"
                },
                "tools.json": {
                  "bytes": 2,
                  "sha256": "tools-sha",
                  "content_type": "application/json",
                  "url": "https://download.auris.fm/function-call/2026-06-21-143005/tools.json"
                }
              }
            }
            """.trimIndent()

        val release = parseFunctionGemmaManifest(manifest)

        assertEquals("2026-06-21-143005", release.version)
        assertEquals(
            listOf("model.litertlm", "model.litertlm.xnnpack_cache_123"),
            release.requiredAssets.map { it.name },
        )
    }

    @Test
    fun `FunctionGemma is not ready when a downloaded asset is partial`() {
        val modelDir = File(tempDir.root, "functiongemma-model").apply { mkdirs() }
        File(modelDir, "model.litertlm").writeText("model")
        File(modelDir, "model.litertlm.xnnpack_cache_123").writeText("bad")
        File(modelDir, "manifest.json").writeText(
            manifestFor(
                modelBytes = 5,
                modelSha = sha256("model"),
                cacheBytes = 5,
                cacheSha = sha256("cache"),
            ),
        )
        val manager = ModelManager(context).apply {
            filesDir = tempDir.root
        }

        assertFalse(manager.isFunctionGemmaModelReady())
    }

    @Test
    fun `FunctionGemma is ready when required downloaded assets match manifest`() {
        val modelDir = File(tempDir.root, "functiongemma-model").apply { mkdirs() }
        File(modelDir, "model.litertlm").writeText("model")
        File(modelDir, "model.litertlm.xnnpack_cache_123").writeText("cache")
        File(modelDir, "manifest.json").writeText(
            manifestFor(
                modelBytes = 5,
                modelSha = sha256("model"),
                cacheBytes = 5,
                cacheSha = sha256("cache"),
            ),
        )
        val manager = ModelManager(context).apply {
            filesDir = tempDir.root
        }

        assertTrue(manager.isFunctionGemmaModelReady())
    }

    @Test
    fun `FunctionGemma release version is read from installed manifest`() {
        val modelDir = File(tempDir.root, "functiongemma-model").apply { mkdirs() }
        File(modelDir, "manifest.json").writeText(
            manifestFor(
                modelBytes = 5,
                modelSha = sha256("model"),
                cacheBytes = 5,
                cacheSha = sha256("cache"),
            ),
        )
        val manager = ModelManager(context).apply {
            filesDir = tempDir.root
        }

        assertEquals("2026-06-21-143005", manager.functionGemmaReleaseVersion())
    }

    @Test
    fun `FunctionGemma release version is null for invalid manifest`() {
        val modelDir = File(tempDir.root, "functiongemma-model").apply { mkdirs() }
        File(modelDir, "manifest.json").writeText("not-json")
        val manager = ModelManager(context).apply {
            filesDir = tempDir.root
        }

        assertEquals(null, manager.functionGemmaReleaseVersion())
    }

    private fun manifestFor(
        modelBytes: Int,
        modelSha: String,
        cacheBytes: Int,
        cacheSha: String,
    ): String =
        """
        {
          "version": "2026-06-21-143005",
          "source_commit": "abc123",
          "assets": {
            "model.litertlm": {
              "bytes": $modelBytes,
              "sha256": "$modelSha",
              "content_type": "application/octet-stream",
              "url": "https://example.test/model.litertlm"
            },
            "model.litertlm.xnnpack_cache_123": {
              "bytes": $cacheBytes,
              "sha256": "$cacheSha",
              "content_type": "application/octet-stream",
              "url": "https://example.test/model.litertlm.xnnpack_cache_123"
            }
          }
        }
        """.trimIndent()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
