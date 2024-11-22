package au.com.shiftyjelly.pocketcasts.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ProfileUpgradeSection(
    isVisible: Boolean,
    contentPadding: PaddingValues,
    onClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(isVisible, modifier = modifier) {
        Box {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        interactionSource = remember(::MutableInteractionSource),
                        indication = ripple(
                            color = MaterialTheme.theme.colors.primaryIcon01,
                        ),
                        onClick = onClick,
                    )
                    .fillMaxWidth()
                    .padding(contentPadding),
            ) {
                val logoResource = if (MaterialTheme.theme.isLight) {
                    IR.drawable.plus_logo_horizontal_light
                } else {
                    IR.drawable.plus_logo_horizontal_dark
                }
                Image(
                    painter = painterResource(logoResource),
                    contentDescription = null,
                    modifier = Modifier.widthIn(max = 232.dp),
                )
                TextH40(
                    text = stringResource(LR.string.profile_help_support),
                    color = MaterialTheme.theme.colors.primaryText02,
                    textAlign = TextAlign.Center,
                )
                TextH50(
                    text = stringResource(LR.string.plus_learn_more_about_plus),
                    color = MaterialTheme.theme.colors.primaryInteractive01,
                    textAlign = TextAlign.Center,
                )
            }
            Icon(
                painter = painterResource(IR.drawable.ic_close),
                tint = MaterialTheme.theme.colors.primaryText02,
                contentDescription = stringResource(LR.string.settings_close_upgrade_offer),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable(
                        role = Role.Button,
                        interactionSource = remember(::MutableInteractionSource),
                        indication = ripple(
                            color = MaterialTheme.theme.colors.primaryIcon01,
                            bounded = false,
                        ),
                        onClick = onCloseClick,
                    )
                    .size(48.dp)
                    .padding(12.dp),
            )
        }
    }
}

@Preview
@Composable
private fun ProfileUpgradeSectionPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppThemeWithBackground(theme) {
        var isVisible by remember { mutableStateOf(true) }

        ProfileUpgradeSection(
            isVisible = isVisible,
            contentPadding = PaddingValues(16.dp),
            onClick = {},
            onCloseClick = { isVisible = false },
        )
    }
}
