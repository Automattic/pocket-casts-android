package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

import dagger.Lazy
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class AsrBackendSelectorTest {

    private lateinit var deviceProbe: DeviceProbe
    private lateinit var whisperCppBackend: WhisperCppBackend
    private lateinit var senseVoiceBackend: SenseVoiceBackend
    private lateinit var selector: AsrBackendSelector

    @Before
    fun setUp() {
        deviceProbe = DeviceProbe(hardware = "", socManufacturer = "", sdkInt = 30)
        whisperCppBackend = WhisperCppBackend()
        senseVoiceBackend = SenseVoiceBackend()
        selector = AsrBackendSelector(
            deviceProbe = deviceProbe,
            whisperCppBackend = object : Lazy<WhisperCppBackend> {
                override fun get(): WhisperCppBackend = whisperCppBackend
            },
            senseVoiceBackend = object : Lazy<SenseVoiceBackend> {
                override fun get(): SenseVoiceBackend = senseVoiceBackend
            },
        )
    }

    @Test
    fun `select returns whisperCppBackend by default for non-CJK locale`() {
        val backend = selector.select()
        assertSame(whisperCppBackend, backend)
    }

    @Test
    fun `manual override to whisper-cpp selects whisperCppBackend`() {
        selector.manualOverride = "whisper-cpp"

        val backend = selector.select()
        assertSame(whisperCppBackend, backend)
    }

    @Test
    fun `manual override to whisper-cpp is case-insensitive`() {
        selector.manualOverride = "WHISPER-CPP"

        val backend = selector.select()
        assertSame(whisperCppBackend, backend)
    }

    @Test
    fun `manual override takes priority over matrix selection`() {
        selector.manualOverride = "whisper-cpp"

        val backend = selector.select()
        assertSame(whisperCppBackend, backend)
    }

    @Test
    fun `manual override to sensevoice selects senseVoiceBackend`() {
        selector.manualOverride = "sensevoice"

        val backend = selector.select()
        assertSame(senseVoiceBackend, backend)
    }

    @Test
    fun `manual override to npu throws not yet implemented error`() {
        selector.manualOverride = "npu"

        assertThrows(IllegalStateException::class.java) {
            selector.select()
        }
    }

    @Test
    fun `manual override to unknown backend throws error`() {
        selector.manualOverride = "unknown"

        assertThrows(IllegalStateException::class.java) {
            selector.select()
        }
    }
}
