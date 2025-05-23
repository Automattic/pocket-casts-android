package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Banner(
    title: String,
    description: String,
    actionLabel: String,
    icon: Painter,
    onActionClick: () -> Unit,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.theme.colors.primaryUi02Active,
    titleColor: Color = MaterialTheme.theme.colors.primaryText01,
    descriptionColor: Color = MaterialTheme.theme.colors.primaryText02,
    actionLabelColor: Color = MaterialTheme.theme.colors.primaryInteractive01,
    iconTint: Color = MaterialTheme.theme.colors.primaryText01,
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 14.dp),
    ) {
        Row(
            modifier = Modifier,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )

            Spacer(
                modifier = Modifier.width(12.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                TextH40(
                    text = title,
                    color = titleColor,
                )
                Spacer(
                    modifier = Modifier.height(4.dp),
                )
                TextP50(
                    text = description,
                    lineHeight = 16.sp,
                    color = descriptionColor,
                )

                CompositionLocalProvider(
                    LocalRippleConfiguration provides RippleConfiguration(color = actionLabelColor),
                ) {
                    TextH50(
                        text = actionLabel,
                        color = actionLabelColor,
                        modifier = Modifier
                            .offset(x = -6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(
                                onClick = onActionClick,
                                role = Role.Button,
                            )
                            .padding(6.dp),
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(16.dp),
            )

            onDismiss?.let {
                Icon(
                    painter = painterResource(IR.drawable.ic_close),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        onDismiss?.let {
            CompositionLocalProvider(
                LocalRippleConfiguration provides RippleConfiguration(color = iconTint),
            ) {
                val dismissText = stringResource(LR.string.dismiss)
                Box(
                    modifier = Modifier
                        .offset(x = 12.dp, y = -12.dp)
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .clickable(
                            onClick = onDismiss,
                            role = Role.Button,
                            indication = dismissRipple,
                            interactionSource = null,
                        )
                        .semantics { contentDescription = dismissText },
                )
            }
        }
    }
}

private val dismissRipple = ripple(radius = 24.dp)

@Preview
@Composable
private fun BannerPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Banner(
            title = "Your shows, on any device",
            description = "Create a free account to sync your shows and listen anywhere.",
            actionLabel = "Create a free account",
            icon = painterResource(IR.drawable.ic_heart_2),
            onActionClick = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun BannerPreviewWithoutDismiss(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Banner(
            title = "Your shows, on any device",
            description = "Create a free account to sync your shows and listen anywhere.",
            actionLabel = "Create a free account",
            icon = painterResource(IR.drawable.ic_heart_2),
            onActionClick = {},
            onDismiss = null,
        )
    }
}
