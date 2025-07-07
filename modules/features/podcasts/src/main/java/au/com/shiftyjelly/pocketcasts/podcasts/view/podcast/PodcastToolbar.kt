package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun PodcastToolbar(
    title: String,
    @FloatRange(0.0, 1.0) transparencyProgress: Float,
    onGoBack: () -> Unit,
    onChromeCast: () -> Unit,
    onShare: () -> Unit,
    onBackgroundColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = toolbarColors(transparencyProgress)
    LaunchedEffect(colors.backgroundColor, onBackgroundColorChange) {
        onBackgroundColorChange(colors.backgroundColor)
    }

    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = MaterialTheme.theme.colors.primaryIcon01),
    ) {
        TopAppBar(
            navigationIcon = {
                ToolbarButton(
                    iconId = IR.drawable.ic_arrow_back,
                    contentDescription = stringResource(LR.string.back),
                    backgroundColor = colors.buttonBackgroundColor,
                    iconColor = colors.buttonIconColor,
                    onClick = onGoBack,
                    modifier = Modifier.padding(start = 8.dp),
                )
            },
            title = {
                Text(
                    text = title,
                    color = colors.titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            actions = {
                val scope = rememberCoroutineScope()
                val performMediaRouteClick = remember { MutableSharedFlow<Unit>() }
                if (!LocalInspectionMode.current) {
                    MediaRouteButton(performMediaRouteClick)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    ToolbarButton(
                        iconId = IR.drawable.ic_chrome_cast,
                        contentDescription = stringResource(LR.string.chromecast),
                        backgroundColor = colors.buttonBackgroundColor,
                        iconColor = colors.buttonIconColor,
                        onClick = {
                            scope.launch {
                                performMediaRouteClick.emit(Unit)
                                onChromeCast()
                            }
                        },
                    )

                    ToolbarButton(
                        iconId = IR.drawable.ic_share,
                        contentDescription = stringResource(LR.string.share),
                        backgroundColor = colors.buttonBackgroundColor,
                        iconColor = colors.buttonIconColor,
                        onClick = onShare,
                    )
                }
            },
            backgroundColor = colors.backgroundColor,
            elevation = 0.dp,
            windowInsets = AppBarDefaults.topAppBarWindowInsets,
            modifier = modifier.fillMaxWidth(),
        )
    }
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
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(iconId),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun MediaRouteButton(
    clickTrigger: Flow<Unit>,
) {
    val scope = rememberCoroutineScope()
    AndroidView(
        factory = { context ->
            androidx.mediarouter.app.MediaRouteButton(context).apply {
                visibility = View.GONE
                CastButtonFactory.setUpMediaRouteButton(context, this)
            }
        },
        update = { view ->
            scope.launch {
                clickTrigger.collect { view.performClick() }
            }
        },
    )
}

private data class PodcastToolbarColors(
    val backgroundColor: Color,
    val titleColor: Color,
    val buttonBackgroundColor: Color,
    val buttonIconColor: Color,
)

@Composable
private fun toolbarColors(progress: Float): PodcastToolbarColors {
    val backgroundColor = MaterialTheme.theme.colors.primaryUi01
    val buttonIconColor = MaterialTheme.theme.colors.primaryIcon01
    val titleColor = MaterialTheme.theme.colors.primaryText01

    return remember(progress) {
        PodcastToolbarColors(
            backgroundColor = backgroundColor.copy(alpha = 1f - progress),
            titleColor = Color(
                ColorUtils.blendARGB(
                    titleColor.toArgb(),
                    Color.Transparent.toArgb(),
                    progress,
                ),
            ),
            buttonBackgroundColor = Color(
                ColorUtils.blendARGB(
                    Color.Transparent.toArgb(),
                    Color.Black.copy(alpha = 0.32f).toArgb(),
                    progress,
                ),
            ),
            buttonIconColor = Color(
                ColorUtils.blendARGB(
                    buttonIconColor.toArgb(),
                    Color.White.toArgb(),
                    progress,
                ),
            ),
        )
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
            PodcastToolbar(
                title = "Transparent",
                transparencyProgress = 1f,
                onGoBack = {},
                onChromeCast = {},
                onShare = {},
                onBackgroundColorChange = {},
            )
            PodcastToolbar(
                title = "Semi Transparent",
                transparencyProgress = 0.5f,
                onGoBack = {},
                onChromeCast = {},
                onShare = {},
                onBackgroundColorChange = {},
            )
            PodcastToolbar(
                title = "Opaque",
                transparencyProgress = 0f,
                onGoBack = {},
                onChromeCast = {},
                onShare = {},
                onBackgroundColorChange = {},
            )
        }
    }
}
