package au.com.shiftyjelly.pocketcasts.playlists.component

import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal sealed interface ToolbarConfig {
    val showTitle: Boolean

    data object WithoutTitle : ToolbarConfig {
        override val showTitle = false
    }

    data object WithTitle : ToolbarConfig {
        override val showTitle = true
    }

    data class ForAlpha(
        @FloatRange(0.0, 1.0) val alpha: Float,
    ) : ToolbarConfig {
        override val showTitle = true
    }
}

@Composable
internal fun PlaylistToolbar(
    title: String,
    config: ToolbarConfig,
    onClickBack: () -> Unit,
    onClickOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rememberToolbarColors(config)
    TopAppBar(
        navigationIcon = {
            ToolbarButton(
                iconId = IR.drawable.ic_arrow_back,
                contentDescription = stringResource(LR.string.back),
                backgroundColor = colors.buttonBackgroundColor,
                iconColor = colors.buttonIconColor,
                onClick = onClickBack,
                modifier = Modifier.padding(start = 8.dp),
            )
        },
        title = {
            AnimatedVisibility(
                visible = config.showTitle,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                val textStyle = LocalTextStyle.current
                val nonScaledTextStyle = textStyle.merge(
                    fontSize = textStyle.fontSize.value.nonScaledSp,
                    lineHeight = textStyle.lineHeight.value.nonScaledSp,
                )
                Text(
                    text = title,
                    style = nonScaledTextStyle,
                    color = colors.titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            ToolbarButton(
                iconId = IR.drawable.ic_overflow,
                contentDescription = stringResource(LR.string.options),
                backgroundColor = colors.buttonBackgroundColor,
                iconColor = colors.buttonIconColor,
                onClick = onClickOptions,
                modifier = Modifier.padding(end = 8.dp),
            )
        },
        backgroundColor = colors.backgroundColor,
        elevation = 0.dp,
        windowInsets = WindowInsets.statusBars,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ToolbarButton(
    @DrawableRes iconId: Int,
    contentDescription: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {},
    ) {
        Image(
            painter = painterResource(iconId),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier.size(20.dp),
        )
    }
}

private data class PlaylistToolbarColors(
    val backgroundColor: Color,
    val titleColor: Color,
    val buttonBackgroundColor: Color,
    val buttonIconColor: Color,
)

@Composable
private fun rememberToolbarColors(config: ToolbarConfig): PlaylistToolbarColors {
    val backgroundColor = MaterialTheme.theme.colors.secondaryUi01
    val buttonIconColor = MaterialTheme.theme.colors.secondaryIcon01
    val titleColor = MaterialTheme.theme.colors.secondaryText01

    return remember(config) {
        when (config) {
            is ToolbarConfig.WithTitle, is ToolbarConfig.WithoutTitle -> PlaylistToolbarColors(
                backgroundColor = Color.Transparent,
                titleColor = titleColor,
                buttonBackgroundColor = Color.Black.copy(alpha = 0.32f),
                buttonIconColor = Color.White,
            )

            is ToolbarConfig.ForAlpha -> PlaylistToolbarColors(
                backgroundColor = backgroundColor.copy(alpha = config.alpha),
                titleColor = Color(
                    ColorUtils.blendARGB(
                        titleColor.toArgb(),
                        Color.Transparent.toArgb(),
                        1f - config.alpha,
                    ),
                ),
                buttonBackgroundColor = Color(
                    ColorUtils.blendARGB(
                        Color.Transparent.toArgb(),
                        Color.Black.copy(alpha = 0.32f).toArgb(),
                        1f - config.alpha,
                    ),
                ),
                buttonIconColor = Color(
                    ColorUtils.blendARGB(
                        buttonIconColor.toArgb(),
                        Color.White.toArgb(),
                        1f - config.alpha,
                    ),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun PodcastToolbarPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppTheme(
        themeType = theme,
    ) {
        Column {
            PlaylistToolbar(
                title = "Without title",
                config = ToolbarConfig.WithoutTitle,
                onClickBack = {},
                onClickOptions = {},
            )
            PlaylistToolbar(
                title = "With title",
                config = ToolbarConfig.WithTitle,
                onClickBack = {},
                onClickOptions = {},
            )
            PlaylistToolbar(
                title = "Transparent",
                config = ToolbarConfig.ForAlpha(0f),
                onClickBack = {},
                onClickOptions = {},
            )
            PlaylistToolbar(
                title = "Semi Transparent",
                config = ToolbarConfig.ForAlpha(0.5f),
                onClickBack = {},
                onClickOptions = {},
            )
            PlaylistToolbar(
                title = "Opaque",
                config = ToolbarConfig.ForAlpha(1f),
                onClickBack = {},
                onClickOptions = {},
            )
        }
    }
}
