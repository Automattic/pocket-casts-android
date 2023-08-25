package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.settings.HeadphoneControlsSettingsFragment
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun NoBookmarksView(
    style: NoBookmarksViewColors,
    openFragment: (Fragment) -> Unit,
    sourceView: SourceView,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<NoBookmarksViewModel>()
    Content(
        style = style,
        onClick = {
            viewModel.onGoToHeadphoneSettingsClicked(sourceView)
            openFragment(HeadphoneControlsSettingsFragment())
        },
        modifier = modifier,
    )
}

@Composable
private fun Content(
    style: NoBookmarksViewColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MessageView(
        titleView = {
            TextH20(
                text = stringResource(LR.string.bookmarks_not_found),
                color = style.textColor(),
            )
        },
        buttonTitleRes = LR.string.bookmarks_headphone_settings,
        buttonAction = onClick,
        style = style.toMessageViewColors(),
        modifier = modifier,
    )
}

sealed class NoBookmarksViewColors {
    @Composable
    abstract fun textColor(): Color

    object Default : NoBookmarksViewColors() {
        @Composable
        override fun textColor(): Color = MaterialTheme.theme.colors.primaryText01
    }

    object Player : NoBookmarksViewColors() {
        @Composable
        override fun textColor(): Color = MaterialTheme.theme.colors.playerContrast01
    }

    fun toMessageViewColors() = when (this) {
        Default -> MessageViewColors.Default
        Player -> MessageViewColors.Player
    }
}

@Preview
@Composable
private fun NoBookmarksPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Content(
            style = NoBookmarksViewColors.Default,
            onClick = {},
        )
    }
}
