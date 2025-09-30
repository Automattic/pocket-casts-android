package au.com.shiftyjelly.pocketcasts.taskerplugin.queryupnext.config

import androidx.activity.viewModels
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityConfigQueryUpNext : ActivityConfigBase<ViewModelConfigQueryUpNext>() {
    override val viewModel: ViewModelConfigQueryUpNext by viewModels()
    override val finishForTaskerRightAway get() = true
}
