package au.com.shiftyjelly.pocketcasts.utils

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DisplayUtil @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    fun hasDynamicFontSize() = Settings.System.getFloat(
        appContext.contentResolver,
        Settings.System.FONT_SCALE
    ) != 1.0f
}
