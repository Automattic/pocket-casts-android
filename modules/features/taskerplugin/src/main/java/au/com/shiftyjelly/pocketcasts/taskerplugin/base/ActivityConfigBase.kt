package au.com.shiftyjelly.pocketcasts.taskerplugin.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.appTheme

abstract class ActivityConfigBase<TViewModel : ViewModelBase<*, *>> : ComponentActivity() {
    protected abstract val viewModel: TViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onCreate({ finish() }, { intent }, { code, data -> setResult(code, data) })
        setContent {
            AppThemeWithBackground(themeType = appTheme.activeTheme) {
                Content()
            }
        }
    }
    @Composable
    protected abstract fun Content()
}
