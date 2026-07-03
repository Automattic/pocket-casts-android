package au.com.shiftyjelly.pocketcasts.voicecontrol.gate

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceControlGateTest {

    @Test
    fun `gate is allowed when all rules are allowed`() = runTest {
        val gate = VoiceControlGate(
            listOf(
                FakeRule("setup", VoiceControlRuleGroup.Setup, MutableStateFlow(VoiceControlRuleState.Allowed)),
                FakeRule("conflict", VoiceControlRuleGroup.Conflicts, MutableStateFlow(VoiceControlRuleState.Allowed)),
                FakeRule("context", VoiceControlRuleGroup.Context, MutableStateFlow(VoiceControlRuleState.Allowed)),
            ),
        )

        gate.state.test {
            assertTrue(awaitItem().allowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `gate is blocked when a Setup rule is blocked`() = runTest {
        val gate = VoiceControlGate(
            listOf(
                FakeRule(
                    "setup",
                    VoiceControlRuleGroup.Setup,
                    MutableStateFlow(VoiceControlRuleState.Blocked("user_disabled")),
                ),
                FakeRule("conflict", VoiceControlRuleGroup.Conflicts, MutableStateFlow(VoiceControlRuleState.Allowed)),
                FakeRule("context", VoiceControlRuleGroup.Context, MutableStateFlow(VoiceControlRuleState.Allowed)),
            ),
        )

        gate.state.test {
            assertFalse(awaitItem().allowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `gate is blocked when an Unknown Conflicts rule exists`() = runTest {
        val gate = VoiceControlGate(
            listOf(
                FakeRule("setup", VoiceControlRuleGroup.Setup, MutableStateFlow(VoiceControlRuleState.Allowed)),
                FakeRule(
                    "conflict",
                    VoiceControlRuleGroup.Conflicts,
                    MutableStateFlow(VoiceControlRuleState.Unknown("unknown_route")),
                ),
                FakeRule("context", VoiceControlRuleGroup.Context, MutableStateFlow(VoiceControlRuleState.Allowed)),
            ),
        )

        gate.state.test {
            assertFalse(awaitItem().allowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Context group passes with one Allowed and one Blocked`() = runTest {
        val gate = VoiceControlGate(
            listOf(
                FakeRule("setup", VoiceControlRuleGroup.Setup, MutableStateFlow(VoiceControlRuleState.Allowed)),
                FakeRule("conflict", VoiceControlRuleGroup.Conflicts, MutableStateFlow(VoiceControlRuleState.Allowed)),
                FakeRule("ctx1", VoiceControlRuleGroup.Context, MutableStateFlow(VoiceControlRuleState.Allowed)),
                FakeRule(
                    "ctx2",
                    VoiceControlRuleGroup.Context,
                    MutableStateFlow(VoiceControlRuleState.Blocked("inactive")),
                ),
            ),
        )

        gate.state.test {
            assertTrue(awaitItem().allowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Context all-blocked fails the gate`() = runTest {
        val gate = VoiceControlGate(
            listOf(
                FakeRule("setup", VoiceControlRuleGroup.Setup, MutableStateFlow(VoiceControlRuleState.Allowed)),
                FakeRule("conflict", VoiceControlRuleGroup.Conflicts, MutableStateFlow(VoiceControlRuleState.Allowed)),
                FakeRule(
                    "ctx1",
                    VoiceControlRuleGroup.Context,
                    MutableStateFlow(VoiceControlRuleState.Blocked("inactive")),
                ),
                FakeRule(
                    "ctx2",
                    VoiceControlRuleGroup.Context,
                    MutableStateFlow(VoiceControlRuleState.Blocked("unknown")),
                ),
            ),
        )

        gate.state.test {
            assertFalse(awaitItem().allowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `gate with no rules is allowed`() = runTest {
        val gate = VoiceControlGate(emptyList())

        gate.state.test {
            assertTrue(awaitItem().allowed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rules map contains all rule states`() = runTest {
        val gate = VoiceControlGate(
            listOf(
                FakeRule("a", VoiceControlRuleGroup.Setup, MutableStateFlow(VoiceControlRuleState.Allowed)),
                FakeRule(
                    "b",
                    VoiceControlRuleGroup.Conflicts,
                    MutableStateFlow(VoiceControlRuleState.Blocked("conflict")),
                ),
            ),
        )

        gate.state.test {
            val state = awaitItem()
            assertEquals(2, state.rules.size)
            assertEquals(VoiceControlRuleState.Allowed, state.rules["a"])
            assertEquals(VoiceControlRuleState.Blocked("conflict"), state.rules["b"])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dynamic state change propagates correctly`() = runTest {
        val conflictState = MutableStateFlow<VoiceControlRuleState>(VoiceControlRuleState.Allowed)
        val conflictingRule =
            FakeRule("dyn", VoiceControlRuleGroup.Conflicts, conflictState)

        val gate = VoiceControlGate(
            listOf(
                FakeRule("setup", VoiceControlRuleGroup.Setup, MutableStateFlow(VoiceControlRuleState.Allowed)),
                conflictingRule,
                FakeRule("ctx", VoiceControlRuleGroup.Context, MutableStateFlow(VoiceControlRuleState.Allowed)),
            ),
        )

        gate.state.test {
            // Initially allowed
            assertTrue(awaitItem().allowed)

            // Block the conflict rule
            conflictState.value = VoiceControlRuleState.Blocked("blocked_now")
            assertFalse(awaitItem().allowed)

            // Restore to allowed
            conflictState.value = VoiceControlRuleState.Allowed
            assertTrue(awaitItem().allowed)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeRule(
        override val id: String,
        override val group: VoiceControlRuleGroup,
        override val state: StateFlow<VoiceControlRuleState>,
    ) : VoiceControlRule
}
