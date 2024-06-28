import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.utils.MutableClock
import java.time.Instant
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class UserSettingTest {
    private val sharedPrefs = InstrumentationRegistry
        .getInstrumentation()
        .targetContext
        .getSharedPreferences("instrumentation_test_shared_prefs", Context.MODE_PRIVATE)

    private val clock = MutableClock()

    private val userSetting = UserSetting.StringPref(
        defaultValue = "default",
        sharedPrefKey = "key",
        sharedPrefs = sharedPrefs,
    )

    @Before
    fun setUp() {
        // clear out test shared preferences so tests don't interfere with each other
        sharedPrefs.edit().clear().commit()
    }

    @Test
    fun usesDefaultValue() {
        assertEquals("default", userSetting.value)
    }

    @Test
    fun handlesSetWithoutTimestampUpdate() {
        clock += 10.seconds
        userSetting.set("new_value", clock = clock, updateModifiedAt = false)

        assertEquals("new_value", userSetting.value)
        assertNull(userSetting.modifiedAt)
    }

    @Test
    fun handlesSetWithTimestampUpdate() {
        userSetting.set("new_value", clock = clock, updateModifiedAt = true)

        assertEquals("new_value", userSetting.value)
        assertEquals(clock.instant(), userSetting.modifiedAt)
    }

    @Test
    fun updateValueButNotTimestampWithoutTimestampUpdate() {
        val currentTime = clock.instant()

        userSetting.set("first_value", clock = clock, updateModifiedAt = true)
        clock += 10.seconds
        userSetting.set("second_value", clock = clock, updateModifiedAt = false)

        assertEquals("second_value", userSetting.value)
        assertEquals(currentTime, userSetting.modifiedAt)
    }

    @Test
    fun emitsValueUpdates() = runTest {
        userSetting.flow.test {
            assertEquals("default", awaitItem())

            userSetting.set("new_value", updateModifiedAt = false)
            assertEquals("new_value", awaitItem())

            cancel()
        }
    }

    @Test
    fun useEpochPlusOneSecondAsDefaultSyncTimestamp() {
        lateinit var timestamp: Instant
        userSetting.getSyncSetting { _, modifiedAt ->
            timestamp = modifiedAt
        }
        assertEquals(Instant.EPOCH.plusSeconds(1), timestamp)
    }

    @Test
    fun useStoredSyncTimestamp() {
        clock += 10.seconds
        userSetting.set("new_value", clock = clock, updateModifiedAt = true)

        lateinit var timestamp: Instant
        userSetting.getSyncSetting { _, modifiedAt ->
            timestamp = modifiedAt
        }
        assertEquals(clock.instant(), timestamp)
    }

    private class ReversingToSetting(
        defaultValue: String,
        sharedPrefKey: String,
        sharedPrefs: SharedPreferences,
    ) : UserSetting.PrefFromString<String>(
        sharedPrefKey = sharedPrefKey,
        defaultValue = defaultValue,
        sharedPrefs = sharedPrefs,
        toString = { it.reversed() },
        fromString = { it },
    )

    @Test
    fun usesCustomToConverter() {
        val setting = ReversingToSetting(
            defaultValue = "default",
            sharedPrefKey = "key",
            sharedPrefs = sharedPrefs,
        )

        assertEquals("default".reversed(), setting.value)

        setting.set("new_value", clock = clock, updateModifiedAt = true)

        assertEquals("new_value".reversed(), setting.value)
        // Verify if converter doesn't jumble modification timestamp
        assertEquals(clock.instant(), setting.modifiedAt)
    }

    private class ReversingFromSetting(
        defaultValue: String,
        sharedPrefKey: String,
        sharedPrefs: SharedPreferences,
    ) : UserSetting.PrefFromString<String>(
        sharedPrefKey = sharedPrefKey,
        defaultValue = defaultValue,
        sharedPrefs = sharedPrefs,
        toString = { it },
        fromString = { it.reversed() },
    )

    @Test
    fun usesCustomFromConverter() {
        val setting = ReversingFromSetting(
            defaultValue = "default",
            sharedPrefKey = "key",
            sharedPrefs = sharedPrefs,
        )

        assertEquals("default".reversed(), setting.value)

        setting.set("new_value", clock = clock, updateModifiedAt = true)

        assertEquals("new_value".reversed(), setting.value)
        // Verify if converter doesn't jumble modification timestamp
        assertEquals(clock.instant(), setting.modifiedAt)
    }
}
