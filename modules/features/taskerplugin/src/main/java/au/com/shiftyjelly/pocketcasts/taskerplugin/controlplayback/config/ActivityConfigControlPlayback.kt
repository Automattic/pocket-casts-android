package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.config

import androidx.activity.viewModels
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.TaskerInputFieldState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActivityConfigControlPlayback : ActivityConfigBase<ViewModelConfigControlPlayback>() {
    override val viewModel: ViewModelConfigControlPlayback by viewModels()

    @Composable
    override fun Content() {
        val commandContent = TaskerInputFieldState.Content(
            viewModel.commandState.collectAsState().value,
            au.com.shiftyjelly.pocketcasts.localization.R.string.playback_command,
            { viewModel.command = it },
            viewModel.taskerVariables,
            viewModel.availableCommands.toList()
        ) {
            Text(viewModel.getDescription(it))
        }
        val skipToChapterContent = if (viewModel.askForChapterToSkipToState.collectAsState().value) {
            TaskerInputFieldState.Content<String>(
                viewModel.chapterToSkipToState.collectAsState().value,
                au.com.shiftyjelly.pocketcasts.localization.R.string.chapter_to_skip_to,
                { viewModel.chapterToSkipTo = it },
                viewModel.taskerVariables
            )
        } else {
            null
        }
        ComposableConfigControlPlayback(
            commandContent,
            skipToChapterContent
        ) { viewModel.finishForTasker() }
    }
}
