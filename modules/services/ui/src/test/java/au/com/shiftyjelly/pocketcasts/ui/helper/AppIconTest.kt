package au.com.shiftyjelly.pocketcasts.ui.helper

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AppIconSetting
import au.com.shiftyjelly.pocketcasts.ui.helper.AppIcon.AppIconType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class AppIconTest {

    private lateinit var packageManager: PackageManager
    private lateinit var appIcon: AppIcon

    @Before
    fun setup() {
        packageManager = mock()
        val context = mock<Context>()
        whenever(context.packageName).thenReturn(PACKAGE_NAME)
        whenever(context.packageManager).thenReturn(packageManager)

        val appIconSetting = mock<UserSetting<AppIconSetting>>()
        whenever(appIconSetting.value).thenReturn(AppIconSetting.DEFAULT)
        val settings = mock<Settings>()
        whenever(settings.appIcon).thenReturn(appIconSetting)

        appIcon = AppIcon(context, settings)
    }

    @Test
    @Config(sdk = [32])
    fun `default icon enables the default alias and disables every custom alias`() {
        appIcon.enableSelectedAlias(AppIconType.DEFAULT)

        val componentCaptor = argumentCaptor<ComponentName>()
        val stateCaptor = argumentCaptor<Int>()
        verify(packageManager, times(AppIconType.entries.size)).setComponentEnabledSetting(
            componentCaptor.capture(),
            stateCaptor.capture(),
            eq(PackageManager.DONT_KILL_APP),
        )

        val states = componentCaptor.allValues.zip(stateCaptor.allValues).associate { (component, state) ->
            component.className to state
        }
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, states[DEFAULT_ALIAS])
        assertEquals(1, states.values.count { it == PackageManager.COMPONENT_ENABLED_STATE_ENABLED })
    }

    @Test
    @Config(sdk = [32])
    fun `replacement alias is enabled before the previous alias is disabled`() {
        appIcon.enableSelectedAlias(AppIconType.DARK)

        val componentCaptor = argumentCaptor<ComponentName>()
        val stateCaptor = argumentCaptor<Int>()
        verify(packageManager, times(AppIconType.entries.size)).setComponentEnabledSetting(
            componentCaptor.capture(),
            stateCaptor.capture(),
            eq(PackageManager.DONT_KILL_APP),
        )

        assertEquals(DARK_ALIAS, componentCaptor.firstValue.className)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, stateCaptor.firstValue)
        assertEquals(1, stateCaptor.allValues.count { it == PackageManager.COMPONENT_ENABLED_STATE_ENABLED })
    }

    @Test
    @Config(sdk = [33])
    fun `aliases are updated atomically when supported`() {
        appIcon.enableSelectedAlias(AppIconType.DARK)

        val settingsCaptor = argumentCaptor<List<PackageManager.ComponentEnabledSetting>>()
        verify(packageManager).setComponentEnabledSettings(settingsCaptor.capture())
        verify(packageManager, never()).setComponentEnabledSetting(any(), any(), any())

        val states = settingsCaptor.firstValue.associate { setting ->
            requireNotNull(setting.componentName).className to setting.enabledState
        }
        assertEquals(AppIconType.entries.size, states.size)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, states[DARK_ALIAS])
        assertEquals(1, states.values.count { it == PackageManager.COMPONENT_ENABLED_STATE_ENABLED })
    }

    private companion object {
        const val PACKAGE_NAME = "au.com.shiftyjelly.pocketcasts.debug"
        const val DEFAULT_ALIAS = "au.com.shiftyjelly.pocketcasts.ui.MainActivityDefault"
        const val DARK_ALIAS = "au.com.shiftyjelly.pocketcasts.ui.MainActivity_1"
    }
}
