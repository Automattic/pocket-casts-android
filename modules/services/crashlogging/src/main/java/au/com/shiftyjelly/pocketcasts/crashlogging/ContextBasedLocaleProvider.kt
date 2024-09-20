package au.com.shiftyjelly.pocketcasts.crashlogging

import android.content.Context
import androidx.core.os.ConfigurationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

internal class ContextBasedLocaleProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocaleProvider {
    override fun provideLocale(): Locale? {
        return ConfigurationCompat.getLocales(context.resources.configuration)[0]
    }
}
