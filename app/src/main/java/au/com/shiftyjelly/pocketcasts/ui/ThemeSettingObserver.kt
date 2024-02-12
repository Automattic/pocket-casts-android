package au.com.shiftyjelly.pocketcasts.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ThemeSetting
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class ThemeSettingObserver(
    private val theme: Theme,
    private val themeSetting: UserSetting<ThemeSetting>,
    private val useSystemTheme: UserSetting<Boolean>,
    private val appCompatActivity: AppCompatActivity,
) {
    fun observeThemeChanges() {
        appCompatActivity.lifecycleScope.launch {
            appCompatActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    themeSetting.flow.collectLatest { themeSetting ->
                        theme.updateTheme(appCompatActivity, Theme.ThemeType.fromThemeSetting(themeSetting))
                    }
                }
                launch {
                    useSystemTheme.flow.filter { it }.collectLatest { _ ->
                        theme.setupThemeForConfig(appCompatActivity, appCompatActivity.resources.configuration)
                    }
                }
            }
        }
    }
}
