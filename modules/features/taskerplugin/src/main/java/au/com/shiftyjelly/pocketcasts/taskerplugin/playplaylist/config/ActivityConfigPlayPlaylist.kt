package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.config

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rxjava2.subscribeAsState
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityConfigPlayPlaylist : ActivityConfigBase<ViewModelConfigPlayPlaylist>() {
    override val viewModel: ViewModelConfigPlayPlaylist by viewModels()

    @Composable
    override fun Content() {
        ComposableConfigPlayPlaylist(
            content = ConfigPlayPlaylistState.Content(
                viewModel.titleState.collectAsState().value ?: "",
                viewModel.playlists.subscribeAsState(listOf()).value.map { it.title },
                { viewModel.title = it },
                { viewModel.finishForTasker() }
            )
        )
    }
}
