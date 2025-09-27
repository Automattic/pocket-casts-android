package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class TrimMode(
    val serverId: Int,
    val analyticsVale: String,
    val labelId: Int,
) {
    OFF(
        serverId = 0,
        analyticsVale = "off",
        labelId = LR.string.off,
    ),
    LOW(
        serverId = 1,
        analyticsVale = "mild",
        labelId = LR.string.player_effects_trim_mild,
    ),
    MEDIUM(
        serverId = 2,
        analyticsVale = "medium",
        labelId = LR.string.player_effects_trim_medium,
    ),
    HIGH(
        serverId = 3,
        analyticsVale = "mad_max",
        labelId = LR.string.player_effects_trim_mad_max,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id } ?: OFF
    }
}
