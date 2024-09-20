package au.com.shiftyjelly.pocketcasts.crashlogging

import java.util.Locale

internal fun interface LocaleProvider {
    fun provideLocale(): Locale?
}
