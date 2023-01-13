package au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcasts.config

import androidx.activity.viewModels
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityConfigQueryPodcasts : ActivityConfigBase<ViewModelConfigQueryPodcasts>() {
    override val viewModel: ViewModelConfigQueryPodcasts by viewModels()
    override val finishForTaskerRightAway get() = true
}
