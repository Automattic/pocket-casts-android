package au.com.shiftyjelly.pocketcasts.preferences

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import android.provider.Settings as AndroidSettings

@Config(manifest = Config.NONE, sdk = [35])
@RunWith(RobolectricTestRunner::class)
class SettingsImplTest {
    private val publicPrefs = FakeSharedPreferences()
    private val privatePrefs = FakeSharedPreferences()

    init {
        AndroidSettings.Secure.putString(
            RuntimeEnvironment.getApplication().contentResolver,
            AndroidSettings.Secure.ANDROID_ID,
            "8bytesid",
        )
    }

    private val settings = SettingsImpl(
        sharedPreferences = publicPrefs,
        privatePreferences = privatePrefs,
        context = RuntimeEnvironment.getApplication(),
        firebaseRemoteConfig = mock<FirebaseRemoteConfig>(),
        moshi = Moshi.Builder()
            .add(
                Instant::class.java,
                object : JsonAdapter<Instant>() {
                    override fun fromJson(reader: JsonReader): Instant = Instant.parse(reader.nextString())
                    override fun toJson(writer: JsonWriter, value: Instant?) {
                        writer.value(value?.toString())
                    }
                },
            )
            .build(),
    )

    @Test
    fun `clearUserPreferences removes user keys from both preferences`() {
        settings.hideNotificationOnPause.set(true, updateModifiedAt = true)
        publicPrefs.edit().putString("SomeOtherKey", "value").commit()
        privatePrefs.edit().putString("SomePrivateKey", "value").commit()

        settings.clearUserPreferences()

        assertFalse(publicPrefs.contains("hideNotificationOnPause"))
        assertFalse(publicPrefs.contains("hideNotificationOnPauseModifiedAt"))
        assertFalse(publicPrefs.contains("SomeOtherKey"))
        assertFalse(privatePrefs.contains("SomePrivateKey"))
    }

    @Test
    fun `clearUserPreferences keeps the analytics consent preferences`() {
        settings.collectAnalytics.set(false, updateModifiedAt = false)
        settings.sendCrashReports.set(false, updateModifiedAt = false)
        settings.linkCrashReportsToUser.set(true, updateModifiedAt = false)

        settings.clearUserPreferences()

        assertFalse(settings.collectAnalytics.value)
        assertFalse(settings.sendCrashReports.value)
        assertTrue(settings.linkCrashReportsToUser.value)
    }

    @Test
    fun `clearUserPreferences refreshes the in-memory setting values`() {
        settings.hideNotificationOnPause.set(true, updateModifiedAt = false)
        assertTrue(settings.hideNotificationOnPause.flow.value)

        settings.clearUserPreferences()

        assertFalse(settings.hideNotificationOnPause.flow.value)
    }
}
