package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions

import au.com.shiftyjelly.pocketcasts.voicecontrol.foreground.ForegroundStateMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleGroup
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.GracePeriodSignal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GateConditionsTest {

    // -- DeviceSupportedCondition --------------------------------------------

    @Test
    fun `device supported when API 26 and sufficient RAM`() {
        val condition = DeviceSupportedCondition(apiLevel = 26, sufficientRam = true)
        assertEquals(VoiceControlRuleGroup.Setup, condition.group)
        assertEquals("device_supported", condition.id)
        assertTrue(condition.evaluate() is VoiceControlRuleState.Allowed)
    }

    @Test
    fun `device unsupported when API below 26`() {
        val condition = DeviceSupportedCondition(apiLevel = 25, sufficientRam = true)
        val state = condition.evaluate()
        assertTrue(state is VoiceControlRuleState.Blocked)
        assertEquals("device_unsupported", (state as VoiceControlRuleState.Blocked).reason)
    }

    @Test
    fun `device unsupported when insufficient RAM`() {
        val condition = DeviceSupportedCondition(apiLevel = 26, sufficientRam = false)
        val state = condition.evaluate()
        assertTrue(state is VoiceControlRuleState.Blocked)
        assertEquals("device_unsupported", (state as VoiceControlRuleState.Blocked).reason)
    }

    // -- ModelsReadyCondition ------------------------------------------------

    @Test
    fun `models allowed when ready`() {
        val condition = ModelsReadyCondition(isReady = true)
        assertEquals(VoiceControlRuleGroup.Setup, condition.group)
        assertEquals("models_ready", condition.id)
        assertTrue(condition.evaluate() is VoiceControlRuleState.Allowed)
    }

    @Test
    fun `models unknown when loading`() {
        val condition = ModelsReadyCondition(isReady = false, failed = false)
        val state = condition.evaluate()
        assertTrue(state is VoiceControlRuleState.Unknown)
        assertEquals("models_loading", (state as VoiceControlRuleState.Unknown).reason)
    }

    @Test
    fun `models blocked on download failure`() {
        val condition = ModelsReadyCondition(isReady = false, failed = true)
        val state = condition.evaluate()
        assertTrue(state is VoiceControlRuleState.Blocked)
        assertEquals("model_download_failed", (state as VoiceControlRuleState.Blocked).reason)
    }

    // -- NotOnCallCondition --------------------------------------------------

    @Test
    fun `not on call when idle`() {
        val condition = NotOnCallCondition(isInCall = false)
        assertEquals(VoiceControlRuleGroup.Conflicts, condition.group)
        assertEquals("not_on_call", condition.id)
        assertTrue(condition.evaluate() is VoiceControlRuleState.Allowed)
    }

    @Test
    fun `blocked when in call`() {
        val condition = NotOnCallCondition(isInCall = true)
        val state = condition.evaluate()
        assertTrue(state is VoiceControlRuleState.Blocked)
        assertEquals("on_call", (state as VoiceControlRuleState.Blocked).reason)
    }

    // -- NotCastingCondition -------------------------------------------------

    @Test
    fun `not casting when idle`() {
        val condition = NotCastingCondition(isCasting = false)
        assertEquals(VoiceControlRuleGroup.Conflicts, condition.group)
        assertEquals("not_casting", condition.id)
        assertTrue(condition.evaluate() is VoiceControlRuleState.Allowed)
    }

    @Test
    fun `blocked when casting`() {
        val condition = NotCastingCondition(isCasting = true)
        val state = condition.evaluate()
        assertTrue(state is VoiceControlRuleState.Blocked)
        assertEquals("casting", (state as VoiceControlRuleState.Blocked).reason)
    }

    // -- BatteryOkCondition --------------------------------------------------

    @Test
    fun `battery ok when not in power save`() {
        val condition = BatteryOkCondition(isPowerSaveMode = false)
        assertEquals(VoiceControlRuleGroup.Conflicts, condition.group)
        assertEquals("battery_ok", condition.id)
        assertTrue(condition.evaluate() is VoiceControlRuleState.Allowed)
    }

    @Test
    fun `blocked when in power save mode`() {
        val condition = BatteryOkCondition(isPowerSaveMode = true)
        val state = condition.evaluate()
        assertTrue(state is VoiceControlRuleState.Blocked)
        assertEquals("battery_saver", (state as VoiceControlRuleState.Blocked).reason)
    }

    // -- AppInForegroundCondition --------------------------------------------

    @Test
    fun `allowed when in foreground`() = runTest {
        val condition = AppInForegroundCondition(mockForegroundMonitor(isForeground = true), backgroundScope)
        assertEquals(VoiceControlRuleGroup.Context, condition.group)
        assertEquals("app_in_foreground", condition.id)
        assertTrue(condition.evaluate() is VoiceControlRuleState.Allowed)
    }

    @Test
    fun `blocked when not in foreground`() = runTest {
        val condition = AppInForegroundCondition(mockForegroundMonitor(isForeground = false), backgroundScope)
        val state = condition.evaluate()
        assertTrue(state is VoiceControlRuleState.Blocked)
        assertEquals("not_foreground", (state as VoiceControlRuleState.Blocked).reason)
    }

    private fun mockForegroundMonitor(isForeground: Boolean): ForegroundStateMonitor {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        return ForegroundStateMonitor(
            appLifecycleProvider = object : au.com.shiftyjelly.pocketcasts.repositories.playback.AppLifecycleProvider {
                override val isInForeground = MutableStateFlow(isForeground)
            },
            scope = scope,
            gracePeriodSignal = GracePeriodSignal(),
        )
    }
}
