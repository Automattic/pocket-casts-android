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
class ModelManagerLfmTest {
    @get:Rule val tempDir = TemporaryFolder()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun parseLfmManifest_requiresExactlyGgufClassifierAndLabelMap() {
        val manifest =
            """
            {
              "version": "2026-06-21-143005",
              "source_commit": "abc123",
              "assets": {
                "model.gguf": {
                  "bytes": 5,
                  "sha256": "gguf-sha",
                  "content_type": "application/octet-stream",
                  "url": "https://download.auris.fm/function-call/2026-06-21-143005/model.gguf"
                },
                "classifier.bin": {
                  "bytes": 5,
                  "sha256": "cls-sha",
                  "content_type": "application/octet-stream",
                  "url": "https://download.auris.fm/function-call/2026-06-21-143005/classifier.bin"
                },
                "label_map.json": {
                  "bytes": 5,
                  "sha256": "map-sha",
                  "content_type": "application/json",
                  "url": "https://download.auris.fm/function-call/2026-06-21-143005/label_map.json"
                }
              }
            }
            """.trimIndent()

        val release = parseLfmManifest(manifest)

        assertEquals("2026-06-21-143005", release.version)
        assertEquals(
            listOf("model.gguf", "classifier.bin", "label_map.json"),
            release.requiredAssets.map { it.name },
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseLfmManifest_rejectsFunctionGemmaManifest() {
        parseLfmManifest(
            """
            {
              "version": "2026-06-21-143005",
              "assets": {
                "model.litertlm": {
                  "bytes": 5,
                  "sha256": "model-sha",
                  "url": "https://download.auris.fm/function-call/model.litertlm"
                },
                "model.litertlm.xnnpack_cache_123": {
                  "bytes": 5,
                  "sha256": "cache-sha",
                  "url": "https://download.auris.fm/function-call/model.litertlm.xnnpack_cache_123"
                }
              }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun lfmIsNotReadyWhenDownloadedAssetIsPartial() {
        val modelDir = File(tempDir.root, "function-call").apply { mkdirs() }
        File(modelDir, "model.gguf").writeText("gguf")
        File(modelDir, "classifier.bin").writeText("cls")
        File(modelDir, "label_map.json").writeText("bad")
        File(modelDir, "manifest.json").writeText(
            manifestFor(
                ggufBytes = 4,
                ggufSha = sha256("gguf"),
                classifierBytes = 3,
                classifierSha = sha256("cls"),
                labelMapBytes = 5,
                labelMapSha = sha256("label"),
            ),
        )
        val manager = ModelManager(context).apply { filesDir = tempDir.root }

        assertFalse(manager.isLfmModelReady())
    }

    @Test
    fun lfmIsReadyWhenRequiredDownloadedAssetsMatchManifest() {
        val modelDir = File(tempDir.root, "function-call").apply { mkdirs() }
        File(modelDir, "model.gguf").writeText("gguf")
        File(modelDir, "classifier.bin").writeText("cls")
        File(modelDir, "label_map.json").writeText("label")
        File(modelDir, "manifest.json").writeText(
            manifestFor(
                ggufBytes = 4,
                ggufSha = sha256("gguf"),
                classifierBytes = 3,
                classifierSha = sha256("cls"),
                labelMapBytes = 5,
                labelMapSha = sha256("label"),
            ),
        )
        val manager = ModelManager(context).apply { filesDir = tempDir.root }

        assertTrue(manager.isLfmModelReady())
    }

    @Test
    fun lfmReleaseVersionIsReadFromInstalledManifest() {
        val modelDir = File(tempDir.root, "function-call").apply { mkdirs() }
        File(modelDir, "manifest.json").writeText(
            manifestFor(
                ggufBytes = 4,
                ggufSha = sha256("gguf"),
                classifierBytes = 3,
                classifierSha = sha256("cls"),
                labelMapBytes = 5,
                labelMapSha = sha256("label"),
            ),
        )
        val manager = ModelManager(context).apply { filesDir = tempDir.root }

        assertEquals("2026-06-21-143005", manager.lfmReleaseVersion())
    }

    private fun manifestFor(
        ggufBytes: Int,
        ggufSha: String,
        classifierBytes: Int,
        classifierSha: String,
        labelMapBytes: Int,
        labelMapSha: String,
    ): String =
        """
        {
          "version": "2026-06-21-143005",
          "source_commit": "abc123",
          "assets": {
            "model.gguf": {
              "bytes": $ggufBytes,
              "sha256": "$ggufSha",
              "content_type": "application/octet-stream",
              "url": "https://example.test/model.gguf"
            },
            "classifier.bin": {
              "bytes": $classifierBytes,
              "sha256": "$classifierSha",
              "content_type": "application/octet-stream",
              "url": "https://example.test/classifier.bin"
            },
            "label_map.json": {
              "bytes": $labelMapBytes,
              "sha256": "$labelMapSha",
              "content_type": "application/json",
              "url": "https://example.test/label_map.json"
            }
          }
        }
        """.trimIndent()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
