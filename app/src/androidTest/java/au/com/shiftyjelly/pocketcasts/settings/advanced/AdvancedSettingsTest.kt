package au.com.shiftyjelly.pocketcasts.settings.advanced

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.NetworkType
import au.com.shiftyjelly.pocketcasts.preferences.SettingsImpl
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

class AdvancedSettingsTest {

    @Test
    fun verifyDefaultSettings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fileName = "FILE_NAME"

        val sharedPreferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()
        val moshi = Moshi.Builder().build()
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val settings = SettingsImpl(
            sharedPreferences = sharedPreferences,
            privatePreferences = sharedPreferences,
            context = context,
            firebaseRemoteConfig = firebaseRemoteConfig,
            moshi = moshi,
        )

        // Non-advanced settings
        assertEquals(false, settings.warnOnMeteredNetwork())
        assertEquals(true, settings.isPodcastAutoDownloadUnmeteredOnly())
        assertEquals(false, settings.isPodcastAutoDownloadPowerOnly())
        assertEquals(false, settings.isUpNextAutoDownloaded())
        assertEquals(true, settings.refreshPodcastsAutomatically())

        // Advanced settings
        assertEquals(true, settings.syncOnMeteredNetwork())
        assertEquals(true, settings.refreshPodcastsOnResume(isUnmetered = false))
        assertEquals(true, settings.refreshPodcastsOnResume(isUnmetered = true))
        assertEquals(NetworkType.CONNECTED, settings.getWorkManagerNetworkTypeConstraint())

        // Now modify the default and check the settings
        settings.setSyncOnMeteredNetwork(false)
        assertEquals(false, settings.syncOnMeteredNetwork())
        assertEquals(false, settings.refreshPodcastsOnResume(isUnmetered = false))
        assertEquals(true, settings.refreshPodcastsOnResume(isUnmetered = true))
        assertEquals(NetworkType.UNMETERED, settings.getWorkManagerNetworkTypeConstraint())
    }
}
