package au.com.shiftyjelly.pocketcasts.views.dialog

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ThemeSetting
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.flags.FragmentGetContextFix
import dagger.hilt.android.internal.builders.FragmentComponentBuilder
import dagger.hilt.android.internal.managers.FragmentComponentManager
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import dagger.hilt.internal.GeneratedComponentManagerHolder
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class HiltShimApplication :
    Application(),
    GeneratedComponentManager<Any> {
    override fun generatedComponent(): Any = SingletonShim

    private object SingletonShim : GeneratedComponent, FragmentGetContextFix.FragmentGetContextFixEntryPoint {
        override fun getDisableFragmentGetContextFix(): Set<Boolean> = emptySet()
    }
}

private val lightThemeSetting = mock<UserSetting<ThemeSetting>> { on { value } doReturn ThemeSetting.LIGHT }

private val lightTheme = Theme(mock<Settings> { on { theme } doReturn lightThemeSetting })

class HiltShimActivity :
    FragmentActivity(),
    GeneratedComponentManagerHolder {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(UR.style.ThemeLight)
        super.onCreate(savedInstanceState)
    }

    override fun componentManager() = this

    override fun generatedComponent(): Any = FragmentShim

    private object FragmentShim :
        GeneratedComponent,
        FragmentComponentManager.FragmentComponentBuilderEntryPoint,
        FragmentComponentBuilder,
        FragmentComponent,
        ConfirmationDialog_GeneratedInjector {
        override fun fragmentComponentBuilder() = this

        override fun fragment(fragment: Fragment) = this

        override fun build() = this

        override fun injectConfirmationDialog(confirmationDialog: ConfirmationDialog) {
            confirmationDialog.theme = lightTheme
        }
    }
}
