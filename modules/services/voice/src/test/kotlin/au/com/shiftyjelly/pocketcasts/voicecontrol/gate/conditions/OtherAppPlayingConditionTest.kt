@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions

import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtherAppPlayingConditionTest {

    @Test
    fun `initial state is unknown`() {
        val condition = OtherAppPlayingCondition()
        val state = condition.state.value
        assertTrue(state is VoiceControlRuleState.Unknown)
        assertEquals("initializing", (state as VoiceControlRuleState.Unknown).reason)
    }

    @Test
    fun `has correct id and group`() {
        val condition = OtherAppPlayingCondition()
        assertEquals(VoiceControlRuleGroup.Conflicts, condition.group)
        assertEquals("other_app_playing", condition.id)
    }

    @Test
    fun `blocks when other app is playing after debounce`() = runTest {
        val condition = OtherAppPlayingCondition(debounceMs = 500, scope = backgroundScope)
        assertEquals(VoiceControlRuleGroup.Conflicts, condition.group)
        assertEquals("other_app_playing", condition.id)

        condition.update(otherAppPlaying = true)
        // Debounce is 500ms; wait past it
        delay(600)

        val state = condition.state.value
        assertTrue(state is VoiceControlRuleState.Blocked)
        assertEquals("other_app_playing", (state as VoiceControlRuleState.Blocked).reason)
    }

    @Test
    fun `allowed when no other app is playing`() = runTest {
        val condition = OtherAppPlayingCondition(scope = backgroundScope)

        condition.update(otherAppPlaying = false)

        assertTrue(condition.state.value is VoiceControlRuleState.Allowed)
    }

    @Test
    fun `debounces transient flickers`() = runTest {
        val condition = OtherAppPlayingCondition(debounceMs = 500, scope = backgroundScope)

        // A quick true->false->true flicker within <500ms should not emit Blocked
        condition.update(otherAppPlaying = true)
        delay(200)
        condition.update(otherAppPlaying = false)
        delay(200)
        condition.update(otherAppPlaying = true)
        delay(200)

        // Total elapsed: 600ms, but latest update(true) was only 200ms ago
        // Debounce (500ms) should NOT have fired yet
        assertFalse("State should not be Blocked during debounce window", condition.state.value is VoiceControlRuleState.Blocked)

        // Wait for the debounce to fire
        delay(300)

        // After full debounce the condition should stabilise to Blocked
        val state = condition.state.value
        assertTrue(state is VoiceControlRuleState.Blocked)
        assertEquals("other_app_playing", (state as VoiceControlRuleState.Blocked).reason)
    }

    @Test
    fun `evaluate returns blocked when other app is playing`() {
        val condition = OtherAppPlayingCondition()
        val state = condition.evaluate(otherAppPlaying = true)
        assertTrue(state is VoiceControlRuleState.Blocked)
        assertEquals("other_app_playing", (state as VoiceControlRuleState.Blocked).reason)
    }

    @Test
    fun `evaluate returns allowed when no other app is playing`() {
        val condition = OtherAppPlayingCondition()
        val state = condition.evaluate(otherAppPlaying = false)
        assertTrue(state is VoiceControlRuleState.Allowed)
    }

    @Test
    fun `evaluate with null AudioManager returns allowed (safe default)`() {
        val condition = OtherAppPlayingCondition(audioManager = null)
        val state = condition.evaluate()
        assertTrue(state is VoiceControlRuleState.Allowed)
    }

    @Test
    fun `host app playback is not treated as other app playing`() {
        // When the host is playing audio, the condition should allow
        // (per spec: Auris must own the audio output).
        // The evaluate(Boolean) path takes a pre-computed boolean —
        // passing false means "no other app" → Allowed.
        val condition = OtherAppPlayingCondition()
        val state = condition.evaluate(otherAppPlaying = false)
        assertTrue(state is VoiceControlRuleState.Allowed)
    }

    @Test
    fun `host own audio with active context is not other app`() {
        // Fix for the false positive: audio is active (host is audibly playing) but the
        // host holds a playback context, so the session belongs to the host, not another app.
        assertFalse(otherAppPlaying(isMusicActive = true, hostHasActiveContext = true))
    }

    @Test
    fun `other app audio when host has no context is other app`() {
        assertTrue(otherAppPlaying(isMusicActive = true, hostHasActiveContext = false))
    }

    @Test
    fun `no active audio is never other app`() {
        assertFalse(otherAppPlaying(isMusicActive = false, hostHasActiveContext = false))
        assertFalse(otherAppPlaying(isMusicActive = false, hostHasActiveContext = true))
    }
}
