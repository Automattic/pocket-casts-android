package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import java.util.Locale

object WhatsNewCatalogLocale {
    const val FALLBACK = "en"

    private val traditionalChineseCountries = setOf("tw", "hk", "mo")

    private val legacyLanguageCodes = mapOf("iw" to "he", "in" to "id", "ji" to "yi")

    fun catalogName(locale: Locale): String {
        val language = locale.language.lowercase().let { language -> legacyLanguageCodes[language] ?: language }
        val country = locale.country.lowercase()
        val script = locale.script.lowercase()

        return when {
            language.isEmpty() -> FALLBACK
            language == "pt" && country == "br" -> "pt-br"
            language != "zh" -> language
            script == "hant" -> "zh-tw"
            script == "hans" -> "zh-cn"
            country in traditionalChineseCountries -> "zh-tw"
            else -> "zh-cn"
        }
    }
}
