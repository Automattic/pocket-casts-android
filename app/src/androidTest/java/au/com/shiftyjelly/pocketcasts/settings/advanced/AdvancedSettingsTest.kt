package au.com.shiftyjelly.pocketcasts.settings.advanced

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.preferences.SettingsImpl
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

class AdvancedSettingsTest {

    @Test
    fun verifyDefaultSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fileName = "FILE_NAME"

        val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        val moshi = Moshi.Builder().build()
        val settings = SettingsImpl(sharedPreferences, sharedPreferences, context, moshi)

        // Advanced settings
        assertEquals(true, settings.syncOnMeteredNetwork())

        // Non-advanced settings
        assertEquals(false, settings.warnOnMeteredNetwork())
        assertEquals(true, settings.isPodcastAutoDownloadUnmeteredOnly())
        assertEquals(false, settings.isPodcastAutoDownloadPowerOnly())
        assertEquals(false, settings.isUpNextAutoDownloaded())
    }
}
