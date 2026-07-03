package au.com.shiftyjelly.pocketcasts.voicecontrol.route

import org.junit.Assert.assertEquals
import org.junit.Test

class MicExposureTest {
    @Test
    fun `headset with microphone maps to Isolated`() {
        assertEquals(MicExposure.Isolated, AudioRoute.Headset(hasMicrophone = true).toMicExposure())
    }

    @Test
    fun `headset without microphone maps to NoMic`() {
        assertEquals(MicExposure.NoMic, AudioRoute.Headset(hasMicrophone = false).toMicExposure())
    }

    @Test
    fun `speaker maps to Exposed`() {
        assertEquals(MicExposure.Exposed, AudioRoute.Speaker.toMicExposure())
    }

    @Test
    fun `bluetooth A2DP only maps to Exposed`() {
        assertEquals(MicExposure.Exposed, AudioRoute.BluetoothA2dpOnly.toMicExposure())
    }

    @Test
    fun `unknown maps to Exposed`() {
        assertEquals(MicExposure.Exposed, AudioRoute.Unknown.toMicExposure())
    }
}
