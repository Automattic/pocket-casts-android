package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType

@Composable
internal fun RulePage(
    title: String,
    onSaveRule: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
    isSaveEnabled: Boolean = true,
    toolbarActions: (@Composable RowScope.(Color) -> Unit) = {},
    content: @Composable BoxScope.(bottomPadding: Dp) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ThemedTopAppBar(
            navigationButton = NavigationButton.Back,
            style = ThemedTopAppBar.Style.Immersive,
            iconColor = MaterialTheme.theme.colors.primaryIcon03,
            windowInsets = ZeroWindowInsets,
            onNavigationClick = onClickBack,
            actions = toolbarActions,
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            TextH20(
                text = title,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                content(ButtonPadding)
            }
            RowButton(
                text = stringResource(R.string.save_smart_rule),
                enabled = isSaveEnabled,
                onClick = onSaveRule,
                includePadding = false,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
            )
        }
    }
}

private val ButtonPadding = 24.dp
private val ZeroWindowInsets = WindowInsets(0)

@Composable
@PreviewRegularDevice
private fun RulePagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        RulePage(
            title = "Title",
            onSaveRule = {},
            onClickBack = {},
        ) { bottomPadding ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(200.dp)
                    .background(MaterialTheme.theme.colors.primaryInteractive01)
                    .padding(top = 24.dp, bottom = bottomPadding),
            )
        }
    }
}
