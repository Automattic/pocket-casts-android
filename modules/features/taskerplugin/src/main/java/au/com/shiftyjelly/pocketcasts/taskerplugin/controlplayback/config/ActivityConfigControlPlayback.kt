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
        val taskerVariables = viewModel.taskerVariables
        val commandContent = TaskerInputFieldState.Content(
            viewModel.commandState.collectAsState().value,
            au.com.shiftyjelly.pocketcasts.localization.R.string.playback_command,
            { viewModel.command = it },
            taskerVariables,
            viewModel.availableCommands.toList()
        ) {
            Text(viewModel.getDescription(it))
        }
        val chapterToSkipToContent = if (viewModel.shouldAskForChapter.collectAsState().value) {
            TaskerInputFieldState.Content<String>(
                viewModel.chapterToSkipTo.collectAsState().value,
                au.com.shiftyjelly.pocketcasts.localization.R.string.chapter_to_skip_to,
                { viewModel.setChapterToSkipTo(it) },
                taskerVariables
            )
        } else {
            null
        }
        val timeToSkipToContent = if (viewModel.showAskForTimeToSkipTo.collectAsState().value) {
            TaskerInputFieldState.Content<String>(
                viewModel.timeToSkipTo.collectAsState().value,
                au.com.shiftyjelly.pocketcasts.localization.R.string.time_to_skip_to_seconds,
                { viewModel.setTimeToSkipTo(it) },
                taskerVariables
            )
        } else {
            null
        }
        val timeToSkipContent = if (viewModel.showAskForTimeToSkip.collectAsState().value) {
            TaskerInputFieldState.Content<String>(
                viewModel.timeToSkip.collectAsState().value,
                au.com.shiftyjelly.pocketcasts.localization.R.string.time_to_skip_seconds,
                { viewModel.setTimeToSkip(it) },
                taskerVariables
            )
        } else {
            null
        }
        ComposableConfigControlPlayback(
            commandContent,
            chapterToSkipToContent,
            timeToSkipToContent,
            timeToSkipContent
        ) { viewModel.finishForTasker() }
    }
}
