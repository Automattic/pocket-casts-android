package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

import android.content.res.AssetManager
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WakeWordThresholdLoaderTest {
    @Test
    fun `loads deployment threshold`() {
        val result = load("""{"deployment_threshold":0.73}""")

        assertEquals(0.73f, result.getOrThrow(), 0.0001f)
    }

    @Test
    fun `legacy balanced threshold fails closed`() {
        val result = load("""{"balanced_threshold":0.73}""")

        assertTrue(result.isFailure)
    }

    @Test
    fun `missing manifest fails closed`() {
        val assets = mock<AssetManager>()
        `when`(assets.open("oww/auris_eval.json")).thenThrow(IllegalStateException("missing"))

        assertTrue(WakeWordThresholdLoader.load(assets).isFailure)
    }

    @Test
    fun `out of range deployment threshold fails closed`() {
        assertTrue(load("""{"deployment_threshold":0.0}""").isFailure)
        assertTrue(load("""{"deployment_threshold":1.01}""").isFailure)
    }

    private fun load(manifest: String): Result<Float> {
        val assets = mock<AssetManager>()
        `when`(assets.open("oww/auris_eval.json")).thenReturn(
            ByteArrayInputStream(manifest.toByteArray()),
        )
        return WakeWordThresholdLoader.load(assets)
    }
}
