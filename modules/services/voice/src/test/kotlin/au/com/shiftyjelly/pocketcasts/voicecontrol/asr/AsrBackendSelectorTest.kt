package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

import dagger.Lazy
import java.util.Locale
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class AsrBackendSelectorTest {

    private lateinit var whisperCppBackend: WhisperCppBackend
    private lateinit var senseVoiceBackend: SenseVoiceBackend
    private lateinit var canaryFlashBackend: CanaryFlashBackend
    private lateinit var selector: AsrBackendSelector

    private fun <T> lazyOf(value: T): Lazy<T> = object : Lazy<T> {
        override fun get(): T = value
    }

    private fun buildSelector(locale: Locale = locale("en")): AsrBackendSelector = AsrBackendSelector(
        whisperCppBackend = lazyOf(whisperCppBackend),
        senseVoiceBackend = lazyOf(senseVoiceBackend),
        canaryFlashBackend = lazyOf(canaryFlashBackend),
        currentLocale = { locale },
    )

    private fun locale(tag: String): Locale = Locale.forLanguageTag(tag)

    @Before
    fun setUp() {
        whisperCppBackend = WhisperCppBackend()
        senseVoiceBackend = SenseVoiceBackend()
        canaryFlashBackend = CanaryFlashBackend()
        selector = buildSelector()
    }

    @Test
    fun `english locale selects senseVoiceBackend fast path`() {
        assertSame(senseVoiceBackend, selector.select())
    }

    @Test
    fun `cjk locale selects senseVoiceBackend`() {
        selector = buildSelector(locale("zh"))
        assertSame(senseVoiceBackend, selector.select())
    }

    @Test
    fun `german locale selects canaryFlashBackend`() {
        selector = buildSelector(locale("de"))
        assertSame(canaryFlashBackend, selector.select())
    }

    @Test
    fun `french and spanish locales select canaryFlashBackend`() {
        for (lang in listOf("fr", "es")) {
            selector = buildSelector(locale(lang))
            assertSame(canaryFlashBackend, selector.select())
        }
    }

    @Test
    fun `unsupported locale selects whisperCppBackend fallback`() {
        selector = buildSelector(locale("ar"))
        assertSame(whisperCppBackend, selector.select())
    }

    @Test
    fun `manual override to canary-flash selects canaryFlashBackend`() {
        selector.manualOverride = "canary-flash"
        assertSame(canaryFlashBackend, selector.select())
    }

    @Test
    fun `manual override to whisper-cpp selects whisperCppBackend`() {
        selector.manualOverride = "whisper-cpp"
        assertSame(whisperCppBackend, selector.select())
    }

    @Test
    fun `manual override to whisper-cpp is case-insensitive`() {
        selector.manualOverride = "WHISPER-CPP"
        assertSame(whisperCppBackend, selector.select())
    }

    @Test
    fun `manual override takes priority over matrix selection`() {
        selector.manualOverride = "whisper-cpp"
        assertSame(whisperCppBackend, selector.select())
    }

    @Test
    fun `manual override to sensevoice selects senseVoiceBackend`() {
        selector.manualOverride = "sensevoice"
        assertSame(senseVoiceBackend, selector.select())
    }

    @Test
    fun `manual override to unknown backend throws error`() {
        selector.manualOverride = "unknown"
        assertThrows("Unknown backend override", IllegalStateException::class.java) {
            selector.select()
        }
    }
}
