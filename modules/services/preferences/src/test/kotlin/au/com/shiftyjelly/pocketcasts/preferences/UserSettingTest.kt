package au.com.shiftyjelly.pocketcasts.preferences

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingTest {

    @Test
    fun `refresh emits the persisted value when the flow is initialized`() {
        val sharedPrefs = FakeSharedPreferences()
        val setting = UserSetting.BoolPref("key", defaultValue = false, sharedPrefs = sharedPrefs)
        setting.set(true, updateModifiedAt = false)
        assertTrue(setting.flow.value)

        sharedPrefs.edit().clear().commit()
        setting.refresh()

        assertFalse(setting.flow.value)
    }

    @Test
    fun `refresh before the flow is initialized does not crash`() {
        val setting = UserSetting.BoolPref("key", defaultValue = false, sharedPrefs = FakeSharedPreferences())

        setting.refresh()

        assertFalse(setting.flow.value)
    }

    @Test
    fun `refreshAll refreshes only settings backed by the given preferences`() {
        val prefsA = FakeSharedPreferences()
        val prefsB = FakeSharedPreferences()
        val settingA = UserSetting.BoolPref("key", defaultValue = false, sharedPrefs = prefsA)
        val settingB = UserSetting.BoolPref("key", defaultValue = false, sharedPrefs = prefsB)
        settingA.set(true, updateModifiedAt = false)
        settingB.set(true, updateModifiedAt = false)

        prefsA.edit().clear().commit()
        prefsB.edit().clear().commit()
        UserSetting.refreshAll(prefsA)

        assertFalse(settingA.flow.value)
        assertTrue(settingB.flow.value)
    }
}
