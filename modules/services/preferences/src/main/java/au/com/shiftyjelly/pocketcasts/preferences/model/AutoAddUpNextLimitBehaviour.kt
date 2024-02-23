package au.com.shiftyjelly.pocketcasts.preferences.model

enum class AutoAddUpNextLimitBehaviour(
    val serverId: Int,
) {
    STOP_ADDING(
        serverId = 1,
    ),
    ONLY_ADD_TO_TOP(
        serverId = 0,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id } ?: STOP_ADDING
    }
}
