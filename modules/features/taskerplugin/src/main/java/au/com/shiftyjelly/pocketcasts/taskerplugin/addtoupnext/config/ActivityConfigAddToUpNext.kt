package au.com.shiftyjelly.pocketcasts.taskerplugin.addtoupnext.config

import androidx.activity.viewModels
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityConfigAddToUpNext : ActivityConfigBase<ViewModelConfigAddToUpNext>() {
    override val viewModel: ViewModelConfigAddToUpNext by viewModels()
}
