package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.config

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ActivityConfigBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ComposableTaskerInputFieldList
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.TaskerInputFieldState
import au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.InputControlPlayback
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import au.com.shiftyjelly.pocketcasts.images.R as RD

@AndroidEntryPoint
class ActivityConfigControlPlayback : ActivityConfigBase<ViewModelConfigControlPlayback>() {
    override val viewModel: ViewModelConfigControlPlayback by viewModels()
}

@Preview(showBackground = true)
@Composable
private fun ComposableConfigControlPlaybackPreview() {
    AppTheme(Theme.ThemeType.CLASSIC_LIGHT) {
        ComposableTaskerInputFieldList(
            listOf(
                TaskerInputFieldState.Content(
                    MutableStateFlow(InputControlPlayback.PlaybackCommand.SkipToTime.name),
                    R.string.playback_command,
                    RD.drawable.filter_play,
                    MutableStateFlow(true),
                    {},
                    listOf("%test"),
                    MutableStateFlow(InputControlPlayback.PlaybackCommand.values().toList())
                ),
                TaskerInputFieldState.Content(
                    MutableStateFlow("60"),
                    R.string.time_to_skip_to_seconds,
                    RD.drawable.filter_time,
                    MutableStateFlow(true),
                    {},
                    listOf("%test")
                )
            )
        ) {}
    }
}
