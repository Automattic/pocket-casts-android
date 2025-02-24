package au.com.shiftyjelly.pocketcasts.utils

import java.util.Locale

class LocaleUtilImpl : LocaleUtil {
    override fun getLanguage(): String = Locale.getDefault().language
}
