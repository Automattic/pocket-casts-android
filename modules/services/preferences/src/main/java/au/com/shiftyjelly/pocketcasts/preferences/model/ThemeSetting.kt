package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting

enum class ThemeSetting(
    val id: String,
    val serverId: Int,
    val analyticsValue: String,
) {
    LIGHT(
        id = "light",
        serverId = 0,
        analyticsValue = "default_light",
    ),
    DARK(
        id = "dark",
        serverId = 1,
        analyticsValue = "default_dark",
    ),
    ROSE(
        id = "rose",
        serverId = 3,
        analyticsValue = "rose",
    ),
    INDIGO(
        id = "indigo",
        serverId = 4,
        analyticsValue = "indigo",
    ),
    EXTRA_DARK(
        id = "extraDark",
        serverId = 2,
        analyticsValue = "extra_dark",
    ),
    DARK_CONTRAST(
        id = "darkContrast",
        serverId = 5,
        analyticsValue = "dark_contrast",
    ),
    LIGHT_CONTRAST(
        id = "lightContrast",
        serverId = 6,
        analyticsValue = "light_contrast",
    ),
    ELECTRIC(
        id = "electric",
        serverId = 7,
        analyticsValue = "electric",
    ),
    CLASSIC_LIGHT(
        id = "classicLight",
        serverId = 8,
        analyticsValue = "classic",
    ),
    RADIOACTIVE(
        id = "radioactive",
        serverId = 9,
        analyticsValue = "radioactive",
    ),
    ;

    class UserSettingPref(
        sharedPrefKey: String,
        defaultValue: ThemeSetting,
        sharedPrefs: SharedPreferences,
    ) : UserSetting.PrefFromString<ThemeSetting>(
        sharedPrefKey = sharedPrefKey,
        defaultValue = defaultValue,
        sharedPrefs = sharedPrefs,
        fromString = { str ->
            ThemeSetting.entries.find { it.id == str } ?: defaultValue
        },
        toString = { it.id },
    )

    companion object {
        fun fromServerId(id: Int) = entries.find { it.serverId == id }
    }
}
