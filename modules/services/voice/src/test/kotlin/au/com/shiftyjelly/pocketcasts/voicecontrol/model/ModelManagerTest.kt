package au.com.shiftyjelly.pocketcasts.voicecontrol.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
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
}
