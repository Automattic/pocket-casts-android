package au.com.shiftyjelly.pocketcasts.chat.ui

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatLoadingPhaseTest {
    @Test
    fun `phases follow the time since the question was sent`() {
        assertEquals(ChatLoadingPhase.Dots, ChatLoadingPhase.at(Duration.ZERO))
        assertEquals(ChatLoadingPhase.Dots, ChatLoadingPhase.at(1_199.milliseconds))
        assertEquals(ChatLoadingPhase.ReadingTranscript, ChatLoadingPhase.at(1_200.milliseconds))
        assertEquals(ChatLoadingPhase.FindingMoments, ChatLoadingPhase.at(3_500.milliseconds))
        assertEquals(ChatLoadingPhase.WritingAnswer, ChatLoadingPhase.at(6_000.milliseconds))
        assertEquals(ChatLoadingPhase.StillWorking, ChatLoadingPhase.at(12_000.milliseconds))
    }

    @Test
    fun `still working holds until the answer arrives`() {
        assertEquals(ChatLoadingPhase.StillWorking, ChatLoadingPhase.at(5.minutes))
        assertNull(ChatLoadingPhase.StillWorking.next)
    }

    @Test
    fun `a negative elapsed time shows the dots`() {
        assertEquals(ChatLoadingPhase.Dots, ChatLoadingPhase.at((-1).milliseconds))
    }
}
