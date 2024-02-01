package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting

enum class ThemeSetting(
    val id: String,
    val serverId: Int,
) {
    LIGHT(
        id = "light",
        serverId = 0,
    ),
    DARK(
        id = "dark",
        serverId = 1,
    ),
    ROSE(
        id = "rose",
        serverId = 3,
    ),
    INDIGO(
        id = "indigo",
        serverId = 4,
    ),
    EXTRA_DARK(
        id = "extraDark",
        serverId = 2,
    ),
    DARK_CONTRAST(
        id = "darkContrast",
        serverId = 5,
    ),
    LIGHT_CONTRAST(
        id = "lightContrast",
        serverId = 6,
    ),
    ELECTRIC(
        id = "electric",
        serverId = 7,
    ),
    CLASSIC_LIGHT(
        id = "classicLight",
        serverId = 8,
    ),
    RADIOACTIVE(
        id = "radioactive",
        serverId = 9,
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
        fun fromServerId(id: Int) = entries.find { it.serverId == id } ?: LIGHT
    }
}
