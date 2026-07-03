package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.EarconId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VoiceResponseTest {
    @Test
    fun `Earcon response carries EarconId`() {
        val response = VoiceResponse.Earcon(EarconId.SUCCESS)
        assertEquals(EarconId.SUCCESS, response.id)
    }

    @Test
    fun `Spoken response carries text`() {
        val response = VoiceResponse.Spoken("1.5x speed")
        assertEquals("1.5x speed", response.text)
    }

    @Test
    fun `Silent is a singleton`() {
        assertSame(VoiceResponse.Silent, VoiceResponse.Silent)
    }

    @Test
    fun `Combined response carries earcon and spoken text`() {
        val response = VoiceResponse.Combined(EarconId.SUCCESS, "1.5x speed")
        assertEquals(EarconId.SUCCESS, response.earcon)
        assertEquals("1.5x speed", response.spokenText)
    }
}
