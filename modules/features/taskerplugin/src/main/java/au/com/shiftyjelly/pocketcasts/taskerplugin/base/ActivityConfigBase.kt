package au.com.shiftyjelly.pocketcasts.taskerplugin.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.appTheme

abstract class ActivityConfigBase<TViewModel : ViewModelBase<*, *, *>> : ComponentActivity() {
    protected abstract val viewModel: TViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onCreate({ finish() }, { intent }, { code, data -> setResult(code, data) })
        if (finishForTaskerRightAway) {
            viewModel.finishForTasker()
            return
        }
        setContent {
            AppThemeWithBackground(themeType = appTheme.activeTheme) {
                val taskerVariables = viewModel.taskerVariables
                val inputs = viewModel.inputFields.map { field ->
                    TaskerInputFieldState.Content(
                        field.valueState,
                        field.labelResId,
                        field.iconResId,
                        field.shouldAskForState,
                        { field.value = it },
                        taskerVariables,
                        field.getPossibleValues()
                    ) {
                        Text(field.getValueDescription(it))
                    }
                }
                ComposableTaskerInputFieldList(inputs) {
                    viewModel.finishForTasker()
                }
            }
        }
    }
    protected open val finishForTaskerRightAway get() = false
}
