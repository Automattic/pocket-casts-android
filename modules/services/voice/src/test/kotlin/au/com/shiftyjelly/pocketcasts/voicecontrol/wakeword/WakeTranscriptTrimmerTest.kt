package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.AsrResult
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.AsrToken
import org.junit.Assert.assertEquals
import org.junit.Test

class WakeTranscriptTrimmerTest {

    private val skipForward = AsrResult(
        text = "Auris skip forward",
        tokens = listOf(
            AsrToken("Auris", 0, 300),
            AsrToken(" skip", 500, 800),
            AsrToken(" forward", 800, 1200),
        ),
    )

    @Test
    fun `wake-positive drops tokens overlapping completion band`() {
        assertEquals(
            "skip forward",
            WakeTranscriptTrimmer.commandText(
                result = skipForward,
                wakePositive = true,
                completionSample = 4000,
                sampleRateHz = 16000,
                utteranceDurationMs = 2000,
            ),
        )
    }

    @Test
    fun `wake-negative leaves transcript even with tokens`() {
        assertEquals(
            "Auris skip forward",
            WakeTranscriptTrimmer.commandText(
                result = skipForward,
                wakePositive = false,
                completionSample = 4000,
                sampleRateHz = 16000,
                utteranceDurationMs = 2000,
            ),
        )
    }

    @Test
    fun `missing tokens leave transcript unstripped`() {
        assertEquals(
            "Auris skip forward",
            WakeTranscriptTrimmer.commandText(
                result = AsrResult(text = "Auris skip forward"),
                wakePositive = true,
                completionSample = 4000,
                sampleRateHz = 16000,
                utteranceDurationMs = 2000,
            ),
        )
    }

    @Test
    fun `all overlapping tokens are wake-only`() {
        assertEquals(
            "",
            WakeTranscriptTrimmer.commandText(
                result = AsrResult(
                    text = "Auris",
                    tokens = listOf(AsrToken("Auris", 0, 400)),
                ),
                wakePositive = true,
                completionSample = 4000,
                sampleRateHz = 16000,
                utteranceDurationMs = 2000,
            ),
        )
    }

    @Test
    fun `zero gap command word starting inside pad is dropped`() {
        // completion at 250ms → bandEnd = 370ms; "skip" starts at 300ms (inside pad).
        assertEquals(
            "forward",
            WakeTranscriptTrimmer.commandText(
                result = AsrResult(
                    text = "Auris skip forward",
                    tokens = listOf(
                        AsrToken("Auris", 0, 250),
                        AsrToken(" skip", 300, 500),
                        AsrToken(" forward", 500, 900),
                    ),
                ),
                wakePositive = true,
                completionSample = 4000,
                sampleRateHz = 16000,
                utteranceDurationMs = 2000,
            ),
        )
    }
}
