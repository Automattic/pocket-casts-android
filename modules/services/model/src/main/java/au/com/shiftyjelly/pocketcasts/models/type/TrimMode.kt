package au.com.shiftyjelly.pocketcasts.models.type

enum class TrimMode(
    val serverId: Int,
    val analyticsVale: String,
) {
    OFF(serverId = 0, "off"),
    LOW(serverId = 1, "mild"),
    MEDIUM(serverId = 2, "medium"),
    HIGH(serverId = 3, "mad_max"),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id } ?: OFF
    }
}
