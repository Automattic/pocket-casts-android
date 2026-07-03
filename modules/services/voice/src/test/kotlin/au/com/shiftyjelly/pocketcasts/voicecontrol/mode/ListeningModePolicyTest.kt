@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.voicecontrol.mode

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlGate
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlGateState
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningModePolicyTest {

    // --- New-priority tests ---

    @Test
    fun `grace period active resolves to Continuous regardless of exposure and foreground`() {
        // Grace period overrides everything — even Exposed + background
        val result = resolve(
            gateState = allowedGateState,
            micExposure = MicExposure.Exposed,
            isForeground = false,
            isPlaybackActive = false,
            isAttended = false,
            isGracePeriodActive = true,
            isPlaybackRecent = false,
            wakeWordReady = true,
        )
        assertEquals(ListeningMode.Continuous, result)
    }

    @Test
    fun `foreground plus attended resolves to Continuous`() {
        val result = resolve(
            gateState = allowedGateState,
            micExposure = MicExposure.Exposed,
            isForeground = true,
            isPlaybackActive = false,
            isAttended = true,
            isGracePeriodActive = false,
            isPlaybackRecent = false,
            wakeWordReady = true,
        )
        assertEquals(ListeningMode.Continuous, result)
    }

    @Test
    fun `foreground plus unattended resolves to WakeWord`() {
        val result = resolve(
            gateState = allowedGateState,
            micExposure = MicExposure.Exposed,
            isForeground = true,
            isPlaybackActive = false,
            isAttended = false,
            isGracePeriodActive = false,
            isPlaybackRecent = false,
            wakeWordReady = true,
        )
        assertEquals(ListeningMode.WakeWord, result)
    }

    @Test
    fun `background Isolated playback active plus recent resolves to Continuous`() {
        val result = resolve(
            gateState = allowedGateState,
            micExposure = MicExposure.Isolated,
            isForeground = false,
            isPlaybackActive = true,
            isAttended = false,
            isGracePeriodActive = false,
            isPlaybackRecent = true,
            wakeWordReady = true,
        )
        assertEquals(ListeningMode.Continuous, result)
    }

    @Test
    fun `background Isolated playback active not recent resolves to WakeWord`() {
        val result = resolve(
            gateState = allowedGateState,
            micExposure = MicExposure.Isolated,
            isForeground = false,
            isPlaybackActive = true,
            isAttended = false,
            isGracePeriodActive = false,
            isPlaybackRecent = false,
            wakeWordReady = true,
        )
        assertEquals(ListeningMode.WakeWord, result)
    }

    @Test
    fun `background Exposed playback active resolves to WakeWord`() {
        val result = resolve(
            gateState = allowedGateState,
            micExposure = MicExposure.Exposed,
            isForeground = false,
            isPlaybackActive = true,
            isAttended = false,
            isGracePeriodActive = false,
            isPlaybackRecent = false,
            wakeWordReady = true,
        )
        assertEquals(ListeningMode.WakeWord, result)
    }

    @Test
    fun `background Exposed wake word not ready resolves to Off`() {
        val result = resolve(
            gateState = allowedGateState,
            micExposure = MicExposure.Exposed,
            isForeground = false,
            isPlaybackActive = true,
            isAttended = false,
            isGracePeriodActive = false,
            isPlaybackRecent = false,
            wakeWordReady = false,
        )
        assertEquals(ListeningMode.Off, result)
    }

    @Test
    fun `gate blocked resolves to Off`() = runTest {
        val rule = FakeRule("test", VoiceControlRuleGroup.Setup, VoiceControlRuleState.Blocked("blocked"))
        val gate = VoiceControlGate(listOf(rule), backgroundScope)

        gate.state.test {
            val result = resolve(
                gateState = awaitItem(),
                micExposure = MicExposure.Exposed,
                isForeground = true,
                isPlaybackActive = true,
                isAttended = true,
                isGracePeriodActive = true,
                isPlaybackRecent = true,
                wakeWordReady = true,
            )
            assertEquals(ListeningMode.Off, result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `NoMic resolves to Off`() {
        val result = resolve(
            gateState = allowedGateState,
            micExposure = MicExposure.NoMic,
            isForeground = true,
            isPlaybackActive = true,
            isAttended = true,
            isGracePeriodActive = false,
            isPlaybackRecent = true,
            wakeWordReady = true,
        )
        assertEquals(ListeningMode.Off, result)
    }

    @Test
    fun `gate blocked overrides grace period`() {
        // Gate safety is absolute — even grace period cannot override it
        val result = resolve(
            gateState = VoiceControlGateState(allowed = false, rules = emptyMap()),
            micExposure = MicExposure.Exposed,
            isForeground = true,
            isPlaybackActive = true,
            isAttended = true,
            isGracePeriodActive = true,
            isPlaybackRecent = true,
            wakeWordReady = true,
        )
        assertEquals(ListeningMode.Off, result)
    }

    // --- Still-valid tests from the old rule set ---

    @Test
    fun `background with no active playback resolves to Off`() {
        val result = resolve(
            gateState = allowedGateState,
            micExposure = MicExposure.Exposed,
            isForeground = false,
            isPlaybackActive = false,
            isAttended = false,
            isGracePeriodActive = false,
            isPlaybackRecent = false,
            wakeWordReady = true,
        )
        assertEquals(ListeningMode.Off, result)
    }

    // --- Helpers ---

    private val allowedGateState = VoiceControlGateState(
        allowed = true,
        rules = emptyMap(),
    )

    private class FakeRule(
        override val id: String,
        override val group: VoiceControlRuleGroup,
        initialState: VoiceControlRuleState,
    ) : VoiceControlRule {
        override val state = MutableStateFlow(initialState)
    }
}
