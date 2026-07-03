package au.com.shiftyjelly.pocketcasts.voicecontrol.route

import android.media.AudioDeviceInfo
import au.com.shiftyjelly.pocketcasts.preferences.model.VoiceControlAudioRoutePolicy
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioRoutePolicyRuleTest {
    @Test
    fun `headset policy allows headset with microphone`() {
        val rule = AudioRoutePolicyRule(
            route = MutableStateFlow(AudioRoute.Headset(hasMicrophone = true)),
            policy = MutableStateFlow(VoiceControlAudioRoutePolicy.HeadsetOnly),
        )

        assertEquals(VoiceControlRuleState.Allowed, rule.evaluate())
    }

    @Test
    fun `headset policy blocks speaker`() {
        val rule = AudioRoutePolicyRule(
            route = MutableStateFlow(AudioRoute.Speaker),
            policy = MutableStateFlow(VoiceControlAudioRoutePolicy.HeadsetOnly),
        )

        assertEquals(VoiceControlRuleState.Blocked("audio_route_disallowed"), rule.evaluate())
    }

    @Test
    fun `route classification prefers bluetooth a2dp over builtin speaker`() {
        val route = AndroidAudioRouteMonitor.classifyRoute(
            outputDeviceTypes = listOf(
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            ),
            inputDeviceTypes = emptyList(),
        )

        assertEquals(AudioRoute.BluetoothA2dpOnly, route)
    }

    @Test
    fun `route classification treats wired headphones as no microphone headset route`() {
        val route = AndroidAudioRouteMonitor.classifyRoute(
            outputDeviceTypes = listOf(
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            ),
            inputDeviceTypes = emptyList(),
        )

        assertEquals(AudioRoute.Headset(hasMicrophone = false), route)
    }
}
