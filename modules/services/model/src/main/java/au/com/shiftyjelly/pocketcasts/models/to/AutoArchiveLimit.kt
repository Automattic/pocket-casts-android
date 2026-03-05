package au.com.shiftyjelly.pocketcasts.models.to

import androidx.annotation.StringRes
import com.automattic.eventhorizon.AutoArchiveLimitType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class AutoArchiveLimit(
    val value: Int?,
    val serverId: Int,
    val eventHorizonValue: AutoArchiveLimitType,
    @StringRes val stringRes: Int,
) {
    None(
        value = null,
        serverId = 0,
        eventHorizonValue = AutoArchiveLimitType.None,
        stringRes = LR.string.settings_auto_archive_limit_none,
    ),
    One(
        value = 1,
        serverId = 1,
        eventHorizonValue = AutoArchiveLimitType.One,
        stringRes = LR.string.settings_auto_archive_limit_1,
    ),
    Two(
        value = 2,
        serverId = 2,
        eventHorizonValue = AutoArchiveLimitType.Two,
        stringRes = LR.string.settings_auto_archive_limit_2,
    ),
    Five(
        value = 5,
        serverId = 5,
        eventHorizonValue = AutoArchiveLimitType.Five,
        stringRes = LR.string.settings_auto_archive_limit_5,
    ),
    Ten(
        value = 10,
        serverId = 10,
        eventHorizonValue = AutoArchiveLimitType.Ten,
        stringRes = LR.string.settings_auto_archive_limit_10,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
