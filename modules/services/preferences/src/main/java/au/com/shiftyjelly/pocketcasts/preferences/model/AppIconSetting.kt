package au.com.shiftyjelly.pocketcasts.preferences.model

enum class AppIconSetting(val id: String) {
    DEFAULT("default"),
    DARK("dark"),
    ROUND_LIGHT("roundedLight"),
    ROUND_DARK("roundedDark"),
    INDIGO("indigo"),
    ROSE("rose"),
    CAT("cat"),
    REDVELVET("redvelvet"),
    PRIDE("pride_2023"), // Leave the 2023 in the setting value so existing users don't lose their icon
    PLUS("plus"),
    CLASSIC("classic"),
    ELECTRIC_BLUE("electricBlue"),
    ELECTRIC_PINK("electricPink"),
    RADIOACTIVE("radioactive"),
    HALLOWEEN("halloween"),
    PATRON_CHROME("patron_chrome"),
    PATRON_ROUND("patron_round"),
    PATRON_GLOW("patron_glow"),
    PATRON_DARK("patron_dark"),
}
