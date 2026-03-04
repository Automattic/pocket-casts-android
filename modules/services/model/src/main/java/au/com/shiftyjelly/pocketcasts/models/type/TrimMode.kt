package au.com.shiftyjelly.pocketcasts.models.type

import com.automattic.eventhorizon.TrimModeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class TrimMode(
    val serverId: Int,
    val eventHorizonValue: TrimModeType,
    val labelId: Int,
) {
    OFF(
        serverId = 0,
        eventHorizonValue = TrimModeType.Off,
        labelId = LR.string.off,
    ),
    LOW(
        serverId = 1,
        eventHorizonValue = TrimModeType.Mild,
        labelId = LR.string.player_effects_trim_mild,
    ),
    MEDIUM(
        serverId = 2,
        eventHorizonValue = TrimModeType.Medium,
        labelId = LR.string.player_effects_trim_medium,
    ),
    HIGH(
        serverId = 3,
        eventHorizonValue = TrimModeType.MadMax,
        labelId = LR.string.player_effects_trim_mad_max,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id } ?: OFF
    }
}
