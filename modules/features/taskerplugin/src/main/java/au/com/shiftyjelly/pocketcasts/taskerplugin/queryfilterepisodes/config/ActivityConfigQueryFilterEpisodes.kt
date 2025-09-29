package au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilterepisodes.config

import androidx.activity.viewModels
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityConfigQueryFilterEpisodes : ActivityConfigBase<ViewModelConfigQueryFilterEpisodes>() {
    override val viewModel: ViewModelConfigQueryFilterEpisodes by viewModels()
}
