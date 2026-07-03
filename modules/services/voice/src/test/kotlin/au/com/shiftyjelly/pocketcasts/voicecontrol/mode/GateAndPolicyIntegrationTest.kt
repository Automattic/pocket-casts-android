@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.voicecontrol.mode

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.model.VoiceControlAudioRoutePolicy
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlGate
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlGateState
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.ModelsReadyCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AudioRoute
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AudioRoutePolicyRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests covering the integration of gate rules with real runtime scenarios.
 *
 * The static [resolve] function is covered by [ListeningModePolicyTest].
 * These tests verify that the gate and its rules behave correctly for the
 * combinations that occur when the app transitions between foreground and
 * background with various audio routes and playback states.
 */
class GateAndPolicyIntegrationTest {

    // --- AudioRoutePolicyRule (the root cause of the bug) ---

    @Test
    fun `SpeakerExperimental policy allows Speaker route`() = runTest {
        val route = MutableStateFlow<AudioRoute>(AudioRoute.Speaker)
        val policy = MutableStateFlow(VoiceControlAudioRoutePolicy.SpeakerExperimental)
        val rule = AudioRoutePolicyRule(route, policy, backgroundScope)

        rule.state.test {
            assertEquals(VoiceControlRuleState.Allowed, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `HeadsetOnly policy blocks Speaker route`() = runTest {
        val route = MutableStateFlow<AudioRoute>(AudioRoute.Speaker)
        val policy = MutableStateFlow(VoiceControlAudioRoutePolicy.HeadsetOnly)
        val rule = AudioRoutePolicyRule(route, policy, backgroundScope)

        rule.state.test {
            assertEquals(VoiceControlRuleState.Blocked("audio_route_disallowed"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SpeakerExperimental policy allows Headset with mic`() = runTest {
        val route = MutableStateFlow<AudioRoute>(AudioRoute.Headset(hasMicrophone = true))
        val policy = MutableStateFlow(VoiceControlAudioRoutePolicy.SpeakerExperimental)
        val rule = AudioRoutePolicyRule(route, policy, backgroundScope)

        rule.state.test {
            assertEquals(VoiceControlRuleState.Allowed, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `policy change from HeadsetOnly to SpeakerExperimental unblocks Speaker`() = runTest {
        val route = MutableStateFlow(AudioRoute.Speaker)
        val policy = MutableStateFlow(VoiceControlAudioRoutePolicy.HeadsetOnly)
        val rule = AudioRoutePolicyRule(route, policy, backgroundScope)

        rule.state.test {
            // Starts blocked under HeadsetOnly
            assertEquals(VoiceControlRuleState.Blocked("audio_route_disallowed"), awaitItem())

            // Switch policy to SpeakerExperimental
            policy.value = VoiceControlAudioRoutePolicy.SpeakerExperimental

            // Should now be allowed
            assertEquals(VoiceControlRuleState.Allowed, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Gate integration with all rule groups ---

    @Test
    fun `gate passes for foreground Speaker playback under SpeakerExperimental`() = runTest {
        val gate = makeGate(
            scope = backgroundScope,
            audioRoute = AudioRoute.Speaker,
            audioPolicy = VoiceControlAudioRoutePolicy.SpeakerExperimental,
            appInForeground = true,
            playbackActive = true,
        )

        gate.state.test {
            val state = awaitItem()
            val failures = state.rules.filter { it.value !is VoiceControlRuleState.Allowed }
            assertTrue("Gate blocked by: $failures", state.allowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `gate blocked by HeadsetOnly policy on Speaker route even in foreground`() = runTest {
        val gate = makeGate(
            scope = backgroundScope,
            audioRoute = AudioRoute.Speaker,
            audioPolicy = VoiceControlAudioRoutePolicy.HeadsetOnly,
            appInForeground = true,
            playbackActive = true,
        )

        gate.state.test {
            val state = awaitItem()
            assertFalse("Gate should be blocked with HeadsetOnly+Speaker", state.allowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `gate passes for background Speaker playback under SpeakerExperimental`() = runTest {
        val gate = makeGate(
            scope = backgroundScope,
            audioRoute = AudioRoute.Speaker,
            audioPolicy = VoiceControlAudioRoutePolicy.SpeakerExperimental,
            appInForeground = false,
            playbackActive = true,
        )

        gate.state.test {
            val state = awaitItem()
            val failures = state.rules.filter { it.value !is VoiceControlRuleState.Allowed }
            assertTrue("Gate blocked by: $failures", state.allowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `gate blocked when neither foreground nor playback active`() = runTest {
        val gate = makeGate(
            scope = backgroundScope,
            audioRoute = AudioRoute.Speaker,
            audioPolicy = VoiceControlAudioRoutePolicy.SpeakerExperimental,
            appInForeground = false,
            playbackActive = false,
        )

        gate.state.test {
            val state = awaitItem()
            assertFalse("Gate should be blocked when context group has nothing", state.allowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- resolve() edge cases updated for new priority rules ---

    @Test
    fun `resolve foreground attended with Exposed playback produces Continuous`() {
        // Simulates: app in foreground, user attended, playing on speaker → Continuous
        val result = resolve(
            gateState = allowedGateState,
            micExposure = MicExposure.Exposed,
            isForeground = true,
            isPlaybackActive = true,
            isAttended = true,
            isGracePeriodActive = false,
            isPlaybackRecent = true,
            wakeWordReady = true,
        )
        assertEquals(ListeningMode.Continuous, result)
    }

    @Test
    fun `resolve foreground unattended with Exposed playback produces WakeWord`() {
        // Simulates: app in foreground, user unattended, playing on speaker → WakeWord
        val result = resolve(
            gateState = allowedGateState,
            micExposure = MicExposure.Exposed,
            isForeground = true,
            isPlaybackActive = true,
            isAttended = false,
            isGracePeriodActive = false,
            isPlaybackRecent = true,
            wakeWordReady = true,
        )
        assertEquals(ListeningMode.WakeWord, result)
    }

    @Test
    fun `resolve background Exposed playback produces WakeWord`() {
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
    fun `resolve WakeWord requires detector ready`() {
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
    fun `resolution flips from Continuous to WakeWord when attended expires`() {
        // Foreground + attended → Continuous
        assertEquals(
            ListeningMode.Continuous,
            resolve(
                gateState = allowedGateState,
                micExposure = MicExposure.Exposed,
                isForeground = true,
                isPlaybackActive = true,
                isAttended = true,
                isGracePeriodActive = false,
                isPlaybackRecent = true,
                wakeWordReady = true,
            ),
        )
        // Foreground + unattended → WakeWord
        assertEquals(
            ListeningMode.WakeWord,
            resolve(
                gateState = allowedGateState,
                micExposure = MicExposure.Exposed,
                isForeground = true,
                isPlaybackActive = true,
                isAttended = false,
                isGracePeriodActive = false,
                isPlaybackRecent = true,
                wakeWordReady = true,
            ),
        )
    }

    // --- Helpers ---

    private val allowedGateState = VoiceControlGateState(
        allowed = true,
        rules = emptyMap(),
    )

    /**
     * Build a [VoiceControlGate] with the full set of production-like rules,
     * parameterized by the values that vary in real use.
     */
    private fun makeGate(
        audioRoute: AudioRoute,
        audioPolicy: VoiceControlAudioRoutePolicy,
        appInForeground: Boolean,
        playbackActive: Boolean,
        scope: kotlinx.coroutines.CoroutineScope,
    ): VoiceControlGate {
        val rules: List<VoiceControlRule> = listOf(
            // Setup group (ALL must pass)
            fakeRule("voice_control_user_enabled", VoiceControlRuleGroup.Setup, allowed = true),
            fakeRule("device_supported", VoiceControlRuleGroup.Setup, allowed = true),
            ModelsReadyCondition(isReady = true),
            // Conflicts group (ALL must pass)
            AudioRoutePolicyRule(
                route = MutableStateFlow(audioRoute),
                policy = MutableStateFlow(audioPolicy),
                scope = scope,
            ),
            fakeRule("not_on_call", VoiceControlRuleGroup.Conflicts, allowed = true),
            fakeRule("not_casting", VoiceControlRuleGroup.Conflicts, allowed = true),
            fakeRule("battery_ok", VoiceControlRuleGroup.Conflicts, allowed = true),
            // Context group (ANY must pass)
            fakeRule("app_in_foreground", VoiceControlRuleGroup.Context, allowed = appInForeground),
            fakeRule("playback_context", VoiceControlRuleGroup.Context, allowed = playbackActive),
        )
        return VoiceControlGate(rules, scope)
    }

    private fun fakeRule(id: String, group: VoiceControlRuleGroup, allowed: Boolean): VoiceControlRule {
        return object : VoiceControlRule {
            override val id = id
            override val group = group
            override val state = MutableStateFlow(
                if (allowed) {
                    VoiceControlRuleState.Allowed
                } else {
                    VoiceControlRuleState.Blocked("test_blocked")
                },
            )
        }
    }
}
