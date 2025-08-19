package au.com.shiftyjelly.pocketcasts.playlists.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PlaylistNameInputField(
    state: TextFieldState,
    onClickImeAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FormField(
        state = state,
        placeholder = stringResource(LR.string.new_playlists_title_placeholder),
        trailingIcon = {
            AnimatedVisibility(
                visible = state.text.isNotBlank(),
                enter = ClearButtonEnterTransition,
                exit = ClearButtonExitTransition,
            ) {
                IconButton(
                    onClick = { state.clearText() },
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_close_outlined),
                        contentDescription = stringResource(LR.string.clear),
                        tint = MaterialTheme.theme.colors.primaryIcon02,
                    )
                }
            }
        },
        onImeAction = onClickImeAction,
        modifier = modifier,
    )
}

private val ClearButtonExitTransition = fadeOut()
private val ClearButtonEnterTransition = fadeIn()

@Preview
@Composable
private fun PlaylistNameInputFieldPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    val state = rememberTextFieldState(initialText = "My Playlist")
    AppThemeWithBackground(themeType) {
        PlaylistNameInputField(
            state = state,
            onClickImeAction = {},
            modifier = Modifier.padding(8.dp),
        )
    }
}
