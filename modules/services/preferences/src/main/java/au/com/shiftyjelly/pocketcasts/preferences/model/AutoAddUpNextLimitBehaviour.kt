package au.com.shiftyjelly.pocketcasts.preferences.model

import com.automattic.eventhorizon.AutoAddToUpNextLimitBehaviorType

enum class AutoAddUpNextLimitBehaviour(
    val serverId: Int,
    val analyticsValue: AutoAddToUpNextLimitBehaviorType,
) {
    STOP_ADDING(
        serverId = 1,
        analyticsValue = AutoAddToUpNextLimitBehaviorType.StopAdding,
    ),
    ONLY_ADD_TO_TOP(
        serverId = 0,
        analyticsValue = AutoAddToUpNextLimitBehaviorType.OnlyAddTop,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id } ?: STOP_ADDING
    }
}
