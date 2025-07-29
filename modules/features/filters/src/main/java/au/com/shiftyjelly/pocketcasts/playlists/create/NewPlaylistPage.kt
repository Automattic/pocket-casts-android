package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.FormField
import au.com.shiftyjelly.pocketcasts.compose.components.SparkleImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun NewPlaylistPage(
    titleState: TextFieldState,
    onCreateManualPlaylist: () -> Unit,
    onContinueToSmartPlaylist: () -> Unit,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
    Column(
        modifier = modifier,
    ) {
        IconButton(
            onClick = onClickClose,
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_close),
                contentDescription = stringResource(LR.string.close),
                tint = MaterialTheme.theme.colors.primaryIcon03,
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            TextH20(
                text = stringResource(LR.string.new_playlist),
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            FormField(
                state = titleState,
                placeholder = stringResource(LR.string.new_playlists_title_placeholder),
                trailingIcon = {
                    AnimatedVisibility(
                        visible = titleState.text.isNotBlank(),
                        enter = ClearButtonEnterTransition,
                        exit = ClearButtonExitTransition,
                    ) {
                        IconButton(
                            onClick = { titleState.clearText() },
                        ) {
                            Icon(
                                painter = painterResource(IR.drawable.ic_close_outlined),
                                contentDescription = stringResource(LR.string.clear),
                                tint = MaterialTheme.theme.colors.primaryIcon02,
                            )
                        }
                    }
                },
                onImeAction = onCreateManualPlaylist,
                modifier = Modifier.focusRequester(focusRequester),
            )
            Spacer(
                modifier = Modifier.height(16.dp),
            )
            SmartPlaylistButton(
                enabled = titleState.text.isNotBlank(),
                onClick = onContinueToSmartPlaylist,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(
                modifier = Modifier.height(24.dp),
            )
            RowButton(
                text = stringResource(LR.string.create_playlist),
                enabled = titleState.text.isNotBlank(),
                onClick = onCreateManualPlaylist,
                includePadding = false,
            )
        }
    }
}

@Composable
private fun SmartPlaylistButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.3f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.theme.colors.primaryUi02Active)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        SparkleImage(
            modifier = Modifier.size(24.dp),
            gradientColors = MaterialTheme.theme.colors.run { primaryText01 to primaryText01 },
        )
        Spacer(
            modifier = Modifier.width(12.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            TextH40(
                text = stringResource(LR.string.new_smart_playlist_banner_title),
            )
            TextP60(
                text = stringResource(LR.string.new_smart_playlist_banner_body),
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
        Spacer(
            modifier = Modifier.width(12.dp),
        )
        Image(
            painter = painterResource(IR.drawable.ic_chevron_trimmed),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryText02),
        )
    }
}

private val ClearButtonExitTransition = fadeOut()
private val ClearButtonEnterTransition = fadeIn()

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun NewPlaylistPagerPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    val titleState = rememberTextFieldState(initialText = "My Playlist")
    AppThemeWithBackground(themeType) {
        NewPlaylistPage(
            titleState = titleState,
            onCreateManualPlaylist = {},
            onContinueToSmartPlaylist = {},
            onClickClose = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
