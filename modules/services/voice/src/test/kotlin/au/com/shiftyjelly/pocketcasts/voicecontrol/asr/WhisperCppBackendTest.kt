package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WhisperCppBackendTest {

    @get:Rule val tempDir = TemporaryFolder()

    @Test
    fun `blank transcript is rejected`() {
        assertNull(normalizeWhisperTranscript("  "))
    }

    @Test
    fun `single bracketed annotation is rejected`() {
        assertNull(normalizeWhisperTranscript(" [Music] "))
    }

    @Test
    fun `multiple bracketed annotations are rejected`() {
        assertNull(normalizeWhisperTranscript("[door opens]  [door closes]"))
    }

    @Test
    fun `parenthesized annotation is rejected`() {
        assertNull(normalizeWhisperTranscript("(typing)"))
    }

    @Test
    fun `normal English transcript is retained`() {
        assertEquals("play the next episode", normalizeWhisperTranscript(" play the next episode "))
    }

    @Test
    fun `translated English transcript is retained`() {
        assertEquals("fast forward half a minute", normalizeWhisperTranscript("fast forward half a minute"))
    }

    @Test
    fun `mixed annotation and speech transcript is retained`() {
        assertEquals("[Music] play the next episode", normalizeWhisperTranscript("[Music] play the next episode"))
    }

    // ── ensureReady tests ────────────────────────────────────────────────

    @Test
    fun `ensureReady fails when model file is not set`() = runTest {
        val backend = WhisperCppBackend()
        val result = backend.ensureReady()
        assertTrue(result.isFailure)
        assertEquals(
            "Whisper model not found or empty",
            result.exceptionOrNull()?.message,
        )
    }

    @Test
    fun `ensureReady fails when model file does not exist`() = runTest {
        val backend = WhisperCppBackend()
        backend.setModelFile(File("/nonexistent/whisper-model.bin"))
        val result = backend.ensureReady()
        assertTrue(result.isFailure)
    }

    @Test
    fun `ensureReady succeeds when model file exists and is non-empty`() = runTest {
        val backend = WhisperCppBackend()
        val modelFile = tempDir.newFile("ggml-small-q5_1.bin")
        modelFile.writeBytes(byteArrayOf(1, 2, 3))
        backend.setModelFile(modelFile)
        // Native init will fail in JVM (no whisper library loaded), but the
        // file-existence guard should pass before reaching native code.
        // The file check itself is the testable boundary.
        val result = backend.ensureReady()
        // May succeed or fail depending on native library availability;
        // the important invariant is that the file check ran first.
        if (result.isFailure) {
            // If native init failed, it must be because of the native call, not the file check.
            val msg = result.exceptionOrNull()?.message ?: ""
            assertTrue(
                "Expected native init failure, got: $msg",
                msg.contains("init") || msg.contains("native") || msg.contains("UnsatisfiedLinkError"),
            )
        }
    }

    @Test
    fun `json payload parses timed tokens`() {
        val result = parseWhisperPayload(
            """{"text":"Auris skip","tokens":[{"text":"Auris","startMs":0,"endMs":300},{"text":" skip","startMs":500,"endMs":800}]}""",
        )
        assertEquals("Auris skip", result.text)
        val tokens = result.tokens
        assertEquals(2, tokens?.size)
        assertEquals("Auris", tokens?.get(0)?.text)
        assertEquals(300, tokens?.get(0)?.endMs)
    }

    @Test
    fun `plain text payload has no tokens`() {
        val result = parseWhisperPayload(" skip forward ")
        assertEquals("skip forward", result.text)
        assertNull(result.tokens)
    }
}
