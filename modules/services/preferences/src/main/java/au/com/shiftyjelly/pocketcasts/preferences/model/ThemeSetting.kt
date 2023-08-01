package au.com.shiftyjelly.pocketcasts.preferences.model

import android.content.SharedPreferences
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting

enum class ThemeSetting(val id: String) {
    LIGHT("light"),
    DARK("dark"),
    ROSE("rose"),
    INDIGO("indigo"),
    EXTRA_DARK("extraDark"),
    DARK_CONTRAST("darkContrast"),
    LIGHT_CONTRAST("lightContrast"),
    ELECTRIC("electric"),
    CLASSIC_LIGHT("classicLight"),
    RADIOACTIVE("radioactive");

    class UserSettingPref(
        sharedPrefKey: String,
        defaultValue: ThemeSetting,
        sharedPrefs: SharedPreferences,
    ) : UserSetting.PrefFromString<ThemeSetting>(
        sharedPrefKey = sharedPrefKey,
        defaultValue = defaultValue,
        sharedPrefs = sharedPrefs,
        fromString = { str ->
            ThemeSetting.values().find { it.id == str }
                ?: defaultValue
        },
        toString = { it.id },
    )
}
