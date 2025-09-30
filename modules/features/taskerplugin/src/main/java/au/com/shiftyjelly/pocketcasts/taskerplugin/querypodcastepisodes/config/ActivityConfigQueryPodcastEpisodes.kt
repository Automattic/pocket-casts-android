package au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcastepisodes.config

import androidx.activity.viewModels
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityConfigQueryPodcastEpisodes : ActivityConfigBase<ViewModelConfigQueryPodcastEpisodes>() {
    override val viewModel: ViewModelConfigQueryPodcastEpisodes by viewModels()
}
