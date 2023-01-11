package au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilters.config

import androidx.activity.viewModels
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityConfigQueryFilters : ActivityConfigBase<ViewModelConfigQueryFilters>() {
    override val viewModel: ViewModelConfigQueryFilters by viewModels()
    override val finishForTaskerRightAway get() = true
}
