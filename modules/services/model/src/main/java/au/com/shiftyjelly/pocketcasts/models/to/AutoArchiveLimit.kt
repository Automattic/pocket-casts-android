package au.com.shiftyjelly.pocketcasts.models.to

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R as LR

enum class AutoArchiveLimit(
    val value: Int?,
    val serverId: Int,
    val analyticsValue: String,
    @StringRes val stringRes: Int,
) {
    None(
        value = null,
        serverId = 0,
        analyticsValue = "none",
        stringRes = LR.string.settings_auto_archive_limit_none,
    ),
    One(
        value = 1,
        serverId = 1,
        analyticsValue = "1",
        stringRes = LR.string.settings_auto_archive_limit_1,
    ),
    Two(
        value = 2,
        serverId = 2,
        analyticsValue = "2",
        stringRes = LR.string.settings_auto_archive_limit_2,
    ),
    Five(
        value = 5,
        serverId = 5,
        analyticsValue = "5",
        stringRes = LR.string.settings_auto_archive_limit_5,
    ),
    Ten(
        value = 10,
        serverId = 10,
        analyticsValue = "10",
        stringRes = LR.string.settings_auto_archive_limit_10,
    ),
    ;

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
