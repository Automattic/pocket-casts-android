package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.config

import androidx.activity.viewModels
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityConfigControlPlayback : ActivityConfigBase<ViewModelConfigControlPlayback>() {
    override val viewModel: ViewModelConfigControlPlayback by viewModels()
}
