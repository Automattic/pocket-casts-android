package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.config

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.activity.ComposableConfigPlayPlaylist
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityConfigPlayPlaylist : ActivityConfigBase<ViewModelConfigPlayPlaylist>() {
    override val viewModel: ViewModelConfigPlayPlaylist by viewModels()

    @Composable
    override fun Content() {
        ComposableConfigPlayPlaylist(viewModel) { viewModel.finishForTasker() }
    }
}
