package au.com.shiftyjelly.pocketcasts.models.type

import com.automattic.eventhorizon.TrimModeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class TrimMode(
    val serverId: Int,
    val analyticsValue: TrimModeType,
    val labelId: Int,
) {
    OFF(
        serverId = 0,
        analyticsValue = TrimModeType.Off,
        labelId = LR.string.off,
    ),
    LOW(
        serverId = 1,
        analyticsValue = TrimModeType.Mild,
        labelId = LR.string.player_effects_trim_mild,
    ),
    MEDIUM(
        serverId = 2,
        analyticsValue = TrimModeType.Medium,
        labelId = LR.string.player_effects_trim_medium,
    ),
    HIGH(
        serverId = 3,
        analyticsValue = TrimModeType.MadMax,
        labelId = LR.string.player_effects_trim_mad_max,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id } ?: OFF
    }
}
