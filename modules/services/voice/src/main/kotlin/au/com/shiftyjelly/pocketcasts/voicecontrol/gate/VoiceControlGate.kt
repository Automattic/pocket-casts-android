package au.com.shiftyjelly.pocketcasts.voicecontrol.gate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

data class VoiceControlGateState(
    val allowed: Boolean,
    val rules: Map<String, VoiceControlRuleState>,
    // Per-group condition states, surfaced for lifecycle logging (spec: logs carry
    // setup/conflict/context condition states). Defaults keep rule-free states allowed.
    val setupPassed: Boolean = true,
    val conflictsPassed: Boolean = true,
    val contextPassed: Boolean = true,
)

class VoiceControlGate(
    rules: List<VoiceControlRule>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) {
    val state: StateFlow<VoiceControlGateState> = if (rules.isEmpty()) {
        kotlinx.coroutines.flow.MutableStateFlow(
            VoiceControlGateState(allowed = true, rules = emptyMap()),
        )
    } else {
        combine(rules.map { it.state }) { ruleStates ->
            computeState(rules.zip(ruleStates))
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = computeState(rules.map { it to it.state.value }),
        )
    }

    private fun computeState(
        paired: List<Pair<VoiceControlRule, VoiceControlRuleState>>,
    ): VoiceControlGateState {
        val rulesMap = paired.associate { (rule, state) -> rule.id to state }

        val groupedByGroup = paired.groupBy { (rule, _) -> rule.group }

        val setupPassed = groupAllAllowed(groupedByGroup[VoiceControlRuleGroup.Setup])
        val conflictsPassed = groupAllAllowed(groupedByGroup[VoiceControlRuleGroup.Conflicts])
        val contextPassed = groupAnyAllowed(groupedByGroup[VoiceControlRuleGroup.Context])

        val allowed = setupPassed && conflictsPassed && contextPassed

        Timber.d(
            "Gate: allowed=%b [setup=%b conflicts=%b context=%b] rules=%s",
            allowed,
            setupPassed,
            conflictsPassed,
            contextPassed,
            rulesMap.entries.joinToString { (id, state) -> "$id=$state" },
        )

        return VoiceControlGateState(
            allowed = allowed,
            rules = rulesMap,
            setupPassed = setupPassed,
            conflictsPassed = conflictsPassed,
            contextPassed = contextPassed,
        )
    }

    private fun groupAllAllowed(rulesInGroup: List<Pair<VoiceControlRule, VoiceControlRuleState>>?): Boolean {
        if (rulesInGroup == null) return true
        return rulesInGroup.all { (_, state) -> state is VoiceControlRuleState.Allowed }
    }

    private fun groupAnyAllowed(rulesInGroup: List<Pair<VoiceControlRule, VoiceControlRuleState>>?): Boolean {
        if (rulesInGroup == null) return true
        return rulesInGroup.any { (_, state) -> state is VoiceControlRuleState.Allowed }
    }
}
