package au.com.shiftyjelly.pocketcasts.localization.helper

import android.content.Context
import androidx.annotation.StringRes
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.localization.R
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4ClassRunner::class)
class LocalisationTests {

    private val locales = listOf(
        Locale("en", "EN"),
        Locale("de", "DE"),
        Locale("es", "ES"),
        Locale("fr", "FR"),
        Locale("it", "IT"),
        Locale("ja", "JP"),
        Locale("nl", "NL"),
        Locale("pt", "BR"),
        Locale("ru", "RU"),
        Locale("sv", "SV"),
        Locale("zh", "TW"),
        Locale("zh", "CN"),
    )

    private val strings = listOf(
        StringArgs(R.string.settings_stats_funny_babies, listOf(500.0)),
        StringArgs(R.string.settings_stats_funny_blinked, listOf(500.0)),
        StringArgs(R.string.settings_stats_funny_lightning, listOf(500.0)),
        StringArgs(R.string.settings_stats_funny_skin, listOf(500.0)),
        StringArgs(R.string.settings_stats_funny_sneezed, listOf(500.0)),
        StringArgs(R.string.settings_stats_funny_emails, listOf(500.0)),
        StringArgs(R.string.settings_stats_funny_tweets, listOf(500.0)),
        StringArgs(R.string.settings_stats_funny_air_biscuits, listOf(500.0)),
        StringArgs(R.string.settings_stats_funny_laces, listOf(500.0)),
        StringArgs(R.string.settings_stats_funny_air_balloon, listOf(500.0)),
        StringArgs(R.string.settings_stats_funny_searches, listOf(500.0)),
    )

    /**
     * It is common during a GlotPress translation the string formatter gets broken. This is to make sure that doesn't happen.
     */
    @Test
    fun testStringFormatter() {
        for (locale in locales) {
            val contextLocale = getContextWithLocale(locale)
            for (string in strings) {
                try {
                    contextLocale.getString(string.stringId, *string.args.toTypedArray())
                } catch (ex: Exception) {
                    throw Error("Localisation in $locale failed '${contextLocale.getString(string.stringId)}'", ex)
                }
            }
        }
    }

    private fun getContextWithLocale(locale: Locale): Context {
        // update locale for date formatters
        Locale.setDefault(locale)
        // update locale for app resources
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = context.resources.configuration.apply {
            setLocale(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    data class StringArgs(@StringRes val stringId: Int, val args: List<Any>)
}
