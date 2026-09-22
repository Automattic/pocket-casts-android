package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import java.util.Locale

object WhatsNewCatalogLocale {
    const val FALLBACK = "en"

    private val traditionalChineseCountries = setOf("tw", "hk", "mo")

    fun catalogName(locale: Locale): String {
        val language = locale.language.lowercase()
        val country = locale.country.lowercase()
        val script = locale.script.lowercase()

        return when {
            language != "zh" && language != "pt" -> language.ifEmpty { FALLBACK }
            language == "pt" -> if (country == "br") "pt-br" else language
            script == "hant" || country in traditionalChineseCountries -> "zh-tw"
            else -> "zh-cn"
        }
    }
}
