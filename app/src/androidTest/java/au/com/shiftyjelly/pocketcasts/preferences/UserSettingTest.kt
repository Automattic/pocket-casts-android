import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.Before
import org.junit.Test

class UserSettingTest {

    private val sharedPrefKey = "sharedPrefKey"

    @Before
    fun setUp() {
        // clear out test shared preferences so tests don't interfere with each other
        getSharedPreferences().edit().clear().commit()
    }

    @Test
    fun handlesSetWithoutSync() {
        val userSetting = getUserSetting()

        userSetting.set("new_value", needsSync = false)

        assertWillNotSync(userSetting)
    }

    @Test
    fun handlesSetWithSync() {
        val userSetting = getUserSetting()

        val newValue = "new_value"
        userSetting.set(newValue, needsSync = true)

        assertWillSync(userSetting, newValue)
    }

    @Test
    fun keepsSyncEvenWithModificationNotRequiringSync() {
        val userSetting = getUserSetting()

        // first set requires sync
        userSetting.set("first_value", needsSync = true)

        // second set does not require sync
        val newValue = "second_value"
        userSetting.set("second_value", needsSync = false)

        assertWillSync(userSetting, newValue)
    }

    @Test
    fun clearsSync() {
        val userSetting = getUserSetting()

        userSetting.set("new_value", needsSync = true)
        userSetting.doesNotNeedSync()

        assertWillNotSync(userSetting)
    }

    fun assertWillSync(userSetting: UserSetting.StringPref, expected: String) {
        assertNotNull(userSetting.getModifiedAtServerString())
        assertNotNull(userSetting.getModifiedAt())
        val result = userSetting.getSyncSetting { value, _ -> value }
        assertEquals(expected, result)
    }

    private fun assertWillNotSync(userSetting: UserSetting.StringPref) {
        assertNull(userSetting.getModifiedAtServerString())
        assertNull(userSetting.getModifiedAt())
        val result = userSetting.getSyncSetting { _, _ -> Unit }
        assertNull(result)
    }

    private fun getUserSetting() = UserSetting.StringPref(
        sharedPrefKey = sharedPrefKey,
        sharedPrefs = getSharedPreferences(),
        defaultValue = "default",
    )

    private fun getSharedPreferences(): SharedPreferences =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getSharedPreferences("instrumentation_test_shared_prefs", Context.MODE_PRIVATE)
}
