package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.config

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ComposableTaskerInputFieldList
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.TaskerInputFieldState
import au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.InputControlPlayback
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun ComposableConfigControlPlayback(
    inputCommandContent: TaskerInputFieldState.Content<InputControlPlayback.PlaybackCommand>,
    inputChapterToSkipToContent: TaskerInputFieldState.Content<String>?,
    inputTimeToSkipToContent: TaskerInputFieldState.Content<String>?,
    inputTimeToSkipContent: TaskerInputFieldState.Content<String>?,
    onFinish: () -> Unit
) {
    val inputList = mutableListOf<TaskerInputFieldState.Content<*>>(inputCommandContent)
    inputChapterToSkipToContent?.let { inputList.add(it) }
    inputTimeToSkipToContent?.let { inputList.add(it) }
    inputTimeToSkipContent?.let { inputList.add(it) }
    ComposableTaskerInputFieldList(inputList, onFinish)
}

@Preview(showBackground = true)
@Composable
private fun ComposableConfigControlPlaybackPreview() {
    AppTheme(Theme.ThemeType.CLASSIC_LIGHT) {
        ComposableConfigControlPlayback(
            TaskerInputFieldState.Content(
                InputControlPlayback.PlaybackCommand.SkipToTime.name,
                au.com.shiftyjelly.pocketcasts.localization.R.string.playback_command,
                {}, listOf("%test"),
                InputControlPlayback.PlaybackCommand.values().toList()
            ),
            TaskerInputFieldState.Content(
                "1",
                au.com.shiftyjelly.pocketcasts.localization.R.string.chapter_to_skip_to,
                {}, listOf("%test")
            ),
            TaskerInputFieldState.Content(
                "60",
                au.com.shiftyjelly.pocketcasts.localization.R.string.time_to_skip_to_seconds,
                {}, listOf("%test")
            ),
            TaskerInputFieldState.Content(
                "60",
                au.com.shiftyjelly.pocketcasts.localization.R.string.time_to_skip_seconds,
                {}, listOf("%test")
            )
        ) {}
    }
}
