package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.config

import androidx.activity.viewModels
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityConfigPlayPlaylist : ActivityConfigBase<ViewModelConfigPlayPlaylist>() {
    override val viewModel: ViewModelConfigPlayPlaylist by viewModels()
}
