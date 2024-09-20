package au.com.shiftyjelly.pocketcasts.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ThemeSettingObserver(
    private val appCompatActivity: AppCompatActivity,
    private val theme: Theme,
    private val themeChangeRequests: Flow<Unit>,
) {
    fun observeThemeChanges() {
        appCompatActivity.lifecycleScope.launch {
            appCompatActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                themeChangeRequests.collect {
                    theme.setupThemeForConfig(appCompatActivity, appCompatActivity.resources.configuration)
                }
            }
        }
    }
}
