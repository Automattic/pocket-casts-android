package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.config

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ComposableTaskerInputFieldList
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.TaskerInputFieldState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun ComposableConfigPlayPlaylist(content: TaskerInputFieldState.Content<String>, onFinish: () -> Unit) {
    ComposableTaskerInputFieldList(listOf(content), onFinish)
}

@Preview(showBackground = true)
@Composable
private fun ComposableConfigPlayPlaylistPreview() {
    AppTheme(Theme.ThemeType.CLASSIC_LIGHT) {
        ComposableConfigPlayPlaylist(
            TaskerInputFieldState.Content("New Release", au.com.shiftyjelly.pocketcasts.localization.R.string.filters_filter_name, {}, listOf("%test"), listOf("New Releases", "Up Next")), {}
        )
    }
}
