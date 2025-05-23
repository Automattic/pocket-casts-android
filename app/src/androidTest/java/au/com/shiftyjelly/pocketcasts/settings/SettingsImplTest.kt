package au.com.shiftyjelly.pocketcasts.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.SettingsImpl
import au.com.shiftyjelly.pocketcasts.servers.di.ServersModule
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlin.time.Duration.Companion.days
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsImplTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var settings: SettingsImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        sharedPreferences = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().commit()

        val moshi = ServersModule().provideMoshi()
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        settings = SettingsImpl(
            sharedPreferences = sharedPreferences,
            privatePreferences = sharedPreferences,
            context = context,
            firebaseRemoteConfig = firebaseRemoteConfig,
            moshi = moshi,
        )
    }

    @Test
    fun shouldShowLowStorageModalWhenNeverSnoozed() {
        assertTrue(settings.shouldShowLowStorageModalAfterSnooze())
    }

    @Test
    fun shouldNotShowLowStorageModalIfDismissedLessThanSevenDaysAgo() {
        val twoDaysAgo = System.currentTimeMillis() - 2.days.inWholeMilliseconds
        sharedPreferences.edit().putLong(Settings.LAST_DISMISS_LOW_STORAGE_MODAL_TIME, twoDaysAgo).apply()

        assertFalse(settings.shouldShowLowStorageModalAfterSnooze())
    }

    @Test
    fun shouldShowLowStorageModalIfDismissedSevenDaysAgo() {
        val sevenDaysAgo = System.currentTimeMillis() - 7.days.inWholeMilliseconds
        sharedPreferences.edit().putLong(Settings.LAST_DISMISS_LOW_STORAGE_MODAL_TIME, sevenDaysAgo).apply()

        assertTrue(settings.shouldShowLowStorageModalAfterSnooze())
    }
}
